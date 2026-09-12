package com.debayan.ainotebook.domain.model

import com.debayan.ainotebook.domain.model.ai.InferenceBackend
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode

/**
 * User-controlled application preferences persisted via DataStore. Covers the settings that drive
 * runtime behavior across the app; additional cosmetic/diagnostic settings extend this model as
 * their features land.
 */
data class UserPreferences(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    // AI
    val aiEnabled: Boolean = true,
    val automaticAiGeneration: Boolean = false,
    val aiInactivityTimeoutSeconds: Int = 3,
    val streamAiResponses: Boolean = true,

    /**
     * Whether the app may send recognized text to a configured cloud provider.
     *
     * Defaults to false: cloud use is opt-in, and the consent has to be a deliberate act rather
     * than a default the user discovers afterwards. Recognized *text* is all that is ever sent —
     * never ink, never the handwriting model, never the page image.
     */
    val cloudAiEnabled: Boolean = false,

    /**
     * Whether the on-device language model may be used. Turning this off lets a user who has an API
     * key skip the model download entirely, or reclaim the space after adding one.
     */
    val localModelEnabled: Boolean = true,

    /** Preferred local backend when both are available. */
    val inferenceBackend: InferenceBackend = InferenceBackend.MEDIAPIPE,

    /**
     * Caps device work: offline math plus cloud only, no local model, no continuous recognition.
     * Auto-enabled on first run for devices below the low-memory threshold and then user-owned.
     */
    val lowMemoryMode: Boolean = false,

    /** Cache cloud answers so repeating a question is instant and costs nothing. */
    val cacheAiResponses: Boolean = true,

    // Handwriting
    /** Render AI answers as strokes in the user's own handwriting rather than as typed text. */
    val handwrittenRepliesEnabled: Boolean = true,

    /**
     * Recognize and solve continuously as the user writes, instead of waiting for an explicit tap.
     * Off by default: it costs battery and can feel intrusive mid-thought.
     */
    val solveAsYouWrite: Boolean = false,

    /** BCP-47 tag of the handwriting recognition model to use. */
    val recognitionLanguageTag: String = "en-US",
    // Drawing / Canvas
    val defaultSmoothing: SmoothingMode = SmoothingMode.MEDIUM,
    val pressureSensitivityEnabled: Boolean = true,
    val defaultPenWidth: Float = 3f,
    // OCR / Search
    val ocrEnabled: Boolean = true,
    val automaticIndexing: Boolean = true,
    // Downloads
    val wifiOnlyDownloads: Boolean = true,
) {
    companion object {
        val DEFAULT = UserPreferences()
    }
}
