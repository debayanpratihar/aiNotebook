package com.debayan.ainotebook.domain.model.ai

/** Progress snapshot for a model download, derived from the background worker's state. */
data class ModelDownloadProgress(
    val modelId: String,
    val state: ModelDownloadState,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percent: Int,
    val errorMessage: String?,
) {
    val isTerminal: Boolean
        get() = state == ModelDownloadState.INSTALLED ||
            state == ModelDownloadState.FAILED ||
            state == ModelDownloadState.CANCELLED
}
