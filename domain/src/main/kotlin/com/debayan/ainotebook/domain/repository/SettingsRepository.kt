package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import kotlinx.coroutines.flow.Flow

/** Repository contract for user preferences. Backed by DataStore in the data layer. */
interface SettingsRepository {

    /** Emits the current preferences and every subsequent change (single source of truth). */
    val userPreferences: Flow<UserPreferences>

    // Appearance
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)

    // AI
    suspend fun setAiEnabled(enabled: Boolean)
    suspend fun setAutomaticAiGeneration(enabled: Boolean)
    suspend fun setAiInactivityTimeout(seconds: Int)
    suspend fun setStreamAiResponses(enabled: Boolean)

    // Drawing / Canvas
    suspend fun setDefaultSmoothing(mode: SmoothingMode)
    suspend fun setPressureSensitivity(enabled: Boolean)
    suspend fun setDefaultPenWidth(width: Float)

    // OCR / Search
    suspend fun setOcrEnabled(enabled: Boolean)
    suspend fun setAutomaticIndexing(enabled: Boolean)

    // Downloads
    suspend fun setWifiOnlyDownloads(enabled: Boolean)
}
