package com.debayan.ainotebook.core

/**
 * Application-wide constants shared across modules.
 *
 * Values here must stay framework-agnostic (no Android references) so the pure Kotlin modules
 * can use them. Anything device-specific belongs in the data or app layer.
 */
object AppConstants {

    /** Room database file name. */
    const val DATABASE_NAME: String = "ai_notebook.db"

    /** DataStore preferences file name. */
    const val PREFERENCES_NAME: String = "ai_notebook_preferences"

    /** Remote configuration root (GitHub Pages), per the product spec. Read-only, HTTPS. */
    const val CONFIG_BASE_URL: String = "https://debayanpratihar.github.io/ai-notebook-config/"

    /** Internal storage sub-directory names (created lazily by the storage layer). */
    object Directories {
        const val NOTEBOOKS: String = "notebooks"
        const val EXPORTS: String = "exports"
        const val IMPORTS: String = "imports"
        const val CACHE: String = "cache"
        const val MODELS: String = "models"
        const val THUMBNAILS: String = "thumbnails"
        const val BACKUPS: String = "backups"
        const val LOGS: String = "logs"
    }
}
