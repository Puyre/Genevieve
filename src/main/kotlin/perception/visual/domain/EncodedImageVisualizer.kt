package org.example.perception.visual.domain

import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Рисует активных детекторов поверх приглушённой подложки исходной картинки.
 *
 * Для каждой позиции сетки, в которой сработал хоть один детектор, показывает
 * один полукруг: его плоская граница (диаметр) перпендикулярна усреднённому
 * направлению θ сработавших в этой точке детекторов, а закрашенная половина
 * смотрит в это направление — то есть в ту сторону, где детекторы «видят»
 * яркую часть границы. Цвет полукруга кодирует угол θ.
 */
class EncodedImageVisualizer(
    private val config: ImageEncoderConfig,
    private val scale: Int = 22,
    private val margin: Int = 12,
) {

    fun showWindow(image: RawImage, code: IntArray, title: String = "Encoded image") {
        require(code.size == config.detectors.size) {
            "Вектор длины ${code.size} не соответствует сетчатке из ${config.detectors.size} детекторов"
        }

        SwingUtilities.invokeLater {
            val frame = JFrame(title)
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

            val panel = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    paint(g as Graphics2D, image, code)
                }
            }
            panel.preferredSize = Dimension(
                config.imageWidth * scale + margin * 2,
                config.imageHeight * scale + margin * 2,
            )
            frame.contentPane.add(panel)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
    }

    private fun paint(g: Graphics2D, image: RawImage, code: IntArray) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = Color.BLACK
        g.fillRect(0, 0, config.imageWidth * scale + margin * 2, config.imageHeight * scale + margin * 2)

        // Приглушённая подложка — само изображение. Нужна, чтобы глазом было
        // видно, в какой части картинки активные детекторы.
        for (y in 0 until config.imageHeight) {
            for (x in 0 until config.imageWidth) {
                val raw = image.getPixel(x, y) // [-1, +1]
                val v = ((raw + 1.0) * 0.5 * 90.0).toInt().coerceIn(0, 255)
                g.color = Color(v, v, v)
                g.fillRect(margin + x * scale, margin + y * scale, scale, scale)
            }
        }

        // Собираем активных детекторов по позициям сетки и считаем для каждой
        // точки круговую среднюю θ (через сумму единичных векторов).
        val sumX = HashMap<Long, Double>()
        val sumY = HashMap<Long, Double>()
        val count = HashMap<Long, Int>()

        for (index in code.indices) {
            if (code[index] == 0) continue
            val spec = config.detectors[index]
            val key = (spec.cx.toLong() shl 32) or (spec.cy.toLong() and 0xFFFFFFFFL)
            sumX[key] = (sumX[key] ?: 0.0) + cos(spec.theta)
            sumY[key] = (sumY[key] ?: 0.0) + sin(spec.theta)
            count[key] = (count[key] ?: 0) + 1
        }

        val arcRadius = scale / 2 - 1
        g.stroke = BasicStroke(1.0f)
        for (key in count.keys) {
            val cx = (key shr 32).toInt()
            val cy = (key and 0xFFFFFFFFL).toInt()
            val meanTheta = atan2(sumY.getValue(key), sumX.getValue(key))

            val centerPx = margin + cx * scale + scale / 2
            val centerPy = margin + cy * scale + scale / 2
            val boxX = centerPx - arcRadius
            val boxY = centerPy - arcRadius
            val boxSize = arcRadius * 2

            // Наш θ измеряется в радианах от +x к +y (y вниз по экрану).
            // AWT fillArc считает угол от +x к −y (CCW в обычном смысле).
            // Поэтому awt_deg = −θ_rad. Полукруг идёт от θ−90° до θ+90°,
            // то есть startAngle = awt_deg − 90°, arcAngle = 180°.
            val awtDeg = -meanTheta * 180.0 / PI
            val startAngle = (awtDeg - 90.0).roundToInt()

            val hue = (((meanTheta / (2.0 * PI)) + 1.0) % 1.0).toFloat()
            val fillColor = Color.getHSBColor(hue, 0.85f, 1.0f)

            g.color = fillColor
            g.fillArc(boxX, boxY, boxSize, boxSize, startAngle, 180)

            g.color = Color(255, 255, 255, 120)
            g.drawOval(boxX, boxY, boxSize, boxSize)
        }
    }
}
