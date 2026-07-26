package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding internal database bookkeeping. [id] is pinned to [SINGLETON_ID] so
 * there is always exactly one row.
 */
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val schemaVersion: Int,
    val appVersion: String,
    val lastMigration: Long? = null,
    val databaseCreated: Long,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
