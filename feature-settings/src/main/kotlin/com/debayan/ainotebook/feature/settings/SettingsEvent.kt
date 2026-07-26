package com.debayan.ainotebook.feature.settings

import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode

/** User actions on the settings screen. */
sealed interface SettingsEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent
    data class SetDynamicColor(val enabled: Boolean) : SettingsEvent

    data class SetAiEnabled(val enabled: Boolean) : SettingsEvent
    data class SetAutomaticAi(val enabled: Boolean) : SettingsEvent
    data class SetAiTimeout(val seconds: Int) : SettingsEvent
    data class SetStreamAi(val enabled: Boolean) : SettingsEvent

    data class SetSmoothing(val mode: SmoothingMode) : SettingsEvent
    data class SetPressure(val enabled: Boolean) : SettingsEvent
    data class SetPenWidth(val width: Float) : SettingsEvent

    data class SetOcrEnabled(val enabled: Boolean) : SettingsEvent
    data class SetAutomaticIndexing(val enabled: Boolean) : SettingsEvent

    data class SetWifiOnly(val enabled: Boolean) : SettingsEvent
}
