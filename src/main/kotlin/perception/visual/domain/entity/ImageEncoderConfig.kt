package org.example.perception.visual.domain.entity

import kotlinx.serialization.Serializable

/**
 * Сериализуемая форма всей сетчатки. В JSON хранится:
 *  — общие параметры (размер ожидаемой картинки, радиус, шаг, плотность, порог);
 *  — полный список детекторов с их случайно выбранными предпочтительными направлениями.
 *
 * Сетчатка создаётся один раз через [org.example.perception.visual.domain.ImageEncoder.initialize]
 * и дальше только загружается из этого файла, чтобы коды были одинаковы между запусками.
 */
@Serializable
data class ImageEncoderConfig(
    val imageWidth: Int,
    val imageHeight: Int,
    val radius: Int,
    val stride: Int,
    val detectorsPerCell: Int,
    val threshold: Double,
    val detectors: List<DetectorSpec>,
)

/**
 * Сериализуемое описание одного детектора: его позиция на сетке и случайное
 * предпочтительное направление [theta] в радианах. Общие параметры (радиус
 * окошка, порог) живут в [ImageEncoderConfig].
 */
@Serializable
data class DetectorSpec(
    val cx: Int,
    val cy: Int,
    val theta: Double,
)
