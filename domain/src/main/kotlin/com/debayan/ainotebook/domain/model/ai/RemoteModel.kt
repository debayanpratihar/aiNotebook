package com.debayan.ainotebook.domain.model.ai

/**
 * A model advertised by the remote configuration (models.json). Describes a downloadable GGUF model
 * and the device requirements used to decide whether it can be installed.
 */
data class RemoteModel(
    val id: String,
    val name: String,
    val version: String,
    val provider: String,
    val tier: ModelTier,
    val quantization: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
    val minRamMb: Int,
    val recommendedRamMb: Int,
    val minSdk: Int,
    val supportedAbis: List<String>,
    val description: String,
)
