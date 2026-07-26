package com.debayan.ainotebook.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Typed keys for the app's preferences DataStore. */
internal object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")

    val AI_ENABLED = booleanPreferencesKey("ai_enabled")
    val AUTOMATIC_AI = booleanPreferencesKey("automatic_ai_generation")
    val AI_INACTIVITY_TIMEOUT = intPreferencesKey("ai_inactivity_timeout")
    val STREAM_AI = booleanPreferencesKey("stream_ai_responses")

    val DEFAULT_SMOOTHING = stringPreferencesKey("default_smoothing")
    val PRESSURE_SENSITIVITY = booleanPreferencesKey("pressure_sensitivity")
    val DEFAULT_PEN_WIDTH = floatPreferencesKey("default_pen_width")

    val OCR_ENABLED = booleanPreferencesKey("ocr_enabled")
    val AUTOMATIC_INDEXING = booleanPreferencesKey("automatic_indexing")

    val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
}
