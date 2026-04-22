package org.example.cognition

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import kotlin.random.Random

// Пул контекстов — живая популяция плюс интерфейс для её эволюции.
// Отвечает за три вещи: генерация новых случайных контекстов, ревизия после wake-фазы
// (удаление бесполезных, фиксация проверенных), и персистентность в JSON.
//
// Зафиксированные (fixed) контексты защищены от удаления — они считаются уже
// проверенной частью "знания". Остальные находятся под селекционным давлением:
// тот, кто за wake-фазу не дал ни одного успеха, вылетает; тот, кто набрал достаточно
// успехов, фиксируется; промежуточные остаются жить, накапливая статистику.
class ContextPool(
    val vectorSize: Int,
    initialContexts: List<Context> = emptyList(),
) {
    private val _contexts: MutableList<Context> = initialContexts.toMutableList()

    val contexts: List<Context> get() = _contexts
    val size: Int get() = _contexts.size
    val fixedCount: Int get() = _contexts.count { it.fixed }

    // Добавляет count новых случайных контекстов. Размер маски (число флипаемых
    // битов) сэмплируется равномерно из flipRange — плотность сама по себе является
    // одним из измерений пространства поиска, и фиксировать её заранее значило бы
    // принимать непроверенное решение о том, контексты какого "масштаба" нам нужны.
    //
    // Частные случаи:
    //   flipRange = 0..vectorSize — поиск без предположений о плотности;
    //   flipRange = 50..500       — сфокусированный диапазон;
    //   flipRange = 100..100      — фиксированная плотность (если нужна для
    //                                воспроизводимости отдельного эксперимента).
    fun addRandom(
        count: Int,
        flipRange: IntRange,
        random: Random = Random.Default,
    ) {
        require(!flipRange.isEmpty()) { "flipRange=$flipRange пуст" }
        require(flipRange.first >= 0 && flipRange.last <= vectorSize) {
            "flipRange=$flipRange выходит за [0, $vectorSize]"
        }
        repeat(count) {
            val flips = random.nextInt(flipRange.first, flipRange.last + 1)
            _contexts += Context(sampleUniqueIndices(flips, vectorSize, random))
        }
    }

    // Ревизия по итогам wake-фазы. Политика жёсткая:
    //   - незафиксированные контексты с successCount == 0 удаляются безоговорочно;
    //   - незафиксированные с successCount >= fixThreshold становятся fixed;
    //   - остальные незафиксированные остаются жить на следующую итерацию;
    //   - fixed никогда не трогаем.
    //
    // Возвращает отчёт для логирования и мониторинга сходимости.
    fun revise(fixThreshold: Int): ReviseReport {
        val sizeBefore = _contexts.size
        var newlyFixed = 0
        val iterator = _contexts.iterator()
        while (iterator.hasNext()) {
            val context = iterator.next()
            if (context.fixed) continue
            when {
                context.successCount == 0 -> iterator.remove()
                context.successCount >= fixThreshold -> {
                    context.fixed = true
                    newlyFixed++
                }
            }
        }
        return ReviseReport(
            removed = sizeBefore - _contexts.size,
            newlyFixed = newlyFixed,
            totalFixed = fixedCount,
            totalAlive = _contexts.size,
        )
    }

    // Стриминговая запись: кодируем в OutputStream напрямую, не складывая всю JSON
    // в String в памяти. При пуле в 50k контекстов и средней плотности ~2500 флипов
    // получается ~750 MB JSON — при writeText'е это OOM. encodeToStream пишет
    // последовательно с ограниченным буфером.
    @OptIn(ExperimentalSerializationApi::class)
    fun saveTo(file: File) {
        val snapshot = ContextPoolSnapshot(vectorSize, _contexts.toList())
        file.parentFile?.mkdirs()
        file.outputStream().buffered().use { stream ->
            Json.encodeToStream(ContextPoolSnapshot.serializer(), snapshot, stream)
        }
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun loadFrom(file: File): ContextPool {
            val snapshot =
                file.inputStream().buffered().use { stream ->
                    Json.decodeFromStream(ContextPoolSnapshot.serializer(), stream)
                }
            return ContextPool(snapshot.vectorSize, snapshot.contexts)
        }

        // Сэмплирование count уникальных индексов из [0, range). Для наших параметров
        // (count порядка сотен, range ~11k) hash-set работает быстрее, чем
        // полный shuffle массива индексов.
        private fun sampleUniqueIndices(
            count: Int,
            range: Int,
            random: Random,
        ): IntArray {
            val chosen = HashSet<Int>(count * 2)
            while (chosen.size < count) {
                chosen += random.nextInt(range)
            }
            return chosen.toIntArray().also { it.sort() }
        }
    }
}

data class ReviseReport(
    val removed: Int,
    val newlyFixed: Int,
    val totalFixed: Int,
    val totalAlive: Int,
)

// DTO для сериализации. Отдельный класс, чтобы не тянуть логику ContextPool в
// serialization runtime и не делать приватные поля публичными.
@Serializable
private data class ContextPoolSnapshot(
    val vectorSize: Int,
    val contexts: List<Context>,
)
