package org.example

import org.example.cognition.EncodedSample
import org.example.cognition.RecognitionTrainer
import org.example.cognition.TrainingConfig
import org.example.cognition.toBitSet
import org.example.perception.visual.data.ImageLoader
import org.example.perception.visual.domain.ImageEncoder
import java.io.File

fun main() {
    val encoder = ImageEncoder(maskRadius = 1, detectorActivationThreshold = 5.0, stride = 1)
    encoder.initialize()

    val syntheticDir = File("src/main/resources/synthetic")
    val pngs = syntheticDir
        .listFiles { file -> file.extension == "png" }
        ?.sortedBy { it.name }
        ?: emptyList()
    require(pngs.isNotEmpty()) { "Нет PNG в ${syntheticDir.absolutePath}" }

    val canonicalFile = pngs.firstOrNull { it.nameWithoutExtension.contains("canonical") }
        ?: error("Не найдена каноническая картинка (имя файла должно содержать 'canonical')")

    var sharedVectorSize = -1
    fun encode(file: File): EncodedSample {
        val vector = encoder.encode(ImageLoader.loadImageFromPng(file.path))
        if (sharedVectorSize < 0) sharedVectorSize = vector.size
        require(vector.size == sharedVectorSize) {
            "Разные размеры векторов: ${file.name} дал ${vector.size}, ожидалось $sharedVectorSize"
        }
        val code = vector.toBitSet()
        return EncodedSample(file.nameWithoutExtension, code, code.cardinality())
    }

    val canonical = encode(canonicalFile)
    val training = pngs.filter { it != canonicalFile }.map(::encode)

    RecognitionTrainer(
        config = TrainingConfig(),
        vectorSize = sharedVectorSize,
        canonical = canonical,
        training = training,
    ).train()
}
