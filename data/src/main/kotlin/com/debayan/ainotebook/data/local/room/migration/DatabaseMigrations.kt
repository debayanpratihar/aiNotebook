package com.debayan.ainotebook.data.local.room.migration

import androidx.room.migration.Migration

/**
 * Central registry of Room migrations.
 *
 * Empty at schema version 1. Every future schema change must add an explicit [Migration] here —
 * destructive (drop-and-recreate) migrations are prohibited in production per the database spec,
 * so [ALL] is applied to the builder and `fallbackToDestructiveMigration` is never used.
 */
object DatabaseMigrations {
    val ALL: Array<Migration> = emptyArray()
}
