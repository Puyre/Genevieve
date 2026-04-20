package org.example.perception.visual.domain

import org.example.perception.visual.domain.entity.EdgePrototype
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

class EncodedImageVisualizer(
    private val encoderConfig: ImageEncoderConfig,
    private val cellSize: Int = 20,
) {

    fun visualizeGraphically(encodedVector: IntArray, imageWidth: Int, imageHeight: Int) {
        val gridWidth = (imageWidth - 2 * encoderConfig.maskRadius + encoderConfig.stride - 1) / encoderConfig.stride
        val gridHeight = (imageHeight - 2 * encoderConfig.maskRadius + encoderConfig.stride - 1) / encoderConfig.stride

        val windowWidth = gridWidth * cellSize
        val windowHeight = gridHeight * cellSize

        javax.swing.SwingUtilities.invokeLater {
            val frame = JFrame("Encoded Image Visualization")
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

            val panel = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    var offset = 0

                    for (y in 0 until gridHeight) {
                        for (x in 0 until gridWidth) {
                            val clusterCode =
                                encodedVector.sliceArray(offset until offset + encoderConfig.clusterCodeDimension)
                                    .joinToString("")
                            offset += encoderConfig.clusterCodeDimension

                            val edgeType = encoderConfig.codeToEdgeType[clusterCode]

                            drawCell(g as Graphics2D, x * cellSize, y * cellSize, edgeType)
                        }
                    }
                }
            }

            panel.preferredSize = Dimension(windowWidth, windowHeight)
            frame.contentPane.add(panel)
            frame.pack()
            frame.isVisible = true
        }
    }

    // Принцип отрисовки:
    //  • dark-center прототип    → чёрный фон + светлая полоса/треугольник со стороны яркой области;
    //  • bright-center прототип  → белый фон + тёмная полоса/треугольник со стороны тёмной области;
    //  • линия                    → чёрный фон + яркая толстая линия через центр;
    //  • пустой тёмный/яркий      → сплошной цвет;
    //  • неизвестный код          → серый квадрат.
    //
    // Таким образом на 2-пиксельном белом штрихе мы видим последовательность:
    // чёрная клетка → клетка с тонкой светлой полосой снизу → белая клетка с тонкой тёмной полосой сверху
    // → белая клетка с тонкой тёмной полосой снизу → клетка с тонкой светлой полосой сверху → чёрная.
    // Это и есть «прямоугольник с верхней и нижней границей».
    private fun drawCell(g: Graphics2D, x: Int, y: Int, edgeType: EdgePrototype?) {
        if (edgeType == null) {
            g.color = Color.GRAY
            g.fillRect(x, y, cellSize, cellSize)
            return
        }

        if (edgeType == EdgePrototype.EMPTY_BLACK) {
            g.color = Color.BLACK
            g.fillRect(x, y, cellSize, cellSize)
            return
        }

        if (edgeType == EdgePrototype.EMPTY_WHITE) {
            g.color = Color.WHITE
            g.fillRect(x, y, cellSize, cellSize)
            return
        }

        val centerIsBright = edgeType.data[1][1] > 0
        val bgColor = if (centerIsBright) Color.WHITE else Color.BLACK
        val fgColor = if (centerIsBright) Color.BLACK else Color.WHITE

        g.color = bgColor
        g.fillRect(x, y, cellSize, cellSize)

        val stripe = maxOf(2, cellSize / 5)

        g.color = fgColor
        when (edgeType) {
            // Dark-center: светлая полоса указывает сторону, где яркая область.
            EdgePrototype.HORIZONTAL_DARK_TOP -> g.fillRect(x, y + cellSize - stripe, cellSize, stripe)
            EdgePrototype.HORIZONTAL_DARK_BOTTOM -> g.fillRect(x, y, cellSize, stripe)
            EdgePrototype.VERTICAL_DARK_LEFT -> g.fillRect(x + cellSize - stripe, y, stripe, cellSize)
            EdgePrototype.VERTICAL_DARK_RIGHT -> g.fillRect(x, y, stripe, cellSize)
            EdgePrototype.DIAGONAL_DARK_TOP_LEFT -> fillCornerTriangle(g, x, y, Corner.SE)
            EdgePrototype.DIAGONAL_DARK_TOP_RIGHT -> fillCornerTriangle(g, x, y, Corner.SW)
            EdgePrototype.DIAGONAL_DARK_BOTTOM_LEFT -> fillCornerTriangle(g, x, y, Corner.NE)
            EdgePrototype.DIAGONAL_DARK_BOTTOM_RIGHT -> fillCornerTriangle(g, x, y, Corner.NW)

            // Bright-center: тёмная полоса указывает сторону, где тёмная область.
            EdgePrototype.HORIZONTAL_BRIGHT_BOTTOM -> g.fillRect(x, y, cellSize, stripe)
            EdgePrototype.HORIZONTAL_BRIGHT_TOP -> g.fillRect(x, y + cellSize - stripe, cellSize, stripe)
            EdgePrototype.VERTICAL_BRIGHT_RIGHT -> g.fillRect(x, y, stripe, cellSize)
            EdgePrototype.VERTICAL_BRIGHT_LEFT -> g.fillRect(x + cellSize - stripe, y, stripe, cellSize)
            EdgePrototype.DIAGONAL_BRIGHT_TOP_LEFT -> fillCornerTriangle(g, x, y, Corner.SE)
            EdgePrototype.DIAGONAL_BRIGHT_TOP_RIGHT -> fillCornerTriangle(g, x, y, Corner.SW)
            EdgePrototype.DIAGONAL_BRIGHT_BOTTOM_LEFT -> fillCornerTriangle(g, x, y, Corner.NE)
            EdgePrototype.DIAGONAL_BRIGHT_BOTTOM_RIGHT -> fillCornerTriangle(g, x, y, Corner.NW)

            // Линии: яркая толстая полоса через центр клетки.
            EdgePrototype.LINE_HORIZONTAL -> g.fillRect(x, y + (cellSize - stripe) / 2, cellSize, stripe)
            EdgePrototype.LINE_VERTICAL -> g.fillRect(x + (cellSize - stripe) / 2, y, stripe, cellSize)
            EdgePrototype.LINE_DIAGONAL_TLBR -> drawThickDiagonal(g, x, y, fromTopLeft = true, stripe = stripe)
            EdgePrototype.LINE_DIAGONAL_TRBL -> drawThickDiagonal(g, x, y, fromTopLeft = false, stripe = stripe)

            EdgePrototype.EMPTY_BLACK,
            EdgePrototype.EMPTY_WHITE -> Unit
        }
    }

    private enum class Corner { NE, SE, SW, NW }

    private fun fillCornerTriangle(g: Graphics2D, x: Int, y: Int, corner: Corner) {
        // Треугольник занимает примерно треть клетки, упирается вершиной в центр.
        val size = (cellSize * 0.6).toInt()
        val poly = when (corner) {
            Corner.NE -> Polygon(
                intArrayOf(x + cellSize, x + cellSize, x + cellSize - size),
                intArrayOf(y, y + size, y),
                3,
            )
            Corner.SE -> Polygon(
                intArrayOf(x + cellSize, x + cellSize, x + cellSize - size),
                intArrayOf(y + cellSize, y + cellSize - size, y + cellSize),
                3,
            )
            Corner.SW -> Polygon(
                intArrayOf(x, x, x + size),
                intArrayOf(y + cellSize, y + cellSize - size, y + cellSize),
                3,
            )
            Corner.NW -> Polygon(
                intArrayOf(x, x, x + size),
                intArrayOf(y, y + size, y),
                3,
            )
        }
        g.fillPolygon(poly)
    }

    private fun drawThickDiagonal(g: Graphics2D, x: Int, y: Int, fromTopLeft: Boolean, stripe: Int) {
        val originalStroke = g.stroke
        g.stroke = BasicStroke(stripe.toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
        if (fromTopLeft) {
            g.drawLine(x, y, x + cellSize, y + cellSize)
        } else {
            g.drawLine(x + cellSize, y, x, y + cellSize)
        }
        g.stroke = originalStroke
    }
}
