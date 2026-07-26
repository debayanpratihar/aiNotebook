package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.data.local.datastore.UserPreferencesDataSource
import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import com.debayan.ainotebook.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [SettingsRepository] backed by DataStore via [UserPreferencesDataSource]. */
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : SettingsRepository {

    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)
    override suspend fun setDynamicColor(enabled: Boolean) = dataSource.setDynamicColor(enabled)

    override suspend fun setAiEnabled(enabled: Boolean) = dataSource.setAiEnabled(enabled)
    override suspend fun setAutomaticAiGeneration(enabled: Boolean) = dataSource.setAutomaticAiGeneration(enabled)
    override suspend fun setAiInactivityTimeout(seconds: Int) = dataSource.setAiInactivityTimeout(seconds)
    override suspend fun setStreamAiResponses(enabled: Boolean) = dataSource.setStreamAiResponses(enabled)

    override suspend fun setDefaultSmoothing(mode: SmoothingMode) = dataSource.setDefaultSmoothing(mode)
    override suspend fun setPressureSensitivity(enabled: Boolean) = dataSource.setPressureSensitivity(enabled)
    override suspend fun setDefaultPenWidth(width: Float) = dataSource.setDefaultPenWidth(width)

    override suspend fun setOcrEnabled(enabled: Boolean) = dataSource.setOcrEnabled(enabled)
    override suspend fun setAutomaticIndexing(enabled: Boolean) = dataSource.setAutomaticIndexing(enabled)

    override suspend fun setWifiOnlyDownloads(enabled: Boolean) = dataSource.setWifiOnlyDownloads(enabled)
}
