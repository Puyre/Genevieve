package org.example.perception.visual.data

class DetectorMask(
    private val weights: Array<IntArray>,
    private val maskRadius: Int
) {

    fun apply(image: RawImage, centerX: Int, centerY: Int): Int {
        var sum = 0

        for (dy in -maskRadius..maskRadius) {
            for (dx in -maskRadius..maskRadius) {
                val pixelValue = image.getPixel(x = centerX + dx, y = centerY + dy)
                val maskWeight = weights[dy + maskRadius][dx + maskRadius]
                sum += maskWeight * pixelValue
            }
        }

        return sum
    }
}