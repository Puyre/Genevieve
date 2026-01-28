package org.example.perception.visual.domain

import org.example.perception.visual.domain.entity.EdgePrototype
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.awt.Color
import java.awt.Graphics
import java.awt.Polygon
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import java.awt.Dimension

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

                            drawCell(g, x * cellSize, y * cellSize, edgeType)
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

    private fun drawCell(g: Graphics, x: Int, y: Int, edgeType: EdgePrototype?) {
        when (edgeType) {
            EdgePrototype.HORIZONTAL_DARK_TOP -> {
                // Верхняя половина черная
                g.color = Color.BLACK
                g.fillRect(x, y, cellSize, cellSize / 2)
                // Нижняя половина белая
                g.color = Color.WHITE
                g.fillRect(x, y + cellSize / 2, cellSize, cellSize / 2)
            }


            EdgePrototype.HORIZONTAL_DARK_BOTTOM -> {
                // Верхняя половина белая
                g.color = Color.WHITE
                g.fillRect(x, y, cellSize, cellSize / 2)
                // Нижняя половина черная
                g.color = Color.BLACK
                g.fillRect(x, y + cellSize / 2, cellSize, cellSize / 2)
            }

            EdgePrototype.VERTICAL_DARK_LEFT -> {
                // Левая половина черная
                g.color = Color.BLACK
                g.fillRect(x, y, cellSize / 2, cellSize)
                // Правая половина белая
                g.color = Color.WHITE
                g.fillRect(x + cellSize / 2, y, cellSize / 2, cellSize)
            }

            EdgePrototype.VERTICAL_DARK_RIGHT -> {
                // Левая половина белая
                g.color = Color.WHITE
                g.fillRect(x, y, cellSize / 2, cellSize)
                // Правая половина черная
                g.color = Color.BLACK
                g.fillRect(x + cellSize / 2, y, cellSize / 2, cellSize)
            }

            EdgePrototype.DIAGONAL_DARK_BOTTOM_RIGHT -> {
                // Черный треугольник (нижний-правый)
                g.color = Color.BLACK
                val poly1 = Polygon(
                    intArrayOf(x, x + cellSize, x + cellSize),
                    intArrayOf(y + cellSize, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly1)

                // Белый треугольник (верхний-левый)
                g.color = Color.WHITE
                val poly2 = Polygon(
                    intArrayOf(x, x + cellSize, x),
                    intArrayOf(y, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly2)
            }

            EdgePrototype.DIAGONAL_DARK_TOP_LEFT -> {
                // Белый треугольник (нижний-правый)
                g.color = Color.WHITE
                val poly1 = Polygon(
                    intArrayOf(x, x + cellSize, x + cellSize),
                    intArrayOf(y + cellSize, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly1)

                // Черный треугольник (верхний-левый)
                g.color = Color.BLACK
                val poly2 = Polygon(
                    intArrayOf(x, x + cellSize, x),
                    intArrayOf(y, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly2)
            }

            EdgePrototype.DIAGONAL_DARK_BOTTOM_LEFT -> {
                // Черный треугольник (нижний-левый)
                g.color = Color.BLACK
                val poly1 = Polygon(
                    intArrayOf(x, x + cellSize, x),
                    intArrayOf(y, y + cellSize, y + cellSize),
                    3
                )
                g.fillPolygon(poly1)

                // Белый треугольник (верхний-правый)
                g.color = Color.WHITE
                val poly2 = Polygon(
                    intArrayOf(x, x + cellSize, x + cellSize),
                    intArrayOf(y, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly2)
            }

            EdgePrototype.DIAGONAL_DARK_TOP_RIGHT -> {
                // Белый треугольник (нижний-левый)
                g.color = Color.WHITE
                val poly1 = Polygon(
                    intArrayOf(x, x + cellSize, x),
                    intArrayOf(y, y + cellSize, y + cellSize),
                    3
                )
                g.fillPolygon(poly1)

                // Черный треугольник (верхний-правый)
                g.color = Color.BLACK
                val poly2 = Polygon(
                    intArrayOf(x, x + cellSize, x + cellSize),
                    intArrayOf(y, y, y + cellSize),
                    3
                )
                g.fillPolygon(poly2)
            }

            EdgePrototype.EMPTY -> {
                g.color = Color.WHITE
                g.fillRect(x, y, cellSize, cellSize)
            }

            null -> {
                // Серый квадрат
                g.color = Color.GRAY
                g.fillRect(x, y, cellSize, cellSize)
            }
        }
    }
}