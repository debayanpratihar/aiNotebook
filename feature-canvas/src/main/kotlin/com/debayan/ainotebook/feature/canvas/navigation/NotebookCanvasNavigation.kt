package com.debayan.ainotebook.feature.canvas.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.debayan.ainotebook.feature.canvas.presentation.NotebookCanvasViewModel
import com.debayan.ainotebook.feature.canvas.ui.NotebookCanvasScreen

/** Navigation contract owned by the canvas feature. */
object NotebookCanvasDestination {
    const val ARG_NOTEBOOK_ID = "notebookId"
    const val ROUTE = "notebook/{$ARG_NOTEBOOK_ID}"

    fun route(notebookId: String): String = "notebook/$notebookId"
}

/** Registers the notebook canvas destination in a [NavGraphBuilder]. */
fun NavGraphBuilder.notebookCanvasScreen(
    onBack: () -> Unit,
    onExport: (String) -> Unit,
) {
    composable(
        route = NotebookCanvasDestination.ROUTE,
        arguments = listOf(
            navArgument(NotebookCanvasDestination.ARG_NOTEBOOK_ID) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val notebookId = backStackEntry.arguments?.getString(NotebookCanvasDestination.ARG_NOTEBOOK_ID).orEmpty()
        val viewModel: NotebookCanvasViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        NotebookCanvasScreen(
            state = state,
            onBack = onBack,
            onSelectTool = viewModel::selectTool,
            onSelectEraser = viewModel::selectEraser,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onStrokeCompleted = viewModel::onStrokeCompleted,
            onEraseAt = viewModel::onEraseAt,
            onZoomChanged = viewModel::onZoomChanged,
            onToggleAiPanel = viewModel::toggleAiPanel,
            onGenerateAi = viewModel::generateAi,
            onStopAi = viewModel::stopAi,
            onExport = { onExport(notebookId) },
        )
    }
}
