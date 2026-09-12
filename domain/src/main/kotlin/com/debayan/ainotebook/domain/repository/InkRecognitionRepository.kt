package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.recognition.InkRecognitionResult
import com.debayan.ainotebook.domain.model.recognition.RecognitionHint
import com.debayan.ainotebook.domain.model.recognition.RecognitionModelState
import kotlinx.coroutines.flow.Flow

/**
 * Stroke-based handwriting recognition: the app's primary path from ink to text.
 *
 * This replaces rasterize-then-OCR, which cannot work for handwriting — printed-text OCR models are
 * trained on typeset glyphs and discard the stroke order, direction, and timing that make cursive
 * and math legible. Bitmap OCR is retained only for imported images of printed pages
 * ([OcrRepository]).
 */
interface InkRecognitionRepository {

    /** Availability of the recognizer for [languageTag]. */
    fun observeModelState(languageTag: String): Flow<RecognitionModelState>

    /** Downloads the recognition model if absent; a no-op once it is [RecognitionModelState.Ready]. */
    suspend fun ensureModel(languageTag: String): AppResult<Unit>

    /** Frees the downloaded recognition model for [languageTag]. */
    suspend fun deleteModel(languageTag: String): AppResult<Unit>

    /** Language tags with a model already on device. */
    suspend fun downloadedLanguages(): List<String>

    /**
     * Segments [strokes], recognizes each segment, and reranks the candidates against the math
     * grammar. Strokes may be in any order; segmentation establishes reading order itself.
     */
    suspend fun recognize(
        strokes: List<Stroke>,
        hint: RecognitionHint = RecognitionHint(),
    ): AppResult<InkRecognitionResult>
}
