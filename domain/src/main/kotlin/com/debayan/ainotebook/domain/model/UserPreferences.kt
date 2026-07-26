package com.debayan.ainotebook.domain.model

/**
 * User-controlled application preferences persisted via DataStore.
 *
 * Phase 1 covers the appearance preferences needed to drive the Material 3 theme. Additional
 * settings sections (AI, canvas, handwriting, downloads, storage, accessibility) extend this
 * model in their respective phases.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
) {
    companion object {
        val DEFAULT = UserPreferences()
    }
}
