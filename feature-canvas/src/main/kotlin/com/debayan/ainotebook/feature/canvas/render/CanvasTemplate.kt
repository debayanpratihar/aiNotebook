package com.debayan.ainotebook.feature.canvas.render

/**
 * Background template rendered independently of strokes (changing it never alters drawings). Phase 2
 * covers the common paper styles; additional templates (engineering, music staff, custom) are added
 * with the templates feature.
 */
enum class CanvasTemplate {
    BLANK,
    RULED,
    DOT,
    GRID,
}
