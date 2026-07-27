package com.debayan.ainotebook.data.remote.config

import com.debayan.ainotebook.domain.model.ai.Announcement
import com.debayan.ainotebook.domain.model.ai.ChangelogEntry
import com.debayan.ainotebook.domain.model.ai.ModelTier
import com.debayan.ainotebook.domain.model.ai.RemoteConfig
import com.debayan.ainotebook.domain.model.ai.RemoteModel

private const val BYTES_PER_MB = 1024L * 1024L
private const val MB_PER_GB = 1024

fun RemoteConfigDto.toDomain(): RemoteConfig = RemoteConfig(
    minAppVersionCode = minimumAppVersion,
    latestAppVersionCode = 0,
    recommendedModelId = defaultModel,
    configVersion = configVersion,
)

fun RemoteModelDto.toDomain(): RemoteModel = RemoteModel(
    id = id,
    name = name.ifBlank { id },
    version = version.toString(),
    provider = provider,
    tier = tierFromId(id),
    quantization = quantization,
    fileName = filename,
    sizeBytes = downloadSizeMB * BYTES_PER_MB,
    sha256 = sha256,
    downloadUrl = downloadUrl,
    // The config expresses RAM in GB and has no separate "recommended RAM", so minimum doubles as
    // the recommended figure for the recommendation heuristic.
    minRamMb = minRamGB * MB_PER_GB,
    recommendedRamMb = minRamGB * MB_PER_GB,
    minSdk = DEFAULT_MIN_SDK,
    supportedAbis = emptyList(),
    description = description,
)

fun AnnouncementDto.toDomain(): Announcement = Announcement(
    id = id.ifBlank { title },
    title = title,
    message = message,
    severity = severity,
    publishedAt = publishedAt,
)

fun ChangelogResponseDto.toEntries(): List<ChangelogEntry> =
    if (latestVersion.isBlank() && changes.isEmpty()) {
        emptyList()
    } else {
        listOf(ChangelogEntry(versionName = latestVersion, versionCode = 0, notes = changes, releasedAt = 0L))
    }

private fun tierFromId(id: String): ModelTier = when (id.lowercase()) {
    "compact" -> ModelTier.COMPACT
    "high", "high_quality" -> ModelTier.HIGH_QUALITY
    else -> ModelTier.BALANCED
}

private const val DEFAULT_MIN_SDK = 26
