package org.example.perception.visual.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class EdgeType {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL_45,
    DIAGONAL_135
}