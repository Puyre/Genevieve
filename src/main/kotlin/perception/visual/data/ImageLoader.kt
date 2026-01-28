package org.example.perception.visual.data

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ImageLoader {

    fun loadImageFromPng(path: String, threshold: Int = 32): RawImage {
        val file = File(path)
        val bufferedImage: BufferedImage = ImageIO.read(file)

        val width = bufferedImage.width
        val height = bufferedImage.height

        val pixels = Array(height) { y ->
            IntArray(width) { x ->
                val rgb = bufferedImage.getRGB(x, y)
                // Берем синий канал (для grayscale все каналы одинаковые)
                val gray = rgb and 0xFF
                if (gray >= threshold) 1 else -1
            }
        }

        return RawImage(pixels)
    }
}