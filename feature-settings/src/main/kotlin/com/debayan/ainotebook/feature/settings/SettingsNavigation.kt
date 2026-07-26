package com.debayan.ainotebook.feature.settings

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Navigation contract owned by the settings feature. */
object SettingsDestination {
    const val ROUTE = "settings"
}

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable(SettingsDestination.ROUTE) {
        val viewModel: SettingsViewModel = hiltViewModel()
        val prefs by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsScreen(prefs = prefs, onEvent = viewModel::onEvent, onBack = onBack)
    }
}
