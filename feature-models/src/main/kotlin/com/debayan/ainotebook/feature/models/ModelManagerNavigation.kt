package com.debayan.ainotebook.feature.models

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Navigation contract owned by the model-manager feature. */
object ModelManagerDestination {
    const val ROUTE = "model_manager"
}

fun NavGraphBuilder.modelManagerScreen(onBack: () -> Unit) {
    composable(ModelManagerDestination.ROUTE) {
        val viewModel: ModelManagerViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ModelManagerScreen(
            state = state,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onDownload = viewModel::onDownload,
            onCancelDownload = viewModel::onCancelDownload,
            onActivate = viewModel::onActivate,
            onDelete = viewModel::onDelete,
        )
    }
}
