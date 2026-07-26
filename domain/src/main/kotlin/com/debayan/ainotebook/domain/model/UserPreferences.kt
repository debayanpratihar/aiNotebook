package com.debayan.ainotebook.domain.model

import com.debayan.ainotebook.domain.model.canvas.SmoothingMode

/**
 * User-controlled application preferences persisted via DataStore. Covers the settings that drive
 * runtime behavior across the app; additional cosmetic/diagnostic settings extend this model as
 * their features land.
 */
data class UserPreferences(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    // AI
    val aiEnabled: Boolean = true,
    val automaticAiGeneration: Boolean = false,
    val aiInactivityTimeoutSeconds: Int = 3,
    val streamAiResponses: Boolean = true,
    // Drawing / Canvas
    val defaultSmoothing: SmoothingMode = SmoothingMode.MEDIUM,
    val pressureSensitivityEnabled: Boolean = true,
    val defaultPenWidth: Float = 3f,
    // OCR / Search
    val ocrEnabled: Boolean = true,
    val automaticIndexing: Boolean = true,
    // Downloads
    val wifiOnlyDownloads: Boolean = true,
) {
    companion object {
        val DEFAULT = UserPreferences()
    }
}
