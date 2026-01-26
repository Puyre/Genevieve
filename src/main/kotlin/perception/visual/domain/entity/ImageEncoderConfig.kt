package org.example.perception.visual.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class ImageEncoderConfig(
    val clusterCodeDimension: Int,
    val maskRadius: Int,
    val codeToEdgeType: Map<String, EdgeType>
)