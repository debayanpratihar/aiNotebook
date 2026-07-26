package com.debayan.ainotebook.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Reads and writes [UserPreferences] to a Preferences DataStore. IO errors while reading are
 * recovered by emitting empty preferences (which map to defaults) so a transient failure never
 * crashes the settings flow. Missing keys fall back to [UserPreferences.DEFAULT].
 */
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences.toUserPreferences() }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled }

    suspend fun setAiEnabled(enabled: Boolean) = edit { it[PreferencesKeys.AI_ENABLED] = enabled }
    suspend fun setAutomaticAiGeneration(enabled: Boolean) = edit { it[PreferencesKeys.AUTOMATIC_AI] = enabled }
    suspend fun setAiInactivityTimeout(seconds: Int) = edit { it[PreferencesKeys.AI_INACTIVITY_TIMEOUT] = seconds }
    suspend fun setStreamAiResponses(enabled: Boolean) = edit { it[PreferencesKeys.STREAM_AI] = enabled }

    suspend fun setDefaultSmoothing(mode: SmoothingMode) = edit { it[PreferencesKeys.DEFAULT_SMOOTHING] = mode.name }
    suspend fun setPressureSensitivity(enabled: Boolean) = edit { it[PreferencesKeys.PRESSURE_SENSITIVITY] = enabled }
    suspend fun setDefaultPenWidth(width: Float) = edit { it[PreferencesKeys.DEFAULT_PEN_WIDTH] = width }

    suspend fun setOcrEnabled(enabled: Boolean) = edit { it[PreferencesKeys.OCR_ENABLED] = enabled }
    suspend fun setAutomaticIndexing(enabled: Boolean) = edit { it[PreferencesKeys.AUTOMATIC_INDEXING] = enabled }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) = edit { it[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = enabled }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences.DEFAULT
        return UserPreferences(
            themeMode = this[PreferencesKeys.THEME_MODE].toThemeMode(),
            useDynamicColor = this[PreferencesKeys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
            aiEnabled = this[PreferencesKeys.AI_ENABLED] ?: defaults.aiEnabled,
            automaticAiGeneration = this[PreferencesKeys.AUTOMATIC_AI] ?: defaults.automaticAiGeneration,
            aiInactivityTimeoutSeconds = this[PreferencesKeys.AI_INACTIVITY_TIMEOUT] ?: defaults.aiInactivityTimeoutSeconds,
            streamAiResponses = this[PreferencesKeys.STREAM_AI] ?: defaults.streamAiResponses,
            defaultSmoothing = this[PreferencesKeys.DEFAULT_SMOOTHING].toSmoothing(),
            pressureSensitivityEnabled = this[PreferencesKeys.PRESSURE_SENSITIVITY] ?: defaults.pressureSensitivityEnabled,
            defaultPenWidth = this[PreferencesKeys.DEFAULT_PEN_WIDTH] ?: defaults.defaultPenWidth,
            ocrEnabled = this[PreferencesKeys.OCR_ENABLED] ?: defaults.ocrEnabled,
            automaticIndexing = this[PreferencesKeys.AUTOMATIC_INDEXING] ?: defaults.automaticIndexing,
            wifiOnlyDownloads = this[PreferencesKeys.WIFI_ONLY_DOWNLOADS] ?: defaults.wifiOnlyDownloads,
        )
    }

    private fun String?.toThemeMode(): ThemeMode =
        this?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: UserPreferences.DEFAULT.themeMode

    private fun String?.toSmoothing(): SmoothingMode =
        this?.let { runCatching { SmoothingMode.valueOf(it) }.getOrNull() } ?: UserPreferences.DEFAULT.defaultSmoothing
}
