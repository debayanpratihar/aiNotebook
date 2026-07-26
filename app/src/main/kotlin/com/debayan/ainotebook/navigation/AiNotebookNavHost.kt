package com.debayan.ainotebook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.debayan.ainotebook.feature.canvas.navigation.NotebookCanvasDestination
import com.debayan.ainotebook.feature.canvas.navigation.notebookCanvasScreen
import com.debayan.ainotebook.home.HomeRoute

/**
 * Root navigation graph. Home and the notebook canvas are live; the remaining destinations are
 * placeholders that their feature modules replace in later phases. The notebook destination is
 * contributed by the canvas feature via [notebookCanvasScreen].
 */
@Composable
fun AiNotebookNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onOpenNotebook = { id -> navController.navigate(NotebookCanvasDestination.route(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        notebookCanvasScreen(onBack = { navController.popBackStack() })

        composable(Routes.SEARCH) {
            PlaceholderScreen(title = "Search", onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(title = "Settings", onBack = { navController.popBackStack() })
        }
        composable(Routes.MODEL_MANAGER) {
            PlaceholderScreen(title = "Model Manager", onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPORT) {
            PlaceholderScreen(title = "Export", onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            PlaceholderScreen(title = "About", onBack = { navController.popBackStack() })
        }
    }
}
