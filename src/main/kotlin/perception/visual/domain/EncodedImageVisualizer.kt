package org.example.perception.visual.domain

import org.example.perception.visual.domain.entity.EdgeType
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

    fun visualize(encodedVector: IntArray, imageWidth: Int, imageHeight: Int): String {
        val canvas = Array(imageHeight) { CharArray(imageWidth) { ' ' } }

        var offset = 0

        for (y in encoderConfig.maskRadius until imageHeight - encoderConfig.maskRadius) {
            for (x in encoderConfig.maskRadius until imageWidth - encoderConfig.maskRadius) {
                val clusterCode =
                    encodedVector.sliceArray(offset until offset + encoderConfig.clusterCodeDimension).joinToString("")

                offset += encoderConfig.clusterCodeDimension

                val edgeType = encoderConfig.codeToEdgeType[clusterCode]

                canvas[y][x] = when (edgeType) {
                    EdgeType.HORIZONTAL -> '-'
                    EdgeType.VERTICAL -> '|'
                    EdgeType.DIAGONAL_45 -> '/'
                    EdgeType.DIAGONAL_135 -> '\\'
                    null -> '?'
                }
            }
        }

        return canvas.joinToString("\n") { it.joinToString("") }
    }

    fun visualizeGraphically(encodedVector: IntArray, imageWidth: Int, imageHeight: Int) {
        val gridWidth = imageWidth - 2 * encoderConfig.maskRadius
        val gridHeight = imageHeight - 2 * encoderConfig.maskRadius

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

    private fun drawCell(g: Graphics, x: Int, y: Int, edgeType: EdgeType?) {
        when (edgeType) {
            EdgeType.HORIZONTAL -> {
                // Верхняя половина черная
                g.color = Color.BLACK
                g.fillRect(x, y, cellSize, cellSize / 2)
                // Нижняя половина белая
                g.color = Color.WHITE
                g.fillRect(x, y + cellSize / 2, cellSize, cellSize / 2)
            }

            EdgeType.VERTICAL -> {
                // Левая половина черная
                g.color = Color.BLACK
                g.fillRect(x, y, cellSize / 2, cellSize)
                // Правая половина белая
                g.color = Color.WHITE
                g.fillRect(x + cellSize / 2, y, cellSize / 2, cellSize)
            }

            EdgeType.DIAGONAL_45 -> {
                // Черный треугольник (нижний-левый)
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

            EdgeType.DIAGONAL_135 -> {
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

            null -> {
                // Серый квадрат
                g.color = Color.GRAY
                g.fillRect(x, y, cellSize, cellSize)
            }
        }
    }
}