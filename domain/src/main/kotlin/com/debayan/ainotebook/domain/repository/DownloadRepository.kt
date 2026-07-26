package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import kotlinx.coroutines.flow.Flow

/**
 * Manages background model downloads. Downloads run in WorkManager so they survive process death and
 * resume from a partial file. "Cancel" retains the partial file, so starting the same model again
 * resumes rather than restarts.
 */
interface DownloadRepository {

    fun observeDownload(modelId: String): Flow<ModelDownloadProgress?>

    fun observeActiveDownloads(): Flow<List<ModelDownloadProgress>>

    suspend fun startDownload(model: RemoteModel, allowMeteredNetwork: Boolean): AppResult<Unit>

    suspend fun cancelDownload(modelId: String): AppResult<Unit>
}
