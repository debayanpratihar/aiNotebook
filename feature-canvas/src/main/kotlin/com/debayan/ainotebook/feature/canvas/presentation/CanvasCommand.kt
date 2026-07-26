package com.debayan.ainotebook.feature.canvas.presentation

import com.debayan.ainotebook.domain.model.canvas.Stroke

/**
 * A reversible canvas edit for the undo/redo history. Because strokes are persisted, undoing/redoing
 * simply re-applies the inverse operation against the repository, and the observed stroke flow
 * refreshes the canvas.
 */
sealed interface CanvasCommand {
    data class AddStroke(val stroke: Stroke) : CanvasCommand
    data class RemoveStroke(val stroke: Stroke) : CanvasCommand
}
