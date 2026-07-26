package com.debayan.ainotebook.domain.model.ai

/**
 * A model that has been downloaded, verified, and installed locally. Persisted in the model
 * registry. Exactly one installed model is active at a time.
 */
data class InstalledModel(
    val id: String,
    val name: String,
    val version: String,
    val provider: String,
    val tier: ModelTier,
    val fileName: String,
    val localPath: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
    val minRamMb: Int,
    val recommendedRamMb: Int,
    val installedAt: Long,
    val lastUsedAt: Long?,
    val isActive: Boolean,
)
