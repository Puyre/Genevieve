package org.example.perception.visual.domain.entity

import org.example.perception.visual.data.DetectorMask
import org.example.perception.visual.domain.entity.EdgeType
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import org.example.perception.visual.data.RawImage
import kotlin.text.toInt

class DetectorCluster(
    private val centerX: Int,
    private val centerY: Int,
) {

    companion object {
        private var masks: List<DetectorMask>? = null
        private var maskRadius: Int = 0
        private var detectorActivationThreshold: Int = 0

        fun initialize(maskRadius: Int, detectorActivationThreshold: Int) {
            this.maskRadius = maskRadius
            this.detectorActivationThreshold = detectorActivationThreshold
            this.masks = createMasks(maskRadius)
        }

        private fun createMasks(maskRadius: Int): List<DetectorMask> {
            require(maskRadius == 1) { "Currently only radius=1 is supported" }

            val mask0 = arrayOf(
                intArrayOf(-1, 0, +1),
                intArrayOf(-1, 0, +1),
                intArrayOf(-1, 0, +1)
            )

            val mask45 = arrayOf(
                intArrayOf(-1, -1, 0),
                intArrayOf(-1, 0, +1),
                intArrayOf(0, +1, +1)
            )

            val mask90 = arrayOf(
                intArrayOf(-1, -1, -1),
                intArrayOf(0, 0, 0),
                intArrayOf(+1, +1, +1)
            )

            val mask135 = arrayOf(
                intArrayOf(0, -1, -1),
                intArrayOf(+1, 0, -1),
                intArrayOf(+1, +1, 0)
            )

            val mask180 = arrayOf(
                intArrayOf(+1, 0, -1),
                intArrayOf(+1, 0, -1),
                intArrayOf(+1, 0, -1)
            )

            val mask225 = arrayOf(
                intArrayOf(+1, +1, 0),
                intArrayOf(+1, 0, -1),
                intArrayOf(0, -1, -1)
            )

            val mask270 = arrayOf(
                intArrayOf(+1, +1, +1),
                intArrayOf(0, 0, 0),
                intArrayOf(-1, -1, -1)
            )

            val mask315 = arrayOf(
                intArrayOf(0, +1, +1),
                intArrayOf(-1, 0, +1),
                intArrayOf(-1, -1, 0)
            )

            return listOf(
                DetectorMask(mask0, maskRadius),
                DetectorMask(mask45, maskRadius),
                DetectorMask(mask90, maskRadius),
                DetectorMask(mask135, maskRadius),
                DetectorMask(mask180, maskRadius),
                DetectorMask(mask225, maskRadius),
                DetectorMask(mask270, maskRadius),
                DetectorMask(mask315, maskRadius)
            )
        }
    }

    fun generateConfig(): ImageEncoderConfig {
        requireNotNull(masks) { "Detector cluster must be initialized before use. Pleas call DetectorCluster.initialize()" }

        val codeToEdgeType = mutableMapOf<String, EdgeType>()

        EdgePrototype.entries.forEach { prototype ->
            val code = encode(RawImage(pixels = prototype.data))

            codeToEdgeType[code.joinToString("")] = prototype.type
        }

        return ImageEncoderConfig(
            clusterCodeDimension = masks!!.size,
            maskRadius = maskRadius,
            codeToEdgeType = codeToEdgeType
        )
    }

    fun encode(image: RawImage): IntArray {
        requireNotNull(masks) { "Detector cluster must be initialized before use. Pleas call DetectorCluster.initialize()" }

        val result = IntArray(8)

        masks!!.forEachIndexed { index, mask ->
            val detectorValue = mask.apply(image, centerX = centerX, centerY = centerY)
            result[index] = if (detectorValue >= detectorActivationThreshold) 1 else 0
        }

        return result
    }
}