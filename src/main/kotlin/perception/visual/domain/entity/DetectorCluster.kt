package org.example.perception.visual.domain.entity

import org.example.perception.visual.data.DetectorMask
import org.example.perception.visual.data.RawImage

class DetectorCluster(
    private val centerX: Int,
    private val centerY: Int,
    private val stride: Int,
) {

    companion object {
        private var masks: List<DetectorMask>? = null
        private var maskRadius: Int = 0
        private var detectorActivationThreshold: Double = 0.0

        // Вес центрального пикселя в масках направленных детекторов.
        // При чистом совпадении отклик = W + 3 (три пикселя стороны × 1 + центр × W).
        private const val CENTER_WEIGHT = 3

        fun initialize(maskRadius: Int, detectorActivationThreshold: Double) {
            this.maskRadius = maskRadius
            this.detectorActivationThreshold = detectorActivationThreshold
            this.masks = createMasks(maskRadius)
        }

        // 16 направленных детекторов: для каждого из 8 направлений — пара масок разной полярности.
        // «Яркий центр, тёмная сторона X»: срабатывает, когда центр яркий, а 3 пикселя со стороны X — тёмные.
        // «Тёмный центр, яркая сторона X»: зеркально.
        // Этот дизайн pixel-centric: кластер описывает свой собственный центральный пиксель в контексте соседей,
        // а не «градиент, проходящий где-то через окно».
        private fun createMasks(maskRadius: Int): List<DetectorMask> {
            require(maskRadius == 1) { "Currently only radius=1 is supported" }

            val w = CENTER_WEIGHT

            // --- Яркий центр, тёмная сторона (8 направлений) ---

            val brightCenterDarkN = arrayOf(
                intArrayOf(-1, -1, -1),
                intArrayOf(0, +w, 0),
                intArrayOf(0, 0, 0),
            )

            val brightCenterDarkNE = arrayOf(
                intArrayOf(0, -1, -1),
                intArrayOf(0, +w, -1),
                intArrayOf(0, 0, 0),
            )

            val brightCenterDarkE = arrayOf(
                intArrayOf(0, 0, -1),
                intArrayOf(0, +w, -1),
                intArrayOf(0, 0, -1),
            )

            val brightCenterDarkSE = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(0, +w, -1),
                intArrayOf(0, -1, -1),
            )

            val brightCenterDarkS = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(0, +w, 0),
                intArrayOf(-1, -1, -1),
            )

            val brightCenterDarkSW = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(-1, +w, 0),
                intArrayOf(-1, -1, 0),
            )

            val brightCenterDarkW = arrayOf(
                intArrayOf(-1, 0, 0),
                intArrayOf(-1, +w, 0),
                intArrayOf(-1, 0, 0),
            )

            val brightCenterDarkNW = arrayOf(
                intArrayOf(-1, -1, 0),
                intArrayOf(-1, +w, 0),
                intArrayOf(0, 0, 0),
            )

            // --- Тёмный центр, яркая сторона (8 направлений) ---

            val darkCenterBrightN = arrayOf(
                intArrayOf(+1, +1, +1),
                intArrayOf(0, -w, 0),
                intArrayOf(0, 0, 0),
            )

            val darkCenterBrightNE = arrayOf(
                intArrayOf(0, +1, +1),
                intArrayOf(0, -w, +1),
                intArrayOf(0, 0, 0),
            )

            val darkCenterBrightE = arrayOf(
                intArrayOf(0, 0, +1),
                intArrayOf(0, -w, +1),
                intArrayOf(0, 0, +1),
            )

            val darkCenterBrightSE = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(0, -w, +1),
                intArrayOf(0, +1, +1),
            )

            val darkCenterBrightS = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(0, -w, 0),
                intArrayOf(+1, +1, +1),
            )

            val darkCenterBrightSW = arrayOf(
                intArrayOf(0, 0, 0),
                intArrayOf(+1, -w, 0),
                intArrayOf(+1, +1, 0),
            )

            val darkCenterBrightW = arrayOf(
                intArrayOf(+1, 0, 0),
                intArrayOf(+1, -w, 0),
                intArrayOf(+1, 0, 0),
            )

            val darkCenterBrightNW = arrayOf(
                intArrayOf(+1, +1, 0),
                intArrayOf(+1, -w, 0),
                intArrayOf(0, 0, 0),
            )

            return listOf(
                DetectorMask(brightCenterDarkN, maskRadius),
                DetectorMask(brightCenterDarkNE, maskRadius),
                DetectorMask(brightCenterDarkE, maskRadius),
                DetectorMask(brightCenterDarkSE, maskRadius),
                DetectorMask(brightCenterDarkS, maskRadius),
                DetectorMask(brightCenterDarkSW, maskRadius),
                DetectorMask(brightCenterDarkW, maskRadius),
                DetectorMask(brightCenterDarkNW, maskRadius),
                DetectorMask(darkCenterBrightN, maskRadius),
                DetectorMask(darkCenterBrightNE, maskRadius),
                DetectorMask(darkCenterBrightE, maskRadius),
                DetectorMask(darkCenterBrightSE, maskRadius),
                DetectorMask(darkCenterBrightS, maskRadius),
                DetectorMask(darkCenterBrightSW, maskRadius),
                DetectorMask(darkCenterBrightW, maskRadius),
                DetectorMask(darkCenterBrightNW, maskRadius),
            )
        }
    }

    fun generateConfig(): ImageEncoderConfig {
        requireNotNull(masks) { "Detector cluster must be initialized before use. Pleas call DetectorCluster.initialize()" }

        val codeToEdgeType = mutableMapOf<String, EdgePrototype>()

        EdgePrototype.entries.forEach { prototype ->
            // Прототипы хранятся как идеальные ±1 паттерны (IntArray);
            // переводим в DoubleArray, чтобы прогнать через непрерывный конвейер детекторов.
            val prototypePixels = Array(prototype.data.size) { y ->
                DoubleArray(prototype.data[y].size) { x -> prototype.data[y][x].toDouble() }
            }
            val code = encode(RawImage(pixels = prototypePixels))

            codeToEdgeType[code.joinToString("")] = prototype
        }

        return ImageEncoderConfig(
            clusterCodeDimension = masks!!.size + 1,
            maskRadius = maskRadius,
            codeToEdgeType = codeToEdgeType,
            stride = stride,
        )
    }

    // Выход кластера: 16 бит направленных детекторов + 1 бит полярности центра.
    // Бит полярности нужен, чтобы различать однородно-тёмную и однородно-яркую области,
    // где все направленные детекторы молчат (они по определению не видят однородность).
    fun encode(image: RawImage): IntArray {
        requireNotNull(masks) { "Detector cluster must be initialized before use. Pleas call DetectorCluster.initialize()" }

        val maskCount = masks!!.size
        val result = IntArray(maskCount + 1)

        masks!!.forEachIndexed { index, mask ->
            val detectorValue = mask.apply(image, centerX = centerX, centerY = centerY)
            result[index] = if (detectorValue >= detectorActivationThreshold) 1 else 0
        }

        result[maskCount] = if (image.getPixel(centerX, centerY) > 0.0) 1 else 0

        return result
    }
}
