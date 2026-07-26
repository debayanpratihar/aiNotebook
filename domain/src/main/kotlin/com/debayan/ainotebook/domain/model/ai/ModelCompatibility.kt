package com.debayan.ainotebook.domain.model.ai

/**
 * Result of checking a [RemoteModel] against the device. [isCompatible] is the gate for allowing a
 * download; [meetsRecommendedRam] distinguishes "will run" from "will run well".
 */
data class ModelCompatibility(
    val isCompatible: Boolean,
    val meetsMinRam: Boolean,
    val meetsRecommendedRam: Boolean,
    val hasEnoughStorage: Boolean,
    val abiSupported: Boolean,
    val sdkSupported: Boolean,
    val reason: String?,
)
