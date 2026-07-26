package com.debayan.ainotebook.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Reads and writes [UserPreferences] to a Preferences DataStore.
 *
 * IO errors while reading are recovered by emitting empty preferences (which map to defaults) so a
 * transient read failure never crashes the settings flow.
 */
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            UserPreferences(
                themeMode = preferences[PreferencesKeys.THEME_MODE].toThemeMode(),
                useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR]
                    ?: UserPreferences.DEFAULT.useDynamicColor,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    private fun String?.toThemeMode(): ThemeMode =
        this?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: UserPreferences.DEFAULT.themeMode
}
