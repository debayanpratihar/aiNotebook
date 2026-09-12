package com.debayan.ainotebook.domain.model.handwriting

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.StrokePoint

/**
 * One synthesized pen trace in world coordinates, ready to become a
 * [com.debayan.ainotebook.domain.model.canvas.Stroke].
 *
 * The synthesizer stops at geometry: it has no way to mint stroke ids or layer ids without reaching
 * into persistence, so the caller attaches those. Keeping it that way is what lets the whole
 * synthesis engine live in pure Kotlin and be unit-tested without a device.
 */
data class SynthesizedStroke(
    val points: List<StrokePoint>,
    /** True for a join stroke added between letters rather than a trace the user recorded. */
    val isCursiveJoin: Boolean = false,
)

/**
 * A run of text that had to be rendered as a font instead of as handwriting.
 *
 * This happens for characters the user never recorded and for content that handwriting cannot
 * express at all (tables, code, diagrams). The UI marks these runs visibly rather than hiding the
 * substitution — silently mixing font glyphs into "your handwriting" reads as a bug.
 */
data class TypedFallbackRun(
    val text: String,
    val originX: Float,
    val baselineY: Float,
    val fontSizePx: Float,
    /** Why this run could not be handwritten, shown on long-press. */
    val reason: FallbackReason,
) {
    enum class FallbackReason {
        /** The user has no sample for at least one character in this run. */
        MISSING_GLYPH,

        /** Structured content that handwriting would render illegibly. */
        UNSUPPORTED_CONTENT,
    }
}

/**
 * The complete result of synthesizing a reply: handwritten [strokes], any [typedRuns] that had to
 * fall back to a font, and the [bounds] the caller uses to place or scroll to the answer.
 */
data class SynthesizedText(
    val strokes: List<SynthesizedStroke>,
    val typedRuns: List<TypedFallbackRun> = emptyList(),
    val missingGlyphs: Set<String> = emptySet(),
    val bounds: BoundingBox = BoundingBox.EMPTY,
    val lineCount: Int = 0,
) {
    val isEmpty: Boolean get() = strokes.isEmpty() && typedRuns.isEmpty()

    val hasFallback: Boolean get() = typedRuns.isNotEmpty()

    val totalPointCount: Int get() = strokes.sumOf { it.points.size }

    companion object {
        val EMPTY = SynthesizedText(strokes = emptyList())
    }
}
