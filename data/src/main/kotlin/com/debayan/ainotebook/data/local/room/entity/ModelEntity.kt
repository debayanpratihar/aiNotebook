package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registry row for a locally installed model. Added in schema v2. Exactly one row has
 * [isActive] = true at a time (enforced by the repository within a transaction).
 */
@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val modelId: String,
    val displayName: String,
    val version: String,
    val provider: String,
    val tier: String,
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
