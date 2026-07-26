package com.debayan.ainotebook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Root navigation graph. Wires every primary destination from the spec to a placeholder screen for
 * Phase 1. The Home destination demonstrates real navigation to the other destinations; feature
 * modules will swap in their screens without changing this graph's shape.
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
            PlaceholderScreen(
                title = "Home",
                actions = listOf(
                    PlaceholderAction("Open sample notebook") {
                        navController.navigate(Routes.notebook("sample"))
                    },
                    PlaceholderAction("Search") { navController.navigate(Routes.SEARCH) },
                    PlaceholderAction("Model Manager") { navController.navigate(Routes.MODEL_MANAGER) },
                    PlaceholderAction("Settings") { navController.navigate(Routes.SETTINGS) },
                    PlaceholderAction("Export") { navController.navigate(Routes.EXPORT) },
                    PlaceholderAction("About") { navController.navigate(Routes.ABOUT) },
                ),
            )
        }

        composable(
            route = Routes.NOTEBOOK,
            arguments = listOf(navArgument(Routes.NOTEBOOK_ARG_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getString(Routes.NOTEBOOK_ARG_ID).orEmpty()
            PlaceholderScreen(
                title = "Notebook ($notebookId)",
                onBack = { navController.popBackStack() },
            )
        }

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
