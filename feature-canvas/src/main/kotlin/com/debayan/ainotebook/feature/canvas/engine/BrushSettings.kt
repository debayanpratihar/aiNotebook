package com.debayan.ainotebook.feature.canvas.engine

import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import com.debayan.ainotebook.domain.model.canvas.ToolType

/**
 * The currently selected brush configuration applied to new strokes. Widths are in world units so a
 * stroke keeps a consistent physical size at every zoom level.
 */
data class BrushSettings(
    val tool: ToolType = ToolType.BALL_PEN,
    val color: Long = DEFAULT_INK,
    val width: Float = 3f,
    val opacity: Float = 1f,
    val smoothing: SmoothingMode = SmoothingMode.MEDIUM,
    val pressureEnabled: Boolean = true,
) {
    /** Highlighter strokes render translucent with a multiply blend so text stays visible beneath. */
    val isHighlighter: Boolean get() = tool == ToolType.HIGHLIGHTER

    /** Effective opacity, capped for the highlighter regardless of the configured value. */
    val effectiveOpacity: Float get() = if (isHighlighter) minOf(opacity, HIGHLIGHTER_MAX_OPACITY) else opacity

    companion object {
        /** Near-black ink (ARGB). */
        const val DEFAULT_INK: Long = 0xFF1B1B1F
        const val HIGHLIGHTER_MAX_OPACITY: Float = 0.4f
    }
}
