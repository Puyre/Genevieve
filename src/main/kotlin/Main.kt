package org.example

import org.example.perception.visual.data.ImageLoader
import org.example.perception.visual.domain.EncodedImageVisualizer
import org.example.perception.visual.domain.ImageEncoder
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.io.File

fun main() {
    val configPath = "image_encoder_config.json"
    val encoder = if (File(configPath).exists()) {
        ImageEncoder.load(configPath = configPath)
    } else {
        ImageEncoder.initialize(
            imageWidth = 28,
            imageHeight = 28,
            radius = 2,
            stride = 1,
            detectorsPerCell = 16,
            threshold = 0.5,
            configPath = configPath,
        )
    }

    val imagePath = "src/main/resources/mnist/982.png"
    val image = ImageLoader.loadImageFromPng(imagePath)
    val code = encoder.encode(image)

    printCodeAsMatrix(code, encoder.config)

    EncodedImageVisualizer(encoder.config).showWindow(image, code, title = imagePath)
}

/**
 * Печатает бинарный вектор построчно: один ряд на одну «y-строку» сетки
 * детекторов, внутри ряда чанки по [detectorsPerCell] бит на каждую
 * x-позицию. Активный бит — `#`, неактивный — `.`.
 */
private fun printCodeAsMatrix(code: IntArray, config: ImageEncoderConfig) {
    val xPositions = (config.imageWidth - 1) / config.stride + 1
    val yPositions = (config.imageHeight - 1) / config.stride + 1
    val detectorsPerCell = config.detectorsPerCell
    val activeBits = code.count { it == 1 }

    println("Вектор: длина ${code.size}, активных битов $activeBits (${"%.1f".format(100.0 * activeBits / code.size)}%).")
    println("Сетка: $yPositions строк × $xPositions позиций × $detectorsPerCell детекторов.")
    println()

    var offset = 0
    for (row in 0 until yPositions) {
        val cy = row * config.stride
        val sb = StringBuilder()
        sb.append("cy=%2d | ".format(cy))
        for (col in 0 until xPositions) {
            for (d in 0 until detectorsPerCell) {
                sb.append(if (code[offset + d] == 1) '#' else '.')
            }
            sb.append(' ')
            offset += detectorsPerCell
        }
        println(sb)
    }
}
