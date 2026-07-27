package com.debayan.ainotebook.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.debayan.ainotebook.core.AppConstants
import com.debayan.ainotebook.core.logging.Logger
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.model.ai.ModelDownloadState
import com.debayan.ainotebook.domain.model.ai.ModelTier
import com.debayan.ainotebook.domain.repository.ModelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * Background worker that downloads, verifies, and registers a model. Runs as a foreground (data-sync)
 * service so large downloads survive backgrounding, reports progress via [setProgress], and resumes
 * from a partial file on retry. A model failing SHA-256 verification is deleted and never registered.
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloader: ModelDownloader,
    private val modelRepository: ModelRepository,
    private val notifications: DownloadNotifications,
    private val timeProvider: TimeProvider,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(DownloadWorkKeys.KEY_MODEL_ID) ?: return Result.failure()
        val url = inputData.getString(DownloadWorkKeys.KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(DownloadWorkKeys.KEY_FILE_NAME) ?: return Result.failure()
        val displayName = inputData.getString(DownloadWorkKeys.KEY_NAME) ?: fileName
        val sha256 = inputData.getString(DownloadWorkKeys.KEY_SHA256).orEmpty()
        val declaredSize = inputData.getLong(DownloadWorkKeys.KEY_SIZE, 0L)

        val destination = File(File(appContext.filesDir, AppConstants.Directories.MODELS), fileName)

        return try {
            setForeground(foregroundInfo(modelId, displayName, 0, indeterminate = false, verifying = false))
            setProgress(progressData(ModelDownloadState.DOWNLOADING, 0, 0L, declaredSize))

            val file = downloader.download(url, destination, declaredSize) { downloaded, total ->
                val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                setProgress(progressData(ModelDownloadState.DOWNLOADING, percent, downloaded, total))
                notifications.notify(
                    modelId,
                    notifications.buildNotification(displayName, percent, indeterminate = false, verifying = false),
                )
            }

            setProgress(progressData(ModelDownloadState.VERIFYING, 100, declaredSize, declaredSize))
            notifications.notify(
                modelId,
                notifications.buildNotification(displayName, 100, indeterminate = true, verifying = true),
            )

            if (!downloader.verify(file, sha256)) {
                file.delete()
                logger.w(TAG, "SHA-256 verification failed for model $modelId")
                return Result.failure(errorData("Downloaded file failed integrity verification"))
            }

            modelRepository.registerInstalledModel(buildInstalledModel(modelId, url, fileName, displayName, sha256, declaredSize, file))
            Result.success(
                workDataOf(
                    DownloadWorkKeys.KEY_STATE to ModelDownloadState.INSTALLED.name,
                    DownloadWorkKeys.KEY_PROGRESS to 100,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.e(TAG, "Model download failed for $modelId", throwable)
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(errorData(throwable.message ?: "Download failed"))
            }
        }
    }

    private fun buildInstalledModel(
        modelId: String,
        url: String,
        fileName: String,
        displayName: String,
        sha256: String,
        declaredSize: Long,
        file: File,
    ): InstalledModel {
        val tier = runCatching {
            ModelTier.valueOf(inputData.getString(DownloadWorkKeys.KEY_TIER).orEmpty())
        }.getOrDefault(ModelTier.BALANCED)
        return InstalledModel(
            id = modelId,
            name = displayName,
            version = inputData.getString(DownloadWorkKeys.KEY_VERSION).orEmpty(),
            provider = inputData.getString(DownloadWorkKeys.KEY_PROVIDER).orEmpty(),
            tier = tier,
            fileName = fileName,
            localPath = file.absolutePath,
            sizeBytes = if (declaredSize > 0) declaredSize else file.length(),
            sha256 = sha256,
            downloadUrl = url,
            minRamMb = inputData.getInt(DownloadWorkKeys.KEY_MIN_RAM, 0),
            recommendedRamMb = inputData.getInt(DownloadWorkKeys.KEY_REC_RAM, 0),
            installedAt = timeProvider.now(),
            lastUsedAt = null,
            isActive = false,
        )
    }

    private fun foregroundInfo(
        modelId: String,
        displayName: String,
        percent: Int,
        indeterminate: Boolean,
        verifying: Boolean,
    ): ForegroundInfo {
        val notification = notifications.buildNotification(displayName, percent, indeterminate, verifying)
        val id = notifications.notificationId(modelId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun progressData(state: ModelDownloadState, percent: Int, downloaded: Long, total: Long) =
        workDataOf(
            DownloadWorkKeys.KEY_STATE to state.name,
            DownloadWorkKeys.KEY_PROGRESS to percent,
            DownloadWorkKeys.KEY_DOWNLOADED to downloaded,
            DownloadWorkKeys.KEY_TOTAL to total,
        )

    private fun errorData(message: String) =
        workDataOf(
            DownloadWorkKeys.KEY_STATE to ModelDownloadState.FAILED.name,
            DownloadWorkKeys.KEY_ERROR to message,
        )

    private companion object {
        const val TAG = "ModelDownloadWorker"
        const val MAX_ATTEMPTS = 3
    }
}
