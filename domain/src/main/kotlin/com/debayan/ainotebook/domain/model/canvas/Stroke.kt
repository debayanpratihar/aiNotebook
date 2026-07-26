package com.debayan.ainotebook.domain.model.canvas

/**
 * A single editable vector stroke. The same model is produced by user input and by AI handwriting
 * (the AI writes through the identical engine), so both render and edit identically.
 *
 * [color] is packed ARGB stored as [Long]. [width] and [opacity] are the base brush values; per-point
 * pressure modulates the rendered width. Coordinates in [points] and [boundingBox] are world-space.
 */
data class Stroke(
    val id: String,
    val layerId: String,
    val tool: ToolType,
    val color: Long,
    val width: Float,
    val opacity: Float,
    val points: List<StrokePoint>,
    val boundingBox: BoundingBox,
    val createdAt: Long,
    val isAiGenerated: Boolean = false,
)
