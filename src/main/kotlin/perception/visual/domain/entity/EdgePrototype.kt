package org.example.perception.visual.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class EdgePrototype(
    val data: Array<IntArray>,
) {
    // --- Границы, наблюдаемые с тёмной стороны (центр кластера = тёмный пиксель) ---

    HORIZONTAL_DARK_BOTTOM(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    HORIZONTAL_DARK_TOP(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
            intArrayOf(1, 1, 1),
        ),
    ),
    VERTICAL_DARK_LEFT(
        data = arrayOf(
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, -1, 1),
        ),
    ),
    VERTICAL_DARK_RIGHT(
        data = arrayOf(
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
            intArrayOf(1, -1, -1),
        ),
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

    // --- Границы, наблюдаемые с яркой стороны (центр кластера = яркий пиксель) ---

    HORIZONTAL_BRIGHT_BOTTOM(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
        ),
    ),
    HORIZONTAL_BRIGHT_TOP(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    VERTICAL_BRIGHT_RIGHT(
        data = arrayOf(
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, 1, 1),
        ),
    ),
    VERTICAL_BRIGHT_LEFT(
        data = arrayOf(
            intArrayOf(1, 1, -1),
            intArrayOf(1, 1, -1),
            intArrayOf(1, 1, -1),
        ),
    ),
    DIAGONAL_BRIGHT_TOP_LEFT(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, -1),
            intArrayOf(1, -1, -1),
        ),
    ),
    DIAGONAL_BRIGHT_BOTTOM_RIGHT(
        data = arrayOf(
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, 1, 1),
            intArrayOf(1, 1, 1),
        ),
    ),
    DIAGONAL_BRIGHT_TOP_RIGHT(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, -1, 1),
        ),
    ),
    DIAGONAL_BRIGHT_BOTTOM_LEFT(
        data = arrayOf(
            intArrayOf(1, -1, -1),
            intArrayOf(1, 1, -1),
            intArrayOf(1, 1, 1),
        ),
    ),

    // --- Линии (яркая полоса через яркий центр, тьма по обеим сторонам) ---

    LINE_HORIZONTAL(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(1, 1, 1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    LINE_VERTICAL(
        data = arrayOf(
            intArrayOf(-1, 1, -1),
            intArrayOf(-1, 1, -1),
            intArrayOf(-1, 1, -1),
        ),
    ),
    LINE_DIAGONAL_TLBR(
        data = arrayOf(
            intArrayOf(1, -1, -1),
            intArrayOf(-1, 1, -1),
            intArrayOf(-1, -1, 1),
        ),
    ),
    LINE_DIAGONAL_TRBL(
        data = arrayOf(
            intArrayOf(-1, -1, 1),
            intArrayOf(-1, 1, -1),
            intArrayOf(1, -1, -1),
        ),
    ),

    // --- Однородные области ---

    EMPTY_BLACK(
        data = arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, -1, -1),
        ),
    ),
    EMPTY_WHITE(
        data = arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
        ),
    ),
}
