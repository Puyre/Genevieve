package org.example.cognition

import java.util.BitSet

object Similarity {

    // Жаккар по единицам: |A ∩ B| / |A ∪ B|.
    //
    // Для sparse-кодировки это более разумная метрика, чем доля совпадающих битов
    // целиком (включая нули): нули для нас не несут информации, они — фон. Если из
    // 11k битов активны 200, то совпадение по нулям тривиально и не говорит ни о чём.
    // Жаккар учитывает только "положительные" совпадения и нормируется так, что не
    // поощряет плотные коды автоматически.
    //
    // scratch — переиспользуемый буфер, чтобы не аллоцировать внутри горячего цикла.
    // Объединение считаем через включение-исключение |A∪B| = |A|+|B|−|A∩B|, что
    // экономит ещё одну операцию с битсетом.
    fun jaccard(a: BitSet, b: BitSet, scratch: BitSet): Double {
        scratch.clear()
        scratch.or(a)
        scratch.and(b)
        val intersection = scratch.cardinality()
        val union = a.cardinality() + b.cardinality() - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    // Удобная версия, аллоцирующая scratch на месте. Не использовать в горячем цикле.
    fun jaccard(a: BitSet, b: BitSet): Double =
        jaccard(a, b, BitSet(maxOf(a.length(), b.length())))
}
