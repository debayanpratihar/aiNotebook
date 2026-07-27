package com.debayan.ainotebook.feature.search

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Navigation contract owned by the search feature. */
object SearchDestination {
    const val ROUTE = "search"
}

fun NavGraphBuilder.searchScreen(
    onBack: () -> Unit,
    onOpenNotebook: (String) -> Unit,
) {
    composable(SearchDestination.ROUTE) {
        val viewModel: SearchViewModel = hiltViewModel()
        val query by viewModel.queryText.collectAsStateWithLifecycle()
        val results by viewModel.results.collectAsStateWithLifecycle()
        SearchScreen(
            query = query,
            results = results,
            onQueryChange = viewModel::onQueryChange,
            onOpenNotebook = onOpenNotebook,
            onBack = onBack,
        )
    }
}
