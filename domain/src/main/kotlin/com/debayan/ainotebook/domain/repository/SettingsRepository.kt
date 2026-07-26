package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user preferences. Backed by DataStore in the data layer.
 */
interface SettingsRepository {

    /** Emits the current preferences and every subsequent change (single source of truth). */
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
