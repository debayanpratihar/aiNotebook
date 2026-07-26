package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single sampled point of a stroke.
 *
 * Uses an auto-generated [Long] primary key (not a UUID string): a notebook can contain millions
 * of points, and a compact integer key keeps the index small and inserts fast, matching the
 * schema's "millions of stroke points" performance target.
 */
@Entity(
    tableName = "stroke_points",
    foreignKeys = [
        ForeignKey(
            entity = StrokeEntity::class,
            parentColumns = ["strokeId"],
            childColumns = ["strokeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("strokeId"),
        Index(value = ["strokeId", "sequenceNumber"]),
    ],
)
data class StrokePointEntity(
    @PrimaryKey(autoGenerate = true) val pointId: Long = 0L,
    val strokeId: String,
    val sequenceNumber: Int,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long,
)
