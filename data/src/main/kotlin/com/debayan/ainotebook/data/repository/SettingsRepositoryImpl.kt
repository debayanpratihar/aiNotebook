package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.data.local.datastore.UserPreferencesDataSource
import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
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
}
