package com.debayan.ainotebook.domain.model.handwriting

/**
 * A point in **glyph space**: the normalized coordinate system every stored glyph shares.
 *
 * Origin is on the baseline at the glyph's left edge. One unit equals one x-height. `y` grows
 * downward to match screen and stroke coordinates, so ascenders are negative and descenders are
 * positive.
 */
data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
)

/** One pen-down-to-pen-up trace of a glyph, in glyph space. */
data class NormalizedStroke(
    val points: List<NormalizedPoint>,
) {
    val start: NormalizedPoint? get() = points.firstOrNull()
    val end: NormalizedPoint? get() = points.lastOrNull()
}

/**
 * One recorded instance of one character in the user's hand.
 *
 * Normalization is against the **capture box's printed guides** (baseline and x-height), never
 * against the glyph's own ink bounds. That distinction is what keeps synthesis looking human: if
 * each glyph were scaled to fill its own box, `x`, `h`, and `p` would all come out the same height
 * and the output would read as a ransom note. Normalizing against shared guides preserves the real
 * relative proportions the user actually wrote.
 *
 * Several [variantIndex] samples per character are kept so synthesis can rotate between them; human
 * handwriting never repeats a letter identically, and reusing one template is the most conspicuous
 * tell that text was machine-assembled.
 */
data class GlyphSample(
    val id: String,
    /** The character this sample represents, as a single grapheme (e.g. "a", "7", "√"). */
    val glyph: String,
    val variantIndex: Int,
    val strokes: List<NormalizedStroke>,
    /**
     * Horizontal distance from this glyph's origin to the next glyph's origin, in x-heights.
     * Derived from the ink width plus side bearings measured at capture time.
     */
    val advanceWidth: Float,
    val recordedAt: Long,
) {
    /** Where the pen first touches down — the join target for a preceding cursive letter. */
    val entryPoint: NormalizedPoint? get() = strokes.firstOrNull()?.start

    /** Where the pen last lifts — the join origin for a following cursive letter. */
    val exitPoint: NormalizedPoint? get() = strokes.lastOrNull()?.end

    val pointCount: Int get() = strokes.sumOf { it.points.size }

    /**
     * A sample with no ink, or a single stray dot, is worse than no sample at all: synthesis would
     * emit invisible characters. The training flow rejects these and asks for a rewrite.
     */
    val isUsable: Boolean get() = strokes.isNotEmpty() && pointCount >= MIN_POINTS

    companion object {
        const val MIN_POINTS: Int = 2

        /** Target samples per character. More than this adds variety with diminishing returns. */
        const val TARGET_VARIANTS: Int = 3
    }
}
