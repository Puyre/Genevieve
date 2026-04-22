package org.example.synthetic

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Генератор синтетического датасета цифры "1" в формате MNIST (28×28 grayscale PNG,
// фон чёрный, штрих белый). Нужен для проверки контекстной системы в управляемых
// условиях: здесь мы точно знаем, чем одна вариация отличается от другой.
object SyntheticDigitGenerator {
    private const val SIZE = 28
    private const val CENTER = (SIZE - 1) / 2.0

    // Длина и толщина эталонного штриха подобраны так, чтобы палка занимала основную
    // часть поля, не упираясь в границы, и оставалась узнаваемой как "1".
    private const val BASE_LENGTH = 22.0
    private const val BASE_THICKNESS = 2.0f

    // Рисует "1" как один прямолинейный штрих с заданной геометрией.
    // angleDeg: 0° — строго вертикально, положительный угол наклоняет верх штриха вправо.
    private fun drawOne(
        centerX: Double,
        centerY: Double,
        length: Double,
        thickness: Float,
        angleDeg: Double,
    ): BufferedImage {
        val img = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY)
        val g = img.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color.BLACK
            g.fillRect(0, 0, SIZE, SIZE)
            g.color = Color.WHITE
            g.stroke = BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

            val rad = Math.toRadians(angleDeg)
            val halfDx = Math.sin(rad) * length / 2
            val halfDy = Math.cos(rad) * length / 2
            g.draw(
                Line2D.Double(
                    centerX + halfDx,
                    centerY - halfDy,
                    centerX - halfDx,
                    centerY + halfDy,
                ),
            )
        } finally {
            g.dispose()
        }
        return img
    }

    fun generate(outputDir: File = File("src/main/resources/synthetic")) {
        outputDir.mkdirs()

        val images = mutableListOf<Pair<String, BufferedImage>>()

        // Эталон: строго центрированная вертикальная палка.
        images += "canonical" to drawOne(CENTER, CENTER, BASE_LENGTH, BASE_THICKNESS, 0.0)

        // Горизонтальные сдвиги (по x).
        listOf(-3, -2, -1, 1, 2, 3).forEach { dx ->
            val tag = if (dx >= 0) "plus_$dx" else "minus_${-dx}"
            images += "shift_x_$tag" to
                drawOne(CENTER + dx, CENTER, BASE_LENGTH, BASE_THICKNESS, 0.0)
        }

        // Вертикальные сдвиги (по y).
        listOf(-2, -1, 1, 2).forEach { dy ->
            val tag = if (dy >= 0) "plus_$dy" else "minus_${-dy}"
            images += "shift_y_$tag" to
                drawOne(CENTER, CENTER + dy, BASE_LENGTH, BASE_THICKNESS, 0.0)
        }

        // Толщина штриха (базовая 2.0, пробуем тоньше и толще).
        listOf(1.0f, 1.5f, 2.5f, 3.0f).forEach { thickness ->
            val tag = (thickness * 10).toInt()
            images += "thick_$tag" to drawOne(CENTER, CENTER, BASE_LENGTH, thickness, 0.0)
        }

        // Длина палки (базовая 22, пробуем короче и длиннее).
        listOf(18.0, 20.0, 24.0, 26.0).forEach { length ->
            images += "len_${length.toInt()}" to
                drawOne(CENTER, CENTER, length, BASE_THICKNESS, 0.0)
        }

        // Наклон (в обе стороны).
        listOf(-10, -5, 5, 10, 15).forEach { angle ->
            val tag = if (angle >= 0) "plus_$angle" else "minus_${-angle}"
            images += "slant_$tag" to
                drawOne(CENTER, CENTER, BASE_LENGTH, BASE_THICKNESS, angle.toDouble())
        }

        images.forEachIndexed { index, (name, img) ->
            val prefix = index.toString().padStart(2, '0')
            val file = File(outputDir, "${prefix}_$name.png")
            ImageIO.write(img, "png", file)
        }

        println("Сгенерировано ${images.size} PNG в ${outputDir.absolutePath}")
    }
}

fun main() {
    SyntheticDigitGenerator.generate()
}
