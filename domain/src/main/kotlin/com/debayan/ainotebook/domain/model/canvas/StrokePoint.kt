package com.debayan.ainotebook.domain.model.canvas

/**
 * A single sampled point of a stroke in **world coordinates** (device-independent, never screen
 * space). Sequence is implied by position in [Stroke.points]. [pressure] is normalized 0..1
 * (1.0 when the device reports no pressure).
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = 0L,
)
