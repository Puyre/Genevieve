package org.example.perception.visual.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class EdgePrototype(
    val data: Array<IntArray>
) {
    HORIZONTAL_DARK_BOTTOM(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
        )
    ),
    HORIZONTAL_DARK_TOP(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
            intArrayOf(1, 1, 1),
        )
    ),
    VERTICAL_DARK_LEFT(
        data = arrayOf(
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
        )
    ),
    VERTICAL_DARK_RIGHT(
        data = arrayOf(
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
        )
    ),
    DIAGONAL_DARK_BOTTOM_RIGHT(
        data = arrayOf(
            intArrayOf(1, 1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    DIAGONAL_DARK_TOP_LEFT(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, 1, 1),
        ),
    ),
    DIAGONAL_DARK_BOTTOM_LEFT(
        data = arrayOf(
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    DIAGONAL_DARK_TOP_RIGHT(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(1, 1, -1),
        ),
    ),
    EMPTY_BLACK(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
        )
    ),
    EMPTY_WHITE(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
        )
    )
}