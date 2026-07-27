package com.debayan.ainotebook.feature.export

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Navigation contract owned by the export feature. */
object ExportDestination {
    const val ARG_NOTEBOOK_ID = "notebookId"
    const val ROUTE = "export/{$ARG_NOTEBOOK_ID}"

    fun route(notebookId: String): String = "export/$notebookId"
}

fun NavGraphBuilder.exportScreen(onBack: () -> Unit) {
    composable(
        route = ExportDestination.ROUTE,
        arguments = listOf(navArgument(ExportDestination.ARG_NOTEBOOK_ID) { type = NavType.StringType }),
    ) {
        val viewModel: ExportViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ExportScreen(state = state, onExport = viewModel::export, onBack = onBack)
    }
}
