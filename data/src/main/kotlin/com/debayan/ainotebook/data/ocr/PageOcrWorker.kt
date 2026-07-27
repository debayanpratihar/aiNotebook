package com.debayan.ainotebook.data.ocr

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.debayan.ainotebook.core.logging.Logger
import com.debayan.ainotebook.domain.repository.SearchRepository
import com.debayan.ainotebook.domain.repository.StrokeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Background OCR job for a single page: loads the page's strokes, rasterizes them, recognizes text
 * on-device, and updates the search index. Non-destructive — strokes are never modified. Runs off
 * the UI thread and is replaceable (a newer edit supersedes a pending run).
 */
@HiltWorker
class PageOcrWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val strokeRepository: StrokeRepository,
    private val ocrService: OcrService,
    private val searchRepository: SearchRepository,
    private val renderer: StrokeBitmapRenderer,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val notebookId = inputData.getString(KEY_NOTEBOOK_ID) ?: return Result.failure()
        val pageId = inputData.getString(KEY_PAGE_ID) ?: return Result.failure()

        return try {
            val strokes = strokeRepository.observeStrokes(pageId).first()
            val recognizedText = if (strokes.isEmpty()) {
                ""
            } else {
                val bitmap = renderer.render(strokes)
                if (bitmap == null) {
                    ""
                } else {
                    try {
                        ocrService.recognize(bitmap)
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            searchRepository.indexPage(notebookId, pageId, recognizedText)
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.e(TAG, "OCR indexing failed for page $pageId", throwable)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_NOTEBOOK_ID = "notebook_id"
        const val KEY_PAGE_ID = "page_id"

        private const val TAG = "PageOcrWorker"
        private const val MAX_ATTEMPTS = 3

        fun uniqueName(pageId: String): String = "ocr_$pageId"
    }
}
