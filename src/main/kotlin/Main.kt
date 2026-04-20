package org.example

import org.example.perception.visual.data.ImageLoader
import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.EncodedImageVisualizer
import org.example.perception.visual.domain.ImageEncoder

fun main() {
    val image = ImageLoader.loadImageFromPng("src/main/resources/synthetic/00_canonical.png")

    val encoder = ImageEncoder(maskRadius = 1, detectorActivationThreshold = 5.0, stride = 1)
    val config = encoder.initialize()

    val visualizer = EncodedImageVisualizer(encoderConfig = config)

    val encoded = encoder.encode(image)

    println("Original image size: ${image.width}x${image.height}")
    println("Encoded vector dimension: ${encoded.size}")
    println("Encoded vector: ${encoded.joinToString("")}")

    // Диагностика: какие коды кластера остались вне словаря прототипов.
    // Если список пустой — серых клеток в визуализации не будет.
    val unknownCodes = mutableMapOf<String, Int>()
    var offset = 0
    while (offset + config.clusterCodeDimension <= encoded.size) {
        val code = encoded.sliceArray(offset until offset + config.clusterCodeDimension).joinToString("")
        if (code !in config.codeToEdgeType) {
            unknownCodes[code] = (unknownCodes[code] ?: 0) + 1
        }
        offset += config.clusterCodeDimension
    }
    if (unknownCodes.isEmpty()) {
        println("\nAll cluster codes are covered by the prototype dictionary.")
    } else {
        val total = unknownCodes.values.sum()
        println("\nUnknown cluster codes ($total cells across ${unknownCodes.size} unique patterns):")
        unknownCodes.entries.sortedByDescending { it.value }.forEach { (code, count) ->
            println("  $code  ×$count")
        }
    }

    println("\nVisualized encoded image:")
    visualizer.visualizeGraphically(encoded, image.width, image.height)
}