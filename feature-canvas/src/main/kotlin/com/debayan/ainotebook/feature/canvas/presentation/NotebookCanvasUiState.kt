package com.debayan.ainotebook.feature.canvas.presentation

import com.debayan.ainotebook.domain.model.ai.AiGenerationState
import com.debayan.ainotebook.domain.model.canvas.Page
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.feature.canvas.engine.BrushSettings
import com.debayan.ainotebook.feature.canvas.engine.CanvasToolMode

/** Immutable UI state for the notebook canvas screen. */
data class NotebookCanvasUiState(
    val isLoading: Boolean = true,
    val notebookTitle: String = "Notebook",
    val page: Page? = null,
    val strokes: List<Stroke> = emptyList(),
    val toolMode: CanvasToolMode = CanvasToolMode.DRAW,
    val brush: BrushSettings = BrushSettings(),
    val zoomPercent: Int = 100,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val aiPanelVisible: Boolean = false,
    val aiState: AiGenerationState = AiGenerationState.Idle,
    val answerOverlay: AnswerOverlay? = null,
    val errorMessage: String? = null,
)
