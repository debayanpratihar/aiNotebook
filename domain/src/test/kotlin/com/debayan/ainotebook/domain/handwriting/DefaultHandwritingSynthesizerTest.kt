package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.handwriting.GlyphLibrary
import com.debayan.ainotebook.domain.model.handwriting.GlyphSample
import com.debayan.ainotebook.domain.model.handwriting.NormalizedPoint
import com.debayan.ainotebook.domain.model.handwriting.NormalizedStroke
import com.debayan.ainotebook.domain.model.handwriting.SynthesisStyle
import com.debayan.ainotebook.domain.model.handwriting.TypedFallbackRun.FallbackReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultHandwritingSynthesizerTest {

    private val synthesizer = DefaultHandwritingSynthesizer()

    /** One x-height is 20px and every glyph advances 1.1 x-heights (22px) including tracking. */
    private val style = SynthesisStyle(
        xHeightPx = 20f,
        lineSpacing = 2.5f,
        letterSpacing = 0.1f,
        wordSpacing = 0.6f,
        slantDegrees = 0f,
        jitterAmount = 0f,
        cursiveJoins = false,
        maxWidthPx = 1000f,
        randomSeed = 7L,
    )

    private val letters = libraryOf("abcdefghijklmnopqrstuvwxyz0123456789+=:")

    @Test
    fun jitter_isReproducibleFromTheSeedAndOnlyFromTheSeed() {
        val jittered = style.copy(jitterAmount = 0.06f, cursiveJoins = true)

        val first = synthesizer.synthesize("hello world", letters, jittered, 40f, 120f)
        val again = synthesizer.synthesize("hello world", letters, jittered, 40f, 120f)
        assertEquals(first, again)

        val reseeded = synthesizer.synthesize("hello world", letters, jittered.copy(randomSeed = 8L), 40f, 120f)
        assertNotEquals(first.strokes, reseeded.strokes)
        assertEquals(first.strokes.size, reseeded.strokes.size)
    }

    @Test
    fun wrapping_breaksAtSpacesAndNeverInsideAWord() {
        val wrapped = synthesizer.measure("aaa bbb ccc", letters, style.copy(maxWidthPx = 180f))
        val firstLineAlone = synthesizer.measure("aaa bbb", letters, style)

        assertEquals(2, wrapped.lineCount)
        // A mid-word break would have filled the line right up to 180 instead of stopping at 144.
        assertEquals(firstLineAlone.width, wrapped.width, 0.01f)
    }

    @Test
    fun wrapping_breaksAWordOnlyWhenItAloneOverflowsTheLine() {
        val measurement = synthesizer.measure("aaaaa", letters, style.copy(maxWidthPx = 50f))

        assertEquals(3, measurement.lineCount)
    }

    @Test
    fun explicitNewlines_moveToTheNextBaseline() {
        val result = synthesizer.synthesize("a\nb", letters, style, 0f, 100f)

        assertEquals(2, result.lineCount)
        assertEquals(100f, result.strokes.first().points.first().y, 0.01f)
        assertEquals(150f, result.strokes.last().points.first().y, 0.01f)
    }

    @Test
    fun missingCharacters_collapseIntoOneTypedRun() {
        val result = synthesizer.synthesize("a≈≈b", libraryOf("ab"), style, 0f, 0f)

        assertEquals(1, result.typedRuns.size)
        assertEquals("≈≈", result.typedRuns[0].text)
        assertEquals(FallbackReason.MISSING_GLYPH, result.typedRuns[0].reason)
        assertEquals(22f, result.typedRuns[0].originX, 0.01f)
        assertEquals(setOf("≈"), result.missingGlyphs)
        assertEquals(2, result.strokes.size)
    }

    @Test
    fun emptyLibrary_fallsBackEntirelyToTypedRunsInsteadOfFailing() {
        val result = synthesizer.synthesize("solve 2x", GlyphLibrary.EMPTY, style, 0f, 0f)

        assertTrue(result.strokes.isEmpty())
        assertEquals(listOf("solve", "2x"), result.typedRuns.map { it.text })
        assertTrue(result.typedRuns.all { it.reason == FallbackReason.MISSING_GLYPH })
        assertTrue(result.missingGlyphs.containsAll(setOf("s", "o", "l", "v", "e", "2", "x")))
    }

    @Test
    fun measure_agreesWithTheBoundsSynthesisReports() {
        val busy = style.copy(maxWidthPx = 220f, jitterAmount = 0.05f, cursiveJoins = true, slantDegrees = 9f)
        val texts = listOf(
            "a",
            "hello world",
            "hello wonderful world of ink",
            "a\nbb\nccc",
            "a≈b",
            "| a | b |\n|---|---|",
        )

        for (text in texts) {
            val measured = synthesizer.measure(text, letters, busy)
            val drawn = synthesizer.synthesize(text, letters, busy, 37f, 211f)

            assertEquals(text, measured.lineCount, drawn.lineCount)
            assertEquals(text, measured.width, drawn.bounds.width, 0.05f)
            assertEquals(text, measured.height, drawn.bounds.height, 0.05f)
        }
    }

    @Test
    fun blankInput_producesNothingAtAll() {
        for (text in listOf("", "   ", "\n\n", " \t ")) {
            val result = synthesizer.synthesize(text, letters, style, 10f, 10f)
            assertTrue(text, result.isEmpty)
            assertEquals(text, 0, result.lineCount)
            assertEquals(text, BoundingBox.EMPTY, result.bounds)

            val measured = synthesizer.measure(text, letters, style)
            assertEquals(text, 0, measured.lineCount)
            assertEquals(text, 0f, measured.width, 0f)
            assertEquals(text, 0f, measured.height, 0f)
        }
    }

    @Test
    fun everyEmittedStrokeHasEnoughPointsToBeDrawn() {
        val dotted = GlyphLibrary(
            mapOf(
                "i" to listOf(
                    sample(
                        "i",
                        strokes = listOf(
                            listOf(0.2f to 0f, 0.2f to -1f),
                            listOf(0.2f to -1.4f),
                        ),
                    ),
                ),
            ),
        )

        val result = synthesizer.synthesize("iii", dotted, style.copy(jitterAmount = 0.05f), 0f, 0f)

        assertEquals(6, result.strokes.size)
        assertTrue(result.strokes.all { it.points.size >= GlyphSample.MIN_POINTS })
    }

    @Test
    fun cursiveJoins_connectAdjacentLowercaseLetters() {
        val joined = synthesizer.synthesize("nn", letters, style.copy(cursiveJoins = true), 0f, 0f)
        val unjoined = synthesizer.synthesize("nn", letters, style.copy(cursiveJoins = false), 0f, 0f)

        assertEquals(1, joined.strokes.count { it.isCursiveJoin })
        assertEquals(0, unjoined.strokes.count { it.isCursiveJoin })
        assertTrue(joined.strokes.filter { it.isCursiveJoin }.all { it.points.size >= GlyphSample.MIN_POINTS })
    }

    @Test
    fun cursiveJoins_areSkippedAcrossSpacesAndCapitals() {
        val cursive = style.copy(cursiveJoins = true)

        assertEquals(0, synthesizer.synthesize("n n", letters, cursive, 0f, 0f).strokes.count { it.isCursiveJoin })
        assertEquals(0, synthesizer.synthesize("n7", letters, cursive, 0f, 0f).strokes.count { it.isCursiveJoin })
    }

    @Test
    fun cursiveJoins_areSkippedWhenTheSweepWouldBeLong() {
        // A letter whose pen lifts at the top, like an "o": joining from there would drag a line
        // across the next glyph.
        val topExit = GlyphLibrary(
            mapOf(
                "o" to listOf(
                    sample(
                        "o",
                        strokes = listOf(
                            listOf(0.5f to -1f, 0.1f to -0.5f, 0.5f to 0f, 0.9f to -0.5f, 0.5f to -1f),
                        ),
                    ),
                ),
            ),
        )

        val result = synthesizer.synthesize("oo", topExit, style.copy(cursiveJoins = true), 0f, 0f)

        assertEquals(0, result.strokes.count { it.isCursiveJoin })
    }

    @Test
    fun repeatedLetters_rotateThroughTheRecordedVariants() {
        val twoVariants = GlyphLibrary(
            mapOf(
                "a" to listOf(
                    sample("a", variant = 0, strokes = BOX),
                    sample("a", variant = 1, strokes = BOX + listOf(listOf(0.2f to -0.5f, 0.7f to -0.5f))),
                ),
            ),
        )

        val result = synthesizer.synthesize("aa", twoVariants, style, 0f, 0f)

        assertEquals(3, result.strokes.size)
    }

    @Test
    fun tableRows_becomeUnsupportedContentRuns() {
        val result = synthesizer.synthesize("| a | b |\n|---|---|\n| 1 | 2 |", letters, style, 0f, 0f)

        assertEquals(3, result.typedRuns.size)
        assertTrue(result.typedRuns.all { it.reason == FallbackReason.UNSUPPORTED_CONTENT })
        assertTrue(result.strokes.isEmpty())
        assertEquals(3, result.lineCount)
    }

    @Test
    fun absoluteValueBars_areNotMistakenForATableRow() {
        val result = synthesizer.synthesize("|x| + |y| = 1", letters, style, 0f, 0f)

        assertTrue(result.typedRuns.none { it.reason == FallbackReason.UNSUPPORTED_CONTENT })
        assertTrue(result.strokes.isNotEmpty())
    }

    @Test
    fun fencedCode_becomesUnsupportedAndTheFenceItselfIsNotDrawn() {
        val result = synthesizer.synthesize("answer:\n```\nval x = 1;\n```", letters, style, 0f, 0f)

        val unsupported = result.typedRuns.filter { it.reason == FallbackReason.UNSUPPORTED_CONTENT }
        assertEquals(1, unsupported.size)
        assertEquals("val x = 1;", unsupported[0].text)
        assertEquals(2, result.lineCount)
    }

    @Test
    fun slant_leansInkForwardWithoutTiltingTheBaseline() {
        val upright = synthesizer.synthesize("l", letters, style, 0f, 0f).strokes[0].points
        val leaning = synthesizer.synthesize("l", letters, style.copy(slantDegrees = 15f), 0f, 0f).strokes[0].points

        assertTrue(leaning.minBy { it.y }.x > upright.minBy { it.y }.x)
        assertEquals(upright.first().x, leaning.first().x, 0.001f)
        assertEquals(upright.first().y, leaning.first().y, 0.001f)
    }

    @Test
    fun degenerateStyleValues_produceFiniteInkRatherThanAFailure() {
        val broken = SynthesisStyle(
            xHeightPx = 0f,
            lineSpacing = 0f,
            letterSpacing = Float.NaN,
            wordSpacing = -5f,
            slantDegrees = 900f,
            jitterAmount = 50f,
            maxWidthPx = 0f,
            randomSeed = 3L,
        )

        val result = synthesizer.synthesize("hi there", letters, broken, 0f, 0f)

        assertTrue(result.strokes.isNotEmpty())
        assertTrue(result.strokes.all { it.points.size >= GlyphSample.MIN_POINTS })
        assertTrue(result.strokes.all { stroke -> stroke.points.all { it.x.isFinite() && it.y.isFinite() } })
        synthesizer.measure("hi there", letters, broken)
    }

    private fun libraryOf(characters: String, median: Float = 1f): GlyphLibrary = GlyphLibrary(
        samplesByGlyph = characters.map { it.toString() }.associateWith { listOf(sample(it)) },
        medianAdvanceWidth = median,
    )

    private fun sample(
        character: String,
        variant: Int = 0,
        advance: Float = 1f,
        strokes: List<List<Pair<Float, Float>>> = BOX,
    ) = GlyphSample(
        id = "$character-$variant",
        glyph = character,
        variantIndex = variant,
        strokes = strokes.map { stroke ->
            NormalizedStroke(stroke.map { NormalizedPoint(it.first, it.second) })
        },
        advanceWidth = advance,
        recordedAt = 0L,
    )

    private companion object {
        /** Enters and exits on the baseline, so it is a candidate for a cursive join. */
        val BOX = listOf(listOf(0.08f to 0f, 0.08f to -1f, 0.85f to -1f, 0.85f to 0f))
    }
}
