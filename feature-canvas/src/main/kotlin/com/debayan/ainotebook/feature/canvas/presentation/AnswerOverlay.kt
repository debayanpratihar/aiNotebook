package com.debayan.ainotebook.feature.canvas.presentation

/**
 * A clean AI answer rendered directly onto the canvas next to the user's handwriting, in world
 * coordinates so it pans and zooms with the notes. This is the "solved next to you" experience.
 */
data class AnswerOverlay(
    val text: String,
    val worldX: Float,
    val worldY: Float,
)
