package com.debayan.ainotebook.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text recognition via Google ML Kit (Latin script). Recognition runs entirely on the
 * device — no image or text ever leaves it, per the privacy spec. The recognizer is reused across
 * calls (hence [Singleton]).
 */
@Singleton
class OcrService @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Recognizes text in [bitmap]; returns the concatenated recognized text (may be empty). */
    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result.text)
            }
            .addOnFailureListener { throwable ->
                if (continuation.isActive) continuation.resumeWithException(throwable)
            }
    }
}
