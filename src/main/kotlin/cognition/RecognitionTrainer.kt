package org.example.cognition

import java.io.File
import java.util.BitSet
import kotlin.system.measureTimeMillis

// Закодированный сэмпл: имя файла (для логов), плотный битвектор кода и кеш
// cardinality — чтобы не пересчитывать при каждом вызове Жаккара.
data class EncodedSample(
    val name: String,
    val code: BitSet,
    val cardinality: Int,
)

// Конфиг тренировки. Разумные значения по умолчанию подобраны для первого
// прогона на синтетическом датасете цифры "1".
data class TrainingConfig(
    val poolTargetSize: Int = 50_000,
    val flipRange: IntRange = 1..5000,
    // Порог поднят относительно первого эксперимента: при 0.7 читерские (почти-identity)
    // маски свободно проходили, потому что у нескольких картинок базовый Жаккар и так
    // был ≥ 0.7. На 0.95 ни одна картинка сейчас не покрывается без реального улучшения.
    val recognitionThreshold: Double = 0.95,
    val fixThreshold: Int = 12,
    val maxIterations: Int = 5000,
    val poolPath: File = File("build/cognition/pool.json"),
)

// Одноразовые буферы для горячего цикла, чтобы не аллоцировать BitSet на каждое
// применение контекста (а их миллионы за одну итерацию).
private class Scratches(size: Int) {
    val xor: BitSet = BitSet(size)
    val jaccard: BitSet = BitSet(size)
}

// Агрегированная статистика одного wake-прохода. Все DoubleArray'и проиндексированы
// так же, как training — чтобы по ним строить имена непокрытых картинок.
// Поля:
//   baseJaccard       — Жаккар input↔canonical без применения контекстов.
//   bestJaccardAll    — лучший Жаккар через любой контекст (fixed и unfixed).
//   bestJaccardFixed  — лучший Жаккар через зафиксированный контекст (для покрытия).
private class WakeStats(
    val applications: Long,
    val successes: Long,
    val baseJaccard: DoubleArray,
    val bestJaccardAll: DoubleArray,
    val bestJaccardFixed: DoubleArray,
)

class RecognitionTrainer(
    private val config: TrainingConfig,
    private val vectorSize: Int,
    private val canonical: EncodedSample,
    private val training: List<EncodedSample>,
) {
    fun train() {
        logHeader()
        val pool = loadOrCreatePool()
        val scratch = Scratches(vectorSize)

        for (iteration in 1..config.maxIterations) {
            println()
            println("═══════════════ Итерация $iteration/${config.maxIterations} ═══════════════")

            val wake: WakeStats
            val wakeMs = measureTimeMillis { wake = runWake(pool, scratch) }
            logWake(wake, pool, wakeMs)

            val report = pool.revise(config.fixThreshold)
            val toAdd = (config.poolTargetSize - pool.size).coerceAtLeast(0)
            pool.addRandom(toAdd, config.flipRange)
            logSleep(report, toAdd, pool)

            logSummary(wake)
            save(pool)

            val covered = wake.bestJaccardFixed.indices.count { i ->
                wake.bestJaccardFixed[i] >= config.recognitionThreshold &&
                    wake.bestJaccardFixed[i] > wake.baseJaccard[i]
            }
            if (training.isNotEmpty() && covered == training.size) {
                println()
                println("✅ Все ${training.size} картинок покрыты зафиксированными контекстами за $iteration итераций.")
                println("   Финальный пул: ${pool.size} (fixed=${pool.fixedCount})")
                return
            }
        }

        println()
        println("⏱  Достигнут лимит в ${config.maxIterations} итераций — останавливаемся.")
    }

    private fun loadOrCreatePool(): ContextPool {
        if (config.poolPath.exists()) {
            val pool = ContextPool.loadFrom(config.poolPath)
            require(pool.vectorSize == vectorSize) {
                "Размер вектора в сохранённом пуле (${pool.vectorSize}) ≠ текущему ($vectorSize). " +
                    "Удалите ${config.poolPath} или восстановите совместимый энкодер."
            }
            println("💾 Пул загружен из ${config.poolPath}: ${pool.size} контекстов (fixed=${pool.fixedCount}).")
            return pool
        }
        println("💾 Сохранённого пула нет — генерирую свежий из ${config.poolTargetSize} случайных контекстов.")
        val pool = ContextPool(vectorSize)
        pool.addRandom(config.poolTargetSize, config.flipRange)
        return pool
    }

    private fun runWake(
        pool: ContextPool,
        scratch: Scratches,
    ): WakeStats {
        val size = training.size
        val baseJaccard = DoubleArray(size)
        val bestAll = DoubleArray(size)
        val bestFixed = DoubleArray(size)
        var applications = 0L
        var successes = 0L

        for ((idx, sample) in training.withIndex()) {
            val base = Similarity.jaccard(sample.code, canonical.code, scratch.jaccard)
            baseJaccard[idx] = base

            for (context in pool.contexts) {
                context.applyTo(sample.code, scratch.xor)
                val score = Similarity.jaccard(scratch.xor, canonical.code, scratch.jaccard)
                applications++
                if (score > bestAll[idx]) bestAll[idx] = score
                if (context.fixed && score > bestFixed[idx]) bestFixed[idx] = score
                // Успех = и абсолютный порог, и строгое улучшение над базой. Второе
                // условие убивает identity-подобные контексты: у них score == base, так
                // что как бы высоко ни был base, они не засчитываются.
                if (score >= config.recognitionThreshold && score > base) {
                    context.successCount++
                    successes++
                }
            }
        }

        return WakeStats(applications, successes, baseJaccard, bestAll, bestFixed)
    }

    // ─── Логирование ──────────────────────────────────────────────────────

    private fun logHeader() {
        println("🧠 Genevieve — запуск обучения контекстов")
        println("   Пул (target): ${config.poolTargetSize}")
        println("   flipRange: ${config.flipRange}")
        println("   recognitionThreshold: ${config.recognitionThreshold}, fixThreshold: ${config.fixThreshold}")
        println("   maxIterations: ${config.maxIterations}")
        println("   Путь к пулу: ${config.poolPath}")
        println()
        val density = 100.0 * canonical.cardinality / vectorSize
        println("📸 Обучающих картинок: ${training.size}, размер вектора: $vectorSize")
        println("🎯 Эталон: ${canonical.name} (активных битов: ${canonical.cardinality}, плотность: ${"%.2f".format(density)}%)")
    }

    private fun logWake(
        w: WakeStats,
        pool: ContextPool,
        millis: Long,
    ) {
        val successRate = if (w.applications > 0) 100.0 * w.successes / w.applications else 0.0
        println("🌅 Wake-фаза ($millis мс)")
        println("   Применений: ${w.applications} (пул=${pool.size} × картинок=${training.size})")
        println("   Срабатываний (Жаккар ≥ ${config.recognitionThreshold} И строго выше базы): ${w.successes} (${"%.4f".format(successRate)}%)")
        println("   📐 Базовый Жаккар (input↔canonical):   ${statsLine(w.baseJaccard)}")
        println("   📈 Лучший через любой контекст:         ${statsLine(w.bestJaccardAll)}")
        println("   🎯 Лучший через fixed контекст:         ${statsLine(w.bestJaccardFixed)}")
    }

    private fun logSleep(
        report: ReviseReport,
        added: Int,
        pool: ContextPool,
    ) {
        val unfixed = pool.size - pool.fixedCount
        println("🌙 Sleep-фаза")
        println("   🗑  Удалено (нулевой successCount): ${report.removed}")
        println("   ✨ Зафиксировано новых:              ${report.newlyFixed}")
        println("   🎲 Добавлено случайных:              $added")
        println("   Итого в пуле: ${pool.size} (fixed=${pool.fixedCount}, unfixed=$unfixed)")
        logFixedDensity(pool)
    }

    private fun logFixedDensity(pool: ContextPool) {
        val sizes = pool.contexts.asSequence().filter { it.fixed }.map { it.flipIndices.size }.toList()
        if (sizes.isEmpty()) {
            println("   📐 Плотность fixed-контекстов: — (пока нет)")
            return
        }
        val sorted = sizes.sorted()
        val mean = sorted.sum().toDouble() / sorted.size
        println(
            "   📐 Плотность fixed-контекстов: min=${sorted.first()}, median=${sorted[sorted.size / 2]}," +
                " mean=${"%.1f".format(mean)}, max=${sorted.last()}",
        )
    }

    private fun logSummary(w: WakeStats) {
        val threshold = config.recognitionThreshold
        val uncoveredNames =
            training.withIndex()
                .filter { (i, _) ->
                    w.bestJaccardFixed[i] < threshold || w.bestJaccardFixed[i] <= w.baseJaccard[i]
                }
                .map { it.value.name }
        val covered = training.size - uncoveredNames.size
        println("📊 Покрытие: $covered/${training.size} (fixed контекст, Жаккар ≥ $threshold И строго выше базы)")
        when {
            uncoveredNames.isEmpty() -> Unit
            uncoveredNames.size <= 10 -> println("   Не покрыты: ${uncoveredNames.joinToString(", ")}")
            else ->
                println(
                    "   Не покрыты: ${uncoveredNames.take(10).joinToString(", ")} + ещё ${uncoveredNames.size - 10}",
                )
        }
    }

    private fun save(pool: ContextPool) {
        pool.saveTo(config.poolPath)
        println("💾 Пул сохранён в ${config.poolPath} (${pool.size} контекстов, fixed=${pool.fixedCount})")
    }

    private fun statsLine(values: DoubleArray): String {
        if (values.isEmpty()) return "—"
        val sorted = values.sortedArray()
        val mean = values.sum() / values.size
        return "min=%.3f, median=%.3f, mean=%.3f, max=%.3f".format(
            sorted.first(),
            sorted[sorted.size / 2],
            mean,
            sorted.last(),
        )
    }
}

