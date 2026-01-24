package org.example.perception.visual.domain

import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.entity.DetectorCluster

class ImageEncoder(
    private val maskRadius: Int,
    private val detectorActivationThreshold: Int
) {

    init {
        DetectorCluster.initialize(maskRadius = maskRadius, detectorActivationThreshold = detectorActivationThreshold)
    }

    fun encode(image: RawImage): IntArray {
        val clusterCodes = mutableListOf<IntArray>()

        for (y in maskRadius until image.height - maskRadius) {
            for (x in maskRadius until image.width - maskRadius) {
                val cluster = DetectorCluster(centerX = x, centerY = y)
                val code = cluster.encode(image)

                clusterCodes.add(code)
            }
        }

        val totalSize = clusterCodes.sumOf { it.size }
        val result = IntArray(totalSize)
        var offset = 0

        clusterCodes.forEach { code ->
            code.copyInto(result, offset)
            offset += code.size
        }

        return result
    }
}