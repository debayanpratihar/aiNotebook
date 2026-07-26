package com.debayan.ainotebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-scoped ViewModel that exposes the user preferences driving app-wide appearance (theme
 * mode, dynamic color). Seeded with [UserPreferences.DEFAULT] so the first frame renders without a
 * theme flash while DataStore is read.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserPreferences.DEFAULT,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
