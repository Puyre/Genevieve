package org.example

import org.example.perception.visual.data.ImageLoader
import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.EncodedImageVisualizer
import org.example.perception.visual.domain.ImageEncoder

fun main() {
    val image = ImageLoader.loadImageFromPng("src/main/resources/mnist/993.png")

    val encoder = ImageEncoder(maskRadius = 1, detectorActivationThreshold = 2, stride = 3)
    val config = encoder.initialize()

    val visualizer = EncodedImageVisualizer(encoderConfig = config)

    val encoded = encoder.encode(image)

    println("Original image size: ${image.width}x${image.height}")
    println("Encoded vector dimension: ${encoded.size}")
    println("Encoded vector: ${encoded.joinToString("")}")
    println("\nVisualized encoded image:")
    visualizer.visualizeGraphically(encoded, image.width, image.height)
}