package com.debayan.ainotebook.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.debayan.ainotebook.core.AppConstants
import com.debayan.ainotebook.core.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Periodic maintenance worker. In Phase 1 it guarantees the app's internal storage directories
 * exist (the storage spec requires the app to create missing directories automatically). Later
 * phases extend it with cache trimming and orphan pruning.
 *
 * Built by Hilt's [androidx.hilt.work.HiltWorkerFactory], which is installed by the app module's
 * WorkManager [androidx.work.Configuration].
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            ensureDirectoriesExist()
            Result.success()
        } catch (throwable: Throwable) {
            logger.e(TAG, "Maintenance pass failed", throwable)
            Result.retry()
        }
    }

    private fun ensureDirectoriesExist() {
        REQUIRED_DIRECTORIES.forEach { name ->
            val dir = File(appContext.filesDir, name)
            if (!dir.exists() && !dir.mkdirs()) {
                logger.w(TAG, "Could not create directory: $name")
            }
        }
    }

    companion object {
        const val TAG: String = "MaintenanceWorker"
        const val UNIQUE_NAME: String = "ai_notebook_maintenance"

        private val REQUIRED_DIRECTORIES = listOf(
            AppConstants.Directories.NOTEBOOKS,
            AppConstants.Directories.EXPORTS,
            AppConstants.Directories.IMPORTS,
            AppConstants.Directories.CACHE,
            AppConstants.Directories.MODELS,
            AppConstants.Directories.THUMBNAILS,
            AppConstants.Directories.BACKUPS,
            AppConstants.Directories.LOGS,
        )
    }
}
