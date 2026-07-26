package com.debayan.ainotebook.navigation

/**
 * Central registry of navigation routes.
 *
 * These mirror the primary destinations in the architecture spec (Home, Notebook, Search, Settings,
 * Model Manager, Export, About). Feature modules replace the placeholder screens wired to these
 * routes as they are implemented in later phases.
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val MODEL_MANAGER = "model_manager"
    const val EXPORT = "export"
    const val ABOUT = "about"

    const val NOTEBOOK_ARG_ID = "notebookId"
    const val NOTEBOOK = "notebook/{$NOTEBOOK_ARG_ID}"

    /** Builds a concrete notebook route for a given id. */
    fun notebook(notebookId: String): String = "notebook/$notebookId"
}
