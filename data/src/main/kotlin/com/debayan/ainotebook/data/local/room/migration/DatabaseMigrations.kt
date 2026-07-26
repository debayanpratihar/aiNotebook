package com.debayan.ainotebook.data.local.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry of Room migrations. Every schema change adds an explicit [Migration] here —
 * destructive (drop-and-recreate) migrations are prohibited in production, so [ALL] is applied to
 * the builder and `fallbackToDestructiveMigration` is never used.
 */
object DatabaseMigrations {

    /** v1 → v2: adds the `models` registry table (see [com.debayan.ainotebook.data.local.room.entity.ModelEntity]). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `models` (
                    `modelId` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `version` TEXT NOT NULL,
                    `provider` TEXT NOT NULL,
                    `tier` TEXT NOT NULL,
                    `fileName` TEXT NOT NULL,
                    `localPath` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `sha256` TEXT NOT NULL,
                    `downloadUrl` TEXT NOT NULL,
                    `minRamMb` INTEGER NOT NULL,
                    `recommendedRamMb` INTEGER NOT NULL,
                    `installedAt` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER,
                    `isActive` INTEGER NOT NULL,
                    PRIMARY KEY(`modelId`)
                )
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
