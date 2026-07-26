package com.debayan.ainotebook.domain.model.ai

/** Snapshot of the device resources relevant to running a local model. */
data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val freeStorageBytes: Long,
    val supportedAbis: List<String>,
    val sdkInt: Int,
    val cpuCores: Int,
)
