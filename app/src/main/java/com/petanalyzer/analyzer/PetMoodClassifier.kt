package com.petanalyzer.analyzer

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class PetMoodClassifier(context: Context) {

    private val classifier: ImageClassifier

    data class MoodResult(
        val topLabel: String,
        val topScore: Float,
        val isPetTypical: Boolean,
    )

    companion object {
        private const val PET_LIKE_THRESHOLD = 0.25f
    }

    init {
        classifier = ImageClassifier.createFromFile(context, "mobilenet_v1_1.0_224_quant.tflite")
    }

    fun classify(bitmap: Bitmap): MoodResult {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val tensorImage = TensorImage.fromBitmap(resized)
        val classifications = classifier.classify(tensorImage)

        val categories = classifications.firstOrNull()?.categories ?: emptyList()
        val top = categories.firstOrNull()
        val topLabel = top?.label?.lowercase() ?: ""
        val topScore = top?.score ?: 0f

        val isPetLike = categories.any { c ->
            val label = c.label?.lowercase() ?: ""
            val score = c.score
            score >= PET_LIKE_THRESHOLD && (label.contains("dog") || label.contains("cat")
                    || label.contains("puppy") || label.contains("kitten"))
        }

        return MoodResult(
            topLabel = topLabel,
            topScore = topScore,
            isPetTypical = isPetLike,
        )
    }

    fun close() {
        classifier.close()
    }
}
