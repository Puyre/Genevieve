package org.example.perception.visual.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.entity.DetectorCluster
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.io.File

class ImageEncoder(
    private val maskRadius: Int,
    private val detectorActivationThreshold: Int
) {

    fun initialize(): ImageEncoderConfig {
        DetectorCluster.initialize(maskRadius = maskRadius, detectorActivationThreshold = detectorActivationThreshold)

        val tempCluster = DetectorCluster(centerX = maskRadius, centerY = maskRadius)
        val config = tempCluster.generateConfig()

        saveConfigToFile(config)

        return config
    }

    private fun saveConfigToFile(config: ImageEncoderConfig) {
        val json = Json {
            prettyPrint = true
        }

        val jsonString = json.encodeToString(config)

        val configFile = File("image_encoder_config.json")
        configFile.writeText(jsonString)

        println("Config saved to: ${configFile.absolutePath}")
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