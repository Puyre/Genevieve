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

    private enum class Direction { N, NE, E, SE, S, SW, W, NW }

    private enum class Corner { NE, SE, SW, NW }

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
    //  • dark-center прототип   → чёрный фон + светлые маркеры со стороны ярких областей;
    //  • bright-center прототип → белый фон + тёмные маркеры со стороны тёмных областей;
    //  • STEP_*                  → два маркера на двух соседних сторонах (удвоенное угловое разрешение);
    //  • линия                    → чёрный фон + яркая толстая линия через центр;
    //  • пустой тёмный/яркий      → сплошной цвет;
    //  • неизвестный код          → серый квадрат.
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
            EdgePrototype.LINE_HORIZONTAL -> g.fillRect(x, y + (cellSize - stripe) / 2, cellSize, stripe)
            EdgePrototype.LINE_VERTICAL -> g.fillRect(x + (cellSize - stripe) / 2, y, stripe, cellSize)
            EdgePrototype.LINE_DIAGONAL_TLBR -> drawThickDiagonal(g, x, y, fromTopLeft = true, stripe = stripe)
            EdgePrototype.LINE_DIAGONAL_TRBL -> drawThickDiagonal(g, x, y, fromTopLeft = false, stripe = stripe)
            else -> for (direction in directionsFor(edgeType)) {
                drawDirectionMarker(g, x, y, direction, stripe)
            }
        }
    }

    // Сопоставление прототипа → направления, куда смотрят маркеры.
    // Для одиночных границ — одно направление, для ступенчатых — два соседних.
    // Пустые и линейные прототипы не используют маркеры (у них собственная отрисовка).
    private fun directionsFor(edgeType: EdgePrototype): List<Direction> = when (edgeType) {
        EdgePrototype.HORIZONTAL_DARK_TOP -> listOf(Direction.S)
        EdgePrototype.HORIZONTAL_DARK_BOTTOM -> listOf(Direction.N)
        EdgePrototype.VERTICAL_DARK_LEFT -> listOf(Direction.E)
        EdgePrototype.VERTICAL_DARK_RIGHT -> listOf(Direction.W)
        EdgePrototype.DIAGONAL_DARK_TOP_LEFT -> listOf(Direction.SE)
        EdgePrototype.DIAGONAL_DARK_TOP_RIGHT -> listOf(Direction.SW)
        EdgePrototype.DIAGONAL_DARK_BOTTOM_LEFT -> listOf(Direction.NE)
        EdgePrototype.DIAGONAL_DARK_BOTTOM_RIGHT -> listOf(Direction.NW)

        EdgePrototype.HORIZONTAL_BRIGHT_BOTTOM -> listOf(Direction.N)
        EdgePrototype.HORIZONTAL_BRIGHT_TOP -> listOf(Direction.S)
        EdgePrototype.VERTICAL_BRIGHT_RIGHT -> listOf(Direction.W)
        EdgePrototype.VERTICAL_BRIGHT_LEFT -> listOf(Direction.E)
        EdgePrototype.DIAGONAL_BRIGHT_TOP_LEFT -> listOf(Direction.SE)
        EdgePrototype.DIAGONAL_BRIGHT_TOP_RIGHT -> listOf(Direction.SW)
        EdgePrototype.DIAGONAL_BRIGHT_BOTTOM_LEFT -> listOf(Direction.NE)
        EdgePrototype.DIAGONAL_BRIGHT_BOTTOM_RIGHT -> listOf(Direction.NW)

        EdgePrototype.STEP_BRIGHT_NNE -> listOf(Direction.N, Direction.NE)
        EdgePrototype.STEP_BRIGHT_ENE -> listOf(Direction.NE, Direction.E)
        EdgePrototype.STEP_BRIGHT_ESE -> listOf(Direction.E, Direction.SE)
        EdgePrototype.STEP_BRIGHT_SSE -> listOf(Direction.SE, Direction.S)
        EdgePrototype.STEP_BRIGHT_SSW -> listOf(Direction.S, Direction.SW)
        EdgePrototype.STEP_BRIGHT_WSW -> listOf(Direction.SW, Direction.W)
        EdgePrototype.STEP_BRIGHT_WNW -> listOf(Direction.W, Direction.NW)
        EdgePrototype.STEP_BRIGHT_NNW -> listOf(Direction.NW, Direction.N)

        EdgePrototype.STEP_DARK_NNE -> listOf(Direction.N, Direction.NE)
        EdgePrototype.STEP_DARK_ENE -> listOf(Direction.NE, Direction.E)
        EdgePrototype.STEP_DARK_ESE -> listOf(Direction.E, Direction.SE)
        EdgePrototype.STEP_DARK_SSE -> listOf(Direction.SE, Direction.S)
        EdgePrototype.STEP_DARK_SSW -> listOf(Direction.S, Direction.SW)
        EdgePrototype.STEP_DARK_WSW -> listOf(Direction.SW, Direction.W)
        EdgePrototype.STEP_DARK_WNW -> listOf(Direction.W, Direction.NW)
        EdgePrototype.STEP_DARK_NNW -> listOf(Direction.NW, Direction.N)

        EdgePrototype.LINE_HORIZONTAL,
        EdgePrototype.LINE_VERTICAL,
        EdgePrototype.LINE_DIAGONAL_TLBR,
        EdgePrototype.LINE_DIAGONAL_TRBL,
        EdgePrototype.EMPTY_BLACK,
        EdgePrototype.EMPTY_WHITE -> emptyList()
    }

    private fun drawDirectionMarker(g: Graphics2D, x: Int, y: Int, direction: Direction, stripe: Int) {
        when (direction) {
            Direction.N -> g.fillRect(x, y, cellSize, stripe)
            Direction.S -> g.fillRect(x, y + cellSize - stripe, cellSize, stripe)
            Direction.W -> g.fillRect(x, y, stripe, cellSize)
            Direction.E -> g.fillRect(x + cellSize - stripe, y, stripe, cellSize)
            Direction.NE -> fillCornerTriangle(g, x, y, Corner.NE)
            Direction.SE -> fillCornerTriangle(g, x, y, Corner.SE)
            Direction.SW -> fillCornerTriangle(g, x, y, Corner.SW)
            Direction.NW -> fillCornerTriangle(g, x, y, Corner.NW)
        }
    }

    private fun fillCornerTriangle(g: Graphics2D, x: Int, y: Int, corner: Corner) {
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
