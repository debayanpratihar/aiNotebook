package com.debayan.ainotebook.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.download.DownloadWorkKeys
import com.debayan.ainotebook.data.download.ModelDownloadWorker
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.model.ai.ModelDownloadState
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** [DownloadRepository] over WorkManager. Each model downloads under a unique work name. */
class DownloadRepositoryImpl @Inject constructor(
    private val workManager: WorkManager,
) : DownloadRepository {

    override fun observeDownload(modelId: String): Flow<ModelDownloadProgress?> =
        workManager.getWorkInfosForUniqueWorkFlow(DownloadWorkKeys.uniqueName(modelId))
            .map { infos -> infos.firstOrNull()?.toProgress(modelId) }

    override fun observeActiveDownloads(): Flow<List<ModelDownloadProgress>> =
        workManager.getWorkInfosByTagFlow(DownloadWorkKeys.TAG)
            .map { infos -> infos.mapNotNull { info -> info.extractModelId()?.let { id -> info.toProgress(id) } } }

    override suspend fun startDownload(model: RemoteModel, allowMeteredNetwork: Boolean): AppResult<Unit> {
        val data = workDataOf(
            DownloadWorkKeys.KEY_MODEL_ID to model.id,
            DownloadWorkKeys.KEY_URL to model.downloadUrl,
            DownloadWorkKeys.KEY_FILE_NAME to model.fileName,
            DownloadWorkKeys.KEY_SHA256 to model.sha256,
            DownloadWorkKeys.KEY_SIZE to model.sizeBytes,
            DownloadWorkKeys.KEY_NAME to model.name,
            DownloadWorkKeys.KEY_VERSION to model.version,
            DownloadWorkKeys.KEY_PROVIDER to model.provider,
            DownloadWorkKeys.KEY_TIER to model.tier.name,
            DownloadWorkKeys.KEY_MIN_RAM to model.minRamMb,
            DownloadWorkKeys.KEY_REC_RAM to model.recommendedRamMb,
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (allowMeteredNetwork) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(DownloadWorkKeys.TAG)
            .addTag(DownloadWorkKeys.modelTag(model.id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        // KEEP: if a download for this model is already running/queued, don't start a second one.
        workManager.enqueueUniqueWork(DownloadWorkKeys.uniqueName(model.id), ExistingWorkPolicy.KEEP, request)
        return AppResult.Success(Unit)
    }

    override suspend fun cancelDownload(modelId: String): AppResult<Unit> {
        workManager.cancelUniqueWork(DownloadWorkKeys.uniqueName(modelId))
        return AppResult.Success(Unit)
    }

    private fun WorkInfo.extractModelId(): String? =
        tags.firstOrNull { it.startsWith(DownloadWorkKeys.MODEL_TAG_PREFIX) }
            ?.removePrefix(DownloadWorkKeys.MODEL_TAG_PREFIX)

    private fun WorkInfo.toProgress(modelId: String): ModelDownloadProgress {
        val data = if (state == WorkInfo.State.SUCCEEDED) outputData else progress
        val running = data.getString(DownloadWorkKeys.KEY_STATE)
            ?.let { runCatching { ModelDownloadState.valueOf(it) }.getOrNull() }
        val mapped = when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ModelDownloadState.QUEUED
            WorkInfo.State.RUNNING -> running ?: ModelDownloadState.DOWNLOADING
            WorkInfo.State.SUCCEEDED -> ModelDownloadState.INSTALLED
            WorkInfo.State.FAILED -> ModelDownloadState.FAILED
            WorkInfo.State.CANCELLED -> ModelDownloadState.CANCELLED
        }
        return ModelDownloadProgress(
            modelId = modelId,
            state = mapped,
            downloadedBytes = data.getLong(DownloadWorkKeys.KEY_DOWNLOADED, 0L),
            totalBytes = data.getLong(DownloadWorkKeys.KEY_TOTAL, 0L),
            percent = data.getInt(
                DownloadWorkKeys.KEY_PROGRESS,
                if (mapped == ModelDownloadState.INSTALLED) 100 else 0,
            ),
            errorMessage = outputData.getString(DownloadWorkKeys.KEY_ERROR),
        )
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
