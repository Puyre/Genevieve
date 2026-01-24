package org.example.perception.visual.data

class RawImage(
    private val pixels: Array<IntArray>,
) {

    val height: Int = pixels.size
    val width: Int = if (height > 0) pixels[0].size else 0

    fun getPixel(x: Int, y: Int): Int {
        return pixels[y][x]
    }
}