package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.model.handwriting.GlyphLibrary
import com.debayan.ainotebook.domain.model.handwriting.SynthesisStyle
import com.debayan.ainotebook.domain.model.handwriting.SynthesizedText

/**
 * Turns text into strokes in the user's own handwriting.
 *
 * Pure geometry, no Android and no I/O, so the layout engine — wrapping, kerning, cursive joins,
 * jitter — is fully unit-testable. Callers supply the [library] they already loaded and receive
 * geometry they then attach ids to.
 */
interface HandwritingSynthesizer {

    /**
     * Lays out [text] starting with its first baseline at ([originX], [baselineY]) in world
     * coordinates, wrapping at [SynthesisStyle.maxWidthPx].
     *
     * Never fails: characters with no sample in [library] come back as
     * [com.debayan.ainotebook.domain.model.handwriting.TypedFallbackRun]s rather than being dropped,
     * because silently omitting part of an answer is far worse than rendering it in a font.
     */
    fun synthesize(
        text: String,
        library: GlyphLibrary,
        style: SynthesisStyle,
        originX: Float,
        baselineY: Float,
    ): SynthesizedText

    /**
     * Measures [text] without building geometry, so the canvas can reserve space and decide where to
     * place an answer before committing to drawing it.
     */
    fun measure(
        text: String,
        library: GlyphLibrary,
        style: SynthesisStyle,
    ): SynthesisMeasurement
}

/** Size of a synthesized block, in world units. */
data class SynthesisMeasurement(
    val width: Float,
    val height: Float,
    val lineCount: Int,
)
