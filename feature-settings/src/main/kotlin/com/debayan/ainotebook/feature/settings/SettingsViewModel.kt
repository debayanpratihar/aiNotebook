package com.debayan.ainotebook.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserPreferences.DEFAULT)

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetThemeMode -> settingsRepository.setThemeMode(event.mode)
                is SettingsEvent.SetDynamicColor -> settingsRepository.setDynamicColor(event.enabled)
                is SettingsEvent.SetAiEnabled -> settingsRepository.setAiEnabled(event.enabled)
                is SettingsEvent.SetAutomaticAi -> settingsRepository.setAutomaticAiGeneration(event.enabled)
                is SettingsEvent.SetAiTimeout -> settingsRepository.setAiInactivityTimeout(event.seconds)
                is SettingsEvent.SetStreamAi -> settingsRepository.setStreamAiResponses(event.enabled)
                is SettingsEvent.SetSmoothing -> settingsRepository.setDefaultSmoothing(event.mode)
                is SettingsEvent.SetPressure -> settingsRepository.setPressureSensitivity(event.enabled)
                is SettingsEvent.SetPenWidth -> settingsRepository.setDefaultPenWidth(event.width)
                is SettingsEvent.SetOcrEnabled -> settingsRepository.setOcrEnabled(event.enabled)
                is SettingsEvent.SetAutomaticIndexing -> settingsRepository.setAutomaticIndexing(event.enabled)
                is SettingsEvent.SetWifiOnly -> settingsRepository.setWifiOnlyDownloads(event.enabled)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
