package org.example.cognition

import kotlinx.serialization.Serializable
import java.util.BitSet

// Контекст — это функция bitvector → bitvector, реализованная как XOR-маска.
// Маска хранится разреженно: IntArray индексов тех битов, которые контекст должен
// инвертировать. Такая форма компактнее плотного битсета (на плотности ~1% — примерно
// 100 индексов против 11k бит) и позволяет применить контекст за число операций,
// равное количеству флипов, а не длине вектора.
//
// successCount — сколько раз применение контекста привело к "узнаванию" эталона
// (похожесть по метрике превысила порог). Счётчик копится между итерациями
// обучения — это осознанный выбор: для статистики важна общая история, а не
// результат последнего прогона.
//
// fixed — зафиксирован ли контекст как проверенно полезный. Зафиксированные
// контексты не удаляются в sleep-фазе независимо от текущего счётчика.
@Serializable
class Context(
    val flipIndices: IntArray,
    var successCount: Int = 0,
    var fixed: Boolean = false,
) {

    // Записывает XOR(input, mask) в scratch. scratch должен иметь достаточную ёмкость
    // (BitSet сам расширится, но для горячего цикла лучше переиспользовать один буфер
    // нужного размера, чтобы избежать аллокаций).
    fun applyTo(input: BitSet, scratch: BitSet) {
        scratch.clear()
        scratch.or(input)
        for (idx in flipIndices) {
            scratch.flip(idx)
        }
    }
}

// Конвертация выхода энкодера (IntArray из 0/1) в плотный BitSet. Делается один раз
// при загрузке — дальше вся контекстная математика работает в BitSet.
fun IntArray.toBitSet(): BitSet {
    val bitSet = BitSet(size)
    for (i in indices) {
        if (this[i] != 0) bitSet.set(i)
    }
    return bitSet
}
