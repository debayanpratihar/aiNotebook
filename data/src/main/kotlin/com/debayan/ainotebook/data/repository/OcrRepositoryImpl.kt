package com.debayan.ainotebook.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.ocr.PageOcrWorker
import com.debayan.ainotebook.domain.repository.OcrRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * [OcrRepository] over WorkManager. Uses a per-page unique job with REPLACE plus a short initial
 * delay, so rapid edits coalesce into a single OCR run shortly after the user stops drawing.
 */
class OcrRepositoryImpl @Inject constructor(
    private val workManager: WorkManager,
) : OcrRepository {

    override suspend fun requestPageIndexing(notebookId: String, pageId: String): AppResult<Unit> {
        val data = workDataOf(
            PageOcrWorker.KEY_NOTEBOOK_ID to notebookId,
            PageOcrWorker.KEY_PAGE_ID to pageId,
        )
        val request = OneTimeWorkRequestBuilder<PageOcrWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            PageOcrWorker.uniqueName(pageId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return AppResult.Success(Unit)
    }

    private companion object {
        const val DEBOUNCE_SECONDS = 2L
    }
}
