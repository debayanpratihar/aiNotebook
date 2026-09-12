package com.debayan.ainotebook.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours this app needs that Material 3 has no role for.
 *
 * Material's roles describe *chrome* — containers, outlines, on-colours. They cannot express "the
 * colour of a ruled line", "the red of a margin rule", or "the amber that means the recognizer is
 * unsure", and forcing those onto `secondaryContainer` or `error` would make an unrelated tweak to
 * the scheme silently change the meaning of the recognition heatmap. Keeping them here means the
 * page furniture and the confidence ramp are versioned independently of the Material scheme.
 *
 * Held in a [staticCompositionLocalOf]: these values change only when the whole theme changes, so
 * paying for a full subtree recomposition on that rare event is cheaper than the read-tracking a
 * dynamic local would add to every draw of every ruled page.
 */
@Immutable
data class PaperColors(
    /** Fill of a notebook page. Distinct from `surface` so a page reads as a page on top of chrome. */
    val paper: Color,
    /** The 1px edge that separates a page from the surface behind it. */
    val paperEdge: Color,
    val ruledLine: Color,
    val gridLine: Color,
    /** The classic vertical rule down the left margin. */
    val marginRule: Color,
    /** Default stroke colour for the user's own ink. */
    val ink: Color,
    /** Ink at reading-but-secondary weight: captions, metadata, disabled strokes. */
    val inkMuted: Color,
    /** Brand teal, unmodified, for decorative marks that carry no text. */
    val accentTeal: Color,
    /** Brand gold, for highlights and the "this came from the cloud" attribution. */
    val accentGold: Color,
    /** Translucent wash used to mark search hits over existing content. */
    val highlighter: Color,
    /** Background wash that marks a block as machine-written rather than hand-written. */
    val aiAnswerTint: Color,
    val aiAnswerBorder: Color,
    /** Recognition below the review threshold: the reading is probably wrong. */
    val confidenceLow: Color,
    /** Recognition that parsed but is worth a glance. */
    val confidenceMedium: Color,
    /** Recognition we are willing to solve against without asking. */
    val confidenceHigh: Color,
) {

    /**
     * Maps a recognizer confidence in `0f..1f` onto the heatmap ramp.
     *
     * The low cut-off mirrors the threshold the recognition layer uses to flag a word for review,
     * so a word that the pipeline marks as suspect and a word that the UI paints red are always the
     * same word — a heatmap that disagrees with the "check this" nudge trains users to ignore both.
     */
    fun confidenceColor(confidence: Float): Color = when (ConfidenceLevel.of(confidence)) {
        ConfidenceLevel.LOW -> confidenceLow
        ConfidenceLevel.MEDIUM -> confidenceMedium
        ConfidenceLevel.HIGH -> confidenceHigh
    }

    fun confidenceColor(level: ConfidenceLevel): Color = when (level) {
        ConfidenceLevel.LOW -> confidenceLow
        ConfidenceLevel.MEDIUM -> confidenceMedium
        ConfidenceLevel.HIGH -> confidenceHigh
    }
}

/** The three buckets the recognition heatmap collapses a continuous confidence into. */
enum class ConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {
        /** Below this a reading is treated as unverified; it matches the recognizer's own cut-off. */
        const val LOW_MAX: Float = 0.62f

        /** At or above this the reading is trusted silently. */
        const val HIGH_MIN: Float = 0.85f

        fun of(confidence: Float): ConfidenceLevel = when {
            confidence < LOW_MAX -> LOW
            confidence < HIGH_MIN -> MEDIUM
            else -> HIGH
        }
    }
}

/**
 * Defaults to the light palette rather than throwing.
 *
 * A component that renders in a `@Preview`, in a screenshot test, or inside a dialog window that
 * some caller forgot to wrap should look slightly wrong, not crash. Crashing on a missing design
 * token has never once helped a user.
 */
val LocalPaperColors = staticCompositionLocalOf { LightPaperColors }

/**
 * Whether the user has asked the system to stop animating (developer options' animator scale at 0,
 * or an accessibility setting that zeroes it). Animations that convey state must degrade to an
 * instant or a fade rather than disappearing, so the information survives.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Access point for the tokens Material 3 does not model. Mirrors `MaterialTheme`'s shape. */
object PaperTheme {

    val colors: PaperColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPaperColors.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current

    /** Type styles outside the Material scale: the answer face, expressions, handwriting. */
    val text: PaperTextStyles
        get() = PaperTextStyles
}
