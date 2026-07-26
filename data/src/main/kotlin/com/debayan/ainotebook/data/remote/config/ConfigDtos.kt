package com.debayan.ainotebook.data.remote.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the remote configuration files. Parsed leniently (unknown keys ignored) so the
 * hosted config can add fields without breaking older app versions. Field shapes should track the
 * actual config at the configured base URL.
 */

@Serializable
data class RemoteConfigDto(
    @SerialName("minAppVersionCode") val minAppVersionCode: Int = 0,
    @SerialName("latestAppVersionCode") val latestAppVersionCode: Int = 0,
    @SerialName("recommendedModelId") val recommendedModelId: String? = null,
    @SerialName("configVersion") val configVersion: Int = 1,
)

@Serializable
data class RemoteModelDto(
    val id: String,
    val name: String,
    val version: String = "1.0",
    val provider: String = "Hugging Face",
    val tier: String = "BALANCED",
    val quantization: String = "Q4_K_M",
    val fileName: String,
    val sizeBytes: Long = 0L,
    val sha256: String = "",
    val downloadUrl: String,
    val minRamMb: Int = 0,
    val recommendedRamMb: Int = 0,
    val minSdk: Int = 26,
    val supportedAbis: List<String> = emptyList(),
    val description: String = "",
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO",
    val publishedAt: Long = 0L,
)

@Serializable
data class ChangelogEntryDto(
    val versionName: String,
    val versionCode: Int = 0,
    val notes: List<String> = emptyList(),
    val releasedAt: Long = 0L,
)
