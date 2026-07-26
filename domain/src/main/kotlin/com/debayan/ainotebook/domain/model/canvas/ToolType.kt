package com.debayan.ainotebook.domain.model.canvas

/**
 * Writing tools that produce a stored [Stroke]. Editing actions (erasers, lasso, selection, shapes)
 * operate on strokes rather than being persisted as a tool, so they are modeled by the drawing
 * engine's active-tool concept, not here.
 */
enum class ToolType {
    BALL_PEN,
    FOUNTAIN_PEN,
    PENCIL,
    MECHANICAL_PENCIL,
    MARKER,
    BRUSH,
    CALLIGRAPHY_PEN,
    HIGHLIGHTER,
}
