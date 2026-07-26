package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.ModelEntity
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.model.ai.ModelTier

fun ModelEntity.toDomain(): InstalledModel = InstalledModel(
    id = modelId,
    name = displayName,
    version = version,
    provider = provider,
    tier = tier.toModelTier(),
    fileName = fileName,
    localPath = localPath,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    downloadUrl = downloadUrl,
    minRamMb = minRamMb,
    recommendedRamMb = recommendedRamMb,
    installedAt = installedAt,
    lastUsedAt = lastUsedAt,
    isActive = isActive,
)

fun InstalledModel.toEntity(): ModelEntity = ModelEntity(
    modelId = id,
    displayName = name,
    version = version,
    provider = provider,
    tier = tier.name,
    fileName = fileName,
    localPath = localPath,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    downloadUrl = downloadUrl,
    minRamMb = minRamMb,
    recommendedRamMb = recommendedRamMb,
    installedAt = installedAt,
    lastUsedAt = lastUsedAt,
    isActive = isActive,
)

private fun String.toModelTier(): ModelTier =
    runCatching { ModelTier.valueOf(this) }.getOrDefault(ModelTier.BALANCED)
