package com.debayan.ainotebook.data.remote.config

import kotlinx.serialization.Serializable

/**
 * Wire models for the remote configuration files. Field names/shapes mirror the hosted config at the
 * configured base URL. Parsed leniently (unknown keys ignored) so the config can add fields without
 * breaking older app versions.
 */

@Serializable
data class RemoteConfigDto(
    val configVersion: Int = 1,
    val maintenance: Boolean = false,
    val minimumAppVersion: Int = 0,
    val latestAppVersion: String = "",
    val defaultModel: String? = null,
    val wifiOnlyDefault: Boolean = true,
)

/** `models.json` is an object with a `models` array. */
@Serializable
data class ModelsResponseDto(
    val models: List<RemoteModelDto> = emptyList(),
)

@Serializable
data class RemoteModelDto(
    val id: String,
    val name: String = "",
    val description: String = "",
    val recommended: Boolean = false,
    val minRamGB: Int = 0,
    val recommendedStorageGB: Int = 0,
    val downloadSizeMB: Long = 0L,
    val quantization: String = "Q4_K_M",
    val provider: String = "",
    val repo: String = "",
    val filename: String = "",
    val downloadUrl: String = "",
    val version: Int = 1,
    val sha256: String = "",
)

/** `announcements.json` is a single announcement object. */
@Serializable
data class AnnouncementDto(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO",
    val publishedAt: Long = 0L,
)

/** `changelog.json` is `{ latestVersion, changes: [...] }`. */
@Serializable
data class ChangelogResponseDto(
    val latestVersion: String = "",
    val changes: List<String> = emptyList(),
)
