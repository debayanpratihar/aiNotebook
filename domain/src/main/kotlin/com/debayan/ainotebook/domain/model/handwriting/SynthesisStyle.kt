package com.debayan.ainotebook.domain.model.handwriting

/**
 * How synthesized handwriting is laid out. Spacing values are in **x-heights**, not pixels, so a
 * style stays correct at any zoom or export resolution.
 */
data class SynthesisStyle(
    /** Rendered size of one x-height in world units. The master scale control. */
    val xHeightPx: Float = 22f,

    /** Baseline-to-baseline distance. */
    val lineSpacing: Float = 2.6f,

    /** Extra advance inserted between glyphs, on top of each glyph's own advance width. */
    val letterSpacing: Float = 0.1f,

    /** Advance for a space character. */
    val wordSpacing: Float = 0.6f,

    /** Forward lean, in degrees. Measured from the user's own samples during training. */
    val slantDegrees: Float = 0f,

    /**
     * Per-glyph random variation in position, scale, and rotation, as a fraction of x-height.
     * Zero produces output that is recognizably mechanical even when every glyph is the user's own,
     * because real handwriting never places two letters on exactly the same baseline.
     */
    val jitterAmount: Float = 0.035f,

    /** Whether to draw connecting strokes between adjacent lowercase letters. */
    val cursiveJoins: Boolean = true,

    /** Wrap width in world units. */
    val maxWidthPx: Float = 900f,

    val strokeWidthPx: Float = 3f,

    /** Packed ARGB, matching [com.debayan.ainotebook.domain.model.canvas.Stroke.color]. */
    val colorArgb: Long = DEFAULT_INK_COLOR,

    /**
     * Seeds the jitter. Fixing it makes synthesis a pure function of its inputs, so re-rendering a
     * reply after an undo, a page reload, or a PNG export reproduces the identical ink instead of
     * subtly reflowing.
     */
    val randomSeed: Long = 0L,
) {
    companion object {
        /** A dark blue-black, closer to real ink than pure black. */
        const val DEFAULT_INK_COLOR: Long = 0xFF1A2340L
    }
}
