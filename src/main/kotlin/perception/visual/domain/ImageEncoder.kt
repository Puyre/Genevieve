package org.example.perception.visual.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.perception.visual.data.RawImage
import org.example.perception.visual.domain.entity.Detector
import org.example.perception.visual.domain.entity.DetectorSpec
import org.example.perception.visual.domain.entity.ImageEncoderConfig
import java.io.File
import kotlin.math.PI
import kotlin.random.Random

/**
 * Перцептивный энкодер — «сетчатка» из одинаковых детекторов, расставленных
 * по регулярной сетке с перекрытием рецептивных полей. По одной картинке
 * выдаёт длинный бинарный вектор: один бит с каждого детектора.
 *
 * Экземпляр создаётся один раз через [initialize] (со случайными θ, с записью
 * в JSON) либо через [load] (из ранее сохранённого JSON).
 */
class ImageEncoder private constructor(
    val config: ImageEncoderConfig,
    private val detectors: List<Detector>,
) {

    /**
     * Прогоняет все детекторы по картинке и собирает их биты в один вектор.
     * Длина результата равна общему числу детекторов в сетчатке.
     */
    fun encode(image: RawImage): IntArray {
        require(image.width == config.imageWidth && image.height == config.imageHeight) {
            "Картинка ${image.width}x${image.height} не совпадает с размером сетчатки ${config.imageWidth}x${config.imageHeight}"
        }
        return IntArray(detectors.size) { index -> detectors[index].respond(image) }
    }

    companion object {
        private val json = Json { prettyPrint = true }

        /**
         * Создаёт новую сетчатку со случайно распределёнными предпочтительными
         * направлениями детекторов и сохраняет её в JSON по пути [configPath].
         *
         * @param imageWidth ширина ожидаемых картинок в пикселях.
         * @param imageHeight высота ожидаемых картинок в пикселях.
         * @param radius радиус рецептивного поля детектора в пикселях.
         *   Окошко детектора имеет размер (2·radius + 1) × (2·radius + 1).
         * @param stride шаг сетки: через сколько пикселей ставится следующая
         *   точка сетки. Чем меньше stride, тем сильнее перекрываются рецептивные
         *   поля соседних детекторов и тем плавнее меняется код при сдвиге картинки.
         * @param detectorsPerCell сколько детекторов со случайными θ сидят в
         *   каждой точке сетки. Чем больше — тем гуще представлены разные
         *   направления границ в одной области.
         * @param threshold порог срабатывания: если сумма после наложения маски
         *   на пиксели больше этого числа — детектор выдаёт 1, иначе 0.
         * @param configPath куда записать JSON с полным описанием сетчатки.
         * @param random источник случайности для выбора θ (по умолчанию — общий).
         */
        fun initialize(
            imageWidth: Int,
            imageHeight: Int,
            radius: Int,
            stride: Int,
            detectorsPerCell: Int,
            threshold: Double,
            configPath: String,
            random: Random = Random.Default,
        ): ImageEncoder {
            val angularStep = 2.0 * PI / detectorsPerCell
            val specs = buildList {
                for (cy in radius..imageHeight - 1 - radius step stride) {
                    for (cx in radius..imageWidth - 1 - radius step stride) {
                        // В каждой точке сетки выбираем случайный сдвиг в диапазоне
                        // одного углового шага и от него ставим детекторы с равным
                        // шагом 2π/detectorsPerCell. Внутри точки все 16 направлений
                        // равномерны (нет кучек близких θ), а случайный сдвиг между
                        // точками не даёт на сетчатке возникнуть «полосам слепоты»
                        // на каких-то выделенных углах.
                        val offset = random.nextDouble() * angularStep
                        for (i in 0 until detectorsPerCell) {
                            add(
                                DetectorSpec(
                                    cx = cx,
                                    cy = cy,
                                    theta = offset + i * angularStep,
                                ),
                            )
                        }
                    }
                }
            }

            val config = ImageEncoderConfig(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                radius = radius,
                stride = stride,
                detectorsPerCell = detectorsPerCell,
                threshold = threshold,
                detectors = specs,
            )

            saveConfig(config, configPath)
            return fromConfig(config)
        }

        /**
         * Загружает ранее созданную сетчатку из JSON-файла по пути [configPath].
         */
        fun load(configPath: String): ImageEncoder {
            val config = json.decodeFromString<ImageEncoderConfig>(File(configPath).readText())
            return fromConfig(config)
        }

        private fun fromConfig(config: ImageEncoderConfig): ImageEncoder {
            val detectors = config.detectors.map { spec ->
                Detector(
                    cx = spec.cx,
                    cy = spec.cy,
                    theta = spec.theta,
                    radius = config.radius,
                    threshold = config.threshold,
                )
            }
            return ImageEncoder(config = config, detectors = detectors)
        }

        private fun saveConfig(config: ImageEncoderConfig, configPath: String) {
            val file = File(configPath)
            file.writeText(json.encodeToString(config))
            println("Config saved to: ${file.absolutePath}")
        }
    }
}
