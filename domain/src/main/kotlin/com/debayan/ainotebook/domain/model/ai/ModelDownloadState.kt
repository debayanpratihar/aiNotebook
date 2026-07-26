package com.debayan.ainotebook.domain.model.ai

/** Lifecycle state of a model download. */
enum class ModelDownloadState {
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    INSTALLED,
    FAILED,
    CANCELLED,
}
