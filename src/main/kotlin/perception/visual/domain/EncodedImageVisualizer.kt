package org.example.perception.visual.domain

class EncodedImageVisualizer(
    private val maskRadius: Int,
    private val clusterCodeDimension: Int
) {

    private val codeToSymbol = mapOf(
        "00011100" to '|',
        "11000001" to '|',

        "01110000" to '-',
        "00000111" to '-',

        "00001110" to '/',
        "11100000" to '/',

        "00111000" to '\\',
        "10000011" to '\\'
    )

    fun visualize(encodedVector: IntArray, imageWidth: Int, imageHeight: Int): String {
        val canvas = Array(imageHeight) { CharArray(imageWidth) { ' ' } }

        var offset = 0

        for (y in maskRadius until imageHeight - maskRadius) {
            for (x in maskRadius until imageWidth - maskRadius) {
                val clusterCode = encodedVector.sliceArray(offset until offset + clusterCodeDimension).joinToString("")

                offset += clusterCodeDimension

                canvas[y][x] = codeToSymbol[clusterCode] ?: '?'
            }
        }

        return canvas.joinToString("\n") { it.joinToString("") }
    }
}