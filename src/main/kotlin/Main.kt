package org.example

import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.EncodedImageVisualizer
import org.example.perception.visual.domain.ImageEncoder

fun main() {
    // Изображение 9x9 с кругом в центре
    val pixels = arrayOf(
        intArrayOf(-1, -1, -1, +1, +1, +1, -1, -1, -1),
        intArrayOf(-1, -1, +1, -1, -1, -1, +1, -1, -1),
        intArrayOf(-1, +1, -1, -1, -1, -1, -1, +1, -1),
        intArrayOf(+1, -1, -1, -1, -1, -1, -1, -1, +1),
        intArrayOf(+1, -1, -1, -1, -1, -1, -1, -1, +1),
        intArrayOf(+1, -1, -1, -1, -1, -1, -1, -1, +1),
        intArrayOf(-1, +1, -1, -1, -1, -1, -1, +1, -1),
        intArrayOf(-1, -1, +1, -1, -1, -1, +1, -1, -1),
        intArrayOf(-1, -1, -1, +1, +1, +1, -1, -1, -1)
    )

    val image = RawImage(pixels)
    val encoder = ImageEncoder(maskRadius = 1, detectorActivationThreshold = 2)
    val visualizer = EncodedImageVisualizer(maskRadius = 1, clusterCodeDimension = 8)

    val encoded = encoder.encode(image)

    println("Original image size: ${image.width}x${image.height}")
    println("Encoded vector dimension: ${encoded.size}")
    println("Encoded vector: ${encoded.joinToString("") }}")
    println("\nVisualized encoded image:")
    println(visualizer.visualize(encoded, image.width, image.height))
}