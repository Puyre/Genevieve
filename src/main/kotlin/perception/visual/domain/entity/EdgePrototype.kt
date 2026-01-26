package org.example.perception.visual.domain.entity

enum class EdgePrototype(
    val type: EdgeType,
    val data: Array<IntArray>
) {
    HORIZONTAL_DARK_BOTTOM(
        type = EdgeType.HORIZONTAL,
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
        )
    ),
    HORIZONTAL_DARK_TOP(
        type = EdgeType.HORIZONTAL,
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
            intArrayOf(1, 1, 1),
        )
    ),
    VERTICAL_DARK_LEFT(
        type = EdgeType.VERTICAL,
        data = arrayOf(
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
        )
    ),
    VERTICAL_DARK_RIGHT(
        type = EdgeType.VERTICAL,
        data = arrayOf(
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
        )
    ),
    DIAGONAL_DARK_BOTTOM_RIGHT(
        type = EdgeType.DIAGONAL_45,
        data = arrayOf(
            intArrayOf(1, 1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    DIAGONAL_DARK_TOP_LEFT(
        type = EdgeType.DIAGONAL_45,
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, 1, 1),
        ),
    ),
    DIAGONAL_DARK_BOTTOM_LEFT(
        type = EdgeType.DIAGONAL_135,
        data = arrayOf(
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    DIAGONAL_DARK_TOP_RIGHT(
        type = EdgeType.DIAGONAL_135,
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(1, 1, -1),
        ),
    )
}