package com.debayan.ainotebook.domain.model.handwriting

/**
 * Every glyph sample the user has recorded, indexed by character.
 *
 * Held in memory during synthesis because a single reply touches hundreds of characters and a
 * per-character database round-trip would stall the write-on animation.
 */
data class GlyphLibrary(
    val samplesByGlyph: Map<String, List<GlyphSample>>,
    /**
     * Median advance width across all samples, used as the fallback advance for characters that were
     * never recorded, so a typed-fallback run still occupies believable space.
     */
    val medianAdvanceWidth: Float = DEFAULT_ADVANCE_WIDTH,
) {
    /**
     * Picks a variant for [glyph]. [occurrence] is the running count of how many times this character
     * has already appeared in the text being synthesized, so repeats rotate through variants instead
     * of stamping the same template — the difference between handwriting and a rubber stamp.
     */
    fun sampleFor(glyph: String, occurrence: Int): GlyphSample? {
        val variants = samplesByGlyph[glyph]?.filter { it.isUsable } ?: return null
        if (variants.isEmpty()) return null
        return variants[occurrence.mod(variants.size)]
    }

    fun has(glyph: String): Boolean = !samplesByGlyph[glyph]?.filter { it.isUsable }.isNullOrEmpty()

    /** Characters in [text] with no usable sample; these render as typed fallback. */
    fun missingGlyphsIn(text: String): Set<String> =
        text.graphemes()
            .filter { it.isNotBlank() && !has(it) }
            .toSet()

    /** Fraction of [TrainingPlan.essentialGlyphs] recorded, 0..1. Drives the training progress ring. */
    val essentialCoverage: Float
        get() {
            val essential = TrainingPlan.essentialGlyphs
            if (essential.isEmpty()) return 1f
            return essential.count { has(it) }.toFloat() / essential.size
        }

    val recordedGlyphCount: Int get() = samplesByGlyph.count { (_, samples) -> samples.any { it.isUsable } }

    val totalSampleCount: Int get() = samplesByGlyph.values.sumOf { samples -> samples.count { it.isUsable } }

    /**
     * Whether handwritten replies are worth offering. Below this the output is mostly typed fallback,
     * which looks broken rather than personal.
     */
    val supportsHandwrittenReplies: Boolean get() = essentialCoverage >= MIN_ESSENTIAL_COVERAGE

    companion object {
        val EMPTY = GlyphLibrary(emptyMap())

        /** One x-height of advance, a reasonable default for an unseen character. */
        const val DEFAULT_ADVANCE_WIDTH: Float = 1.0f

        const val MIN_ESSENTIAL_COVERAGE: Float = 0.9f
    }
}

/**
 * Splits a string into user-perceived characters.
 *
 * Naive iteration over `Char` would break the very symbols this feature exists to support: several
 * required math glyphs sit outside the BMP or combine, and half a surrogate pair can never match a
 * recorded sample.
 */
fun String.graphemes(): List<String> {
    val out = ArrayList<String>(length)
    var i = 0
    while (i < length) {
        val codePoint = codePointAt(i)
        val width = Character.charCount(codePoint)
        var end = i + width
        // Absorb any following combining marks so "e" + acute stays one unit.
        while (end < length) {
            val next = codePointAt(end)
            if (Character.getType(next).let {
                    it == Character.NON_SPACING_MARK.toInt() ||
                        it == Character.COMBINING_SPACING_MARK.toInt() ||
                        it == Character.ENCLOSING_MARK.toInt()
                }
            ) {
                end += Character.charCount(next)
            } else {
                break
            }
        }
        out.add(substring(i, end))
        i = end
    }
    return out
}
