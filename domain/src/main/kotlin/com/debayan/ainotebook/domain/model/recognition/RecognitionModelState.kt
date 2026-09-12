package com.debayan.ainotebook.domain.model.recognition

/**
 * Availability of the on-device handwriting recognition model for one language.
 *
 * These models are a few megabytes and are fetched on demand rather than bundled, so the first-run
 * flow has to be able to explain the wait. This is a separate concern from the LLM download: ink
 * recognition is required for the app to function at all, while a language model is optional.
 */
sealed interface RecognitionModelState {
    data object NotDownloaded : RecognitionModelState

    data object Downloading : RecognitionModelState

    data object Ready : RecognitionModelState

    data class Failed(val message: String) : RecognitionModelState
}

/**
 * Context that measurably improves recognition quality. Every field is a hint, never a constraint —
 * a wrong hint degrades the result but cannot make recognition fail.
 */
data class RecognitionHint(
    /** BCP-47 tag, e.g. `en-US`. Drives which downloaded model is used. */
    val languageTag: String = DEFAULT_LANGUAGE_TAG,

    /**
     * Text immediately preceding the ink. The recognizer uses it as a language-model prior, which
     * is why recognizing line-by-line with the previous line as pre-context beats recognizing a
     * whole page blind.
     */
    val preContext: String = "",

    /**
     * Size of the box the ink was written in, in the same units as the stroke coordinates. Gives the
     * recognizer the scale it needs to tell a comma from an apostrophe, or a period from a decimal
     * point.
     */
    val writingAreaWidth: Float = 0f,
    val writingAreaHeight: Float = 0f,

    /**
     * Biases interpretation toward mathematical notation. Set by the heuristic pre-pass so that
     * ambiguous glyphs resolve to operators (`×`, `−`, `÷`) instead of letters.
     */
    val expectMath: Boolean = false,

    /** How many alternatives to request per segment; more gives the math reranker more to work with. */
    val maxCandidates: Int = DEFAULT_MAX_CANDIDATES,
) {
    val hasWritingArea: Boolean get() = writingAreaWidth > 0f && writingAreaHeight > 0f

    companion object {
        const val DEFAULT_LANGUAGE_TAG: String = "en-US"
        const val DEFAULT_MAX_CANDIDATES: Int = 8
    }
}
