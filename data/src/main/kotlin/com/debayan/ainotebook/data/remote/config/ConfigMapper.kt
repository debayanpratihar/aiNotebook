package com.debayan.ainotebook.data.remote.config

import com.debayan.ainotebook.domain.model.ai.Announcement
import com.debayan.ainotebook.domain.model.ai.ChangelogEntry
import com.debayan.ainotebook.domain.model.ai.ModelTier
import com.debayan.ainotebook.domain.model.ai.RemoteConfig
import com.debayan.ainotebook.domain.model.ai.RemoteModel

fun RemoteConfigDto.toDomain(): RemoteConfig = RemoteConfig(
    minAppVersionCode = minAppVersionCode,
    latestAppVersionCode = latestAppVersionCode,
    recommendedModelId = recommendedModelId,
    configVersion = configVersion,
)

fun RemoteModelDto.toDomain(): RemoteModel = RemoteModel(
    id = id,
    name = name,
    version = version,
    provider = provider,
    tier = tier.toModelTier(),
    quantization = quantization,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    downloadUrl = downloadUrl,
    minRamMb = minRamMb,
    recommendedRamMb = recommendedRamMb,
    minSdk = minSdk,
    supportedAbis = supportedAbis,
    description = description,
)

fun AnnouncementDto.toDomain(): Announcement = Announcement(
    id = id,
    title = title,
    message = message,
    severity = severity,
    publishedAt = publishedAt,
)

fun ChangelogEntryDto.toDomain(): ChangelogEntry = ChangelogEntry(
    versionName = versionName,
    versionCode = versionCode,
    notes = notes,
    releasedAt = releasedAt,
)

private fun String.toModelTier(): ModelTier = when (uppercase().replace('-', '_')) {
    "COMPACT" -> ModelTier.COMPACT
    "HIGH_QUALITY", "HIGHQUALITY" -> ModelTier.HIGH_QUALITY
    else -> ModelTier.BALANCED
}
