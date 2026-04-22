package org.example.perception.visual.domain.entity

import org.example.perception.visual.data.RawImage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Один детектор сетчатки. Смотрит на квадратное окошко пикселей вокруг своего
 * центра и выдаёт бинарный сигнал: 1, если в окошке видна граница, нормаль
 * которой попадает в полуплоскость ±90° от предпочтительного направления
 * [theta]; 0 иначе.
 *
 * Внутри держит маску ±1, построенную один раз в конструкторе из своего
 * [theta] и [radius]. С одной стороны от линии, проходящей через центр
 * перпендикулярно [theta], пиксели получают вес +1; с другой — −1; на самой
 * линии — 0.
 */
class Detector(
    val cx: Int,
    val cy: Int,
    val theta: Double,
    val radius: Int,
    val threshold: Double,
) {
    private val windowSize: Int = 2 * radius + 1
    private val mask: Array<IntArray> = buildMask()

    private fun buildMask(): Array<IntArray> {
        val dirX = cos(theta)
        val dirY = sin(theta)
        return Array(windowSize) { my ->
            IntArray(windowSize) { mx ->
                val dx = mx - radius
                val dy = my - radius
                val projection = dx * dirX + dy * dirY
                when {
                    projection > EPSILON -> 1
                    projection < -EPSILON -> -1
                    else -> 0
                }
            }
        }
    }

    /**
     * Применяет маску к пятну пикселей вокруг ([cx], [cy]), складывает и
     * сравнивает с [threshold]. Возвращает 1, если сумма больше порога; 0 иначе.
     */
    fun respond(image: RawImage): Int {
        var sum = 0.0
        for (my in 0 until windowSize) {
            for (mx in 0 until windowSize) {
                val weight = mask[my][mx]
                if (weight == 0) continue
                val px = cx + (mx - radius)
                val py = cy + (my - radius)
                sum += weight * image.getPixel(px, py)
            }
        }
        return if (sum > threshold) 1 else 0
    }

    companion object {
        // Допуск для плавающей арифметики: пиксели, чья проекция на направление
        // детектора по модулю меньше EPSILON, считаются лежащими на центральной
        // линии маски и получают вес 0.
        private const val EPSILON = 1e-9
    }
}
