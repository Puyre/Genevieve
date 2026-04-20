package org.example.perception.visual.data

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ImageLoader {

    // Серый канал PNG приводится к непрерывному диапазону [-1.0, +1.0]:
    // 0 (чёрный) → −1, 255 (белый) → +1, 127.5 (средний) → 0.
    // Это сохраняет антиалиасинг на границах штрихов — информацию,
    // которую бинаризация ранее уничтожала.
    fun loadImageFromPng(path: String): RawImage {
        val file = File(path)
        val bufferedImage: BufferedImage = ImageIO.read(file)

        val width = bufferedImage.width
        val height = bufferedImage.height

        val pixels = Array(height) { y ->
            DoubleArray(width) { x ->
                val rgb = bufferedImage.getRGB(x, y)
                // Для grayscale все каналы одинаковые — берём синий.
                val gray = rgb and 0xFF
                (gray / 127.5) - 1.0
            }
        }

        return RawImage(pixels)
    }
}
