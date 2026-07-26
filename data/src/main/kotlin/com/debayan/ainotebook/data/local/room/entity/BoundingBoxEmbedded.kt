package com.debayan.ainotebook.data.local.room.entity

/**
 * Axis-aligned rectangle embedded into stroke and annotation rows so their spatial extent can be
 * queried/culled without loading every point. Coordinates are in canvas space.
 */
data class BoundingBoxEmbedded(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)
