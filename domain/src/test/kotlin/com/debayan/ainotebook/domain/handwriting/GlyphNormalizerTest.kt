package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.handwriting.GlyphSample
import com.debayan.ainotebook.domain.model.handwriting.NormalizedPoint
import com.debayan.ainotebook.domain.model.handwriting.NormalizedStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

class GlyphNormalizerTest {

    @Test
    fun normalize_measuresAgainstThePrintedGuidesNotTheInkBounds() {
        // Two stems of different heights, written against the same guides: an ascender and an
        // x-height letter. Scaling each to its own bounds would make both exactly one unit tall.
        val ascender = (0..18).map { StrokePoint(120f, BASELINE - it * 4f) }
        val body = (0..10).map { StrokePoint(120f, BASELINE - it * 4f) }

        val tall = normalize(listOf(ascender), glyph = "h")
        val short = normalize(listOf(body), glyph = "x")

        assertEquals(-1.8f, tall.minY(), 0.03f)
        assertEquals(-1f, short.minY(), 0.03f)
        assertEquals(0f, tall.maxY(), 0.03f)
        assertEquals(0f, short.maxY(), 0.03f)
    }

    @Test
    fun normalize_advanceCoversTheInkPlusSideBearings() {
        val square = listOf(
            StrokePoint(140f, BASELINE),
            StrokePoint(180f, BASELINE),
            StrokePoint(180f, BASELINE - 40f),
            StrokePoint(140f, BASELINE - 40f),
            StrokePoint(140f, BASELINE),
        )

        val sample = normalize(listOf(square), glyph = "o")

        // One x-height of ink plus bearings, and the capped left bearing keeps the 40px the writer
        // left inside the box from becoming a full x-height of indent.
        assertTrue(sample.advanceWidth.toString(), sample.advanceWidth > 1f)
        assertTrue(sample.advanceWidth.toString(), sample.advanceWidth < 1.5f)
        assertTrue(sample.minX() >= 0f)
        assertTrue(sample.maxX() <= sample.advanceWidth)
    }

    @Test
    fun normalize_storesFastAndSlowWritingComparably() {
        val slow = (0..40).map { StrokePoint(100f + it * 2f, BASELINE) }
        val fast = listOf(
            StrokePoint(100f, BASELINE),
            StrokePoint(140f, BASELINE),
            StrokePoint(180f, BASELINE),
        )

        val dense = normalize(listOf(slow))
        val sparse = normalize(listOf(fast))

        assertEquals(dense.pointCount, sparse.pointCount)
        assertEquals(dense.advanceWidth, sparse.advanceWidth, 0.001f)
        for (gap in sparse.gaps()) {
            assertTrue("gap $gap is not the stored spacing", gap > 0.03f && gap < 0.09f)
        }
    }

    @Test
    fun normalize_dropsPointsWhereThePenOnlyRested() {
        val resting = List(30) { StrokePoint(100f, BASELINE) } + StrokePoint(140f, BASELINE)

        val sample = normalize(listOf(resting))

        assertTrue(sample.pointCount >= GlyphSample.MIN_POINTS)
        for (gap in sample.gaps()) assertTrue("coincident points survived", gap > 0f)
    }

    @Test
    fun normalize_keepsASingleTapAsADrawableDot() {
        val sample = normalize(listOf(listOf(StrokePoint(120f, BASELINE - 5f))))

        assertEquals(1, sample.strokes.size)
        assertEquals(2, sample.strokes[0].points.size)
        assertTrue(sample.isUsable)
    }

    @Test
    fun normalize_withNothingUsableReturnsASampleTrainingCanReject() {
        val sample = GlyphNormalizer.normalize(
            strokes = listOf(emptyList()),
            boxLeft = 100f,
            baselineY = BASELINE,
            xHeightPx = 40f,
            glyph = "q",
            variantIndex = 2,
            id = "q-2",
            recordedAt = 77L,
        )

        assertFalse(sample.isUsable)
        assertTrue(sample.strokes.isEmpty())
        assertTrue(sample.advanceWidth > 0f)
        assertEquals(2, sample.variantIndex)
        assertEquals(77L, sample.recordedAt)
    }

    @Test
    fun normalize_survivesAnUnmeasurableXHeight() {
        val stroke = listOf(StrokePoint(120f, BASELINE), StrokePoint(120f, BASELINE - 40f))

        val sample = normalize(listOf(stroke), xHeightPx = 0f)

        assertTrue(sample.isUsable)
        assertTrue(sample.strokes.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() })
        assertTrue(sample.advanceWidth.isFinite())
    }

    @Test
    fun normalize_treatsAFlatZeroPressureCaptureAsPressureUnavailable() {
        val flat = (0..10).map { StrokePoint(100f + it * 4f, BASELINE, pressure = 0f) }
        val real = (0..10).map { StrokePoint(100f + it * 4f, BASELINE, pressure = 0.5f) }

        val unreported = normalize(listOf(flat))
        val reported = normalize(listOf(real))

        assertTrue(unreported.strokes.flatMap { it.points }.all { it.pressure == 1f })
        assertTrue(reported.strokes.flatMap { it.points }.all { it.pressure in 0.49f..0.51f })
    }

    @Test
    fun estimateSlantDegrees_readsTheLeanOfAscenderStems() {
        assertEquals(15f, GlyphNormalizer.estimateSlantDegrees(listOf(stem("l", 15f))), 1.5f)
        assertEquals(0f, GlyphNormalizer.estimateSlantDegrees(listOf(stem("l", 0f))), 0.01f)
        assertTrue(GlyphNormalizer.estimateSlantDegrees(listOf(stem("l", -12f))) < -8f)
    }

    @Test
    fun estimateSlantDegrees_claimsNothingWithoutAscenderEvidence() {
        // No samples at all, a glyph with no stem to measure, and a stem that never leaves the
        // x-height body: all three are "unknown", and an unknown lean must render upright.
        assertEquals(0f, GlyphNormalizer.estimateSlantDegrees(emptyList()), 0f)
        assertEquals(0f, GlyphNormalizer.estimateSlantDegrees(listOf(stem("o", 20f))), 0f)
        assertEquals(0f, GlyphNormalizer.estimateSlantDegrees(listOf(stem("l", 20f, height = 1f))), 0f)
    }

    private fun normalize(
        strokes: List<List<StrokePoint>>,
        xHeightPx: Float = 40f,
        boxLeft: Float = 100f,
        glyph: String = "a",
    ): GlyphSample = GlyphNormalizer.normalize(
        strokes = strokes,
        boxLeft = boxLeft,
        baselineY = BASELINE,
        xHeightPx = xHeightPx,
        glyph = glyph,
        variantIndex = 0,
        id = "$glyph-0",
        recordedAt = 11L,
    )

    /** A straight stem of [height] x-heights leaning forward by [degrees], already in glyph space. */
    private fun stem(glyph: String, degrees: Float, height: Float = 1.8f): GlyphSample {
        val lean = tan(degrees * PI.toFloat() / 180f)
        val steps = (height / 0.1f).toInt()
        val points = (0..steps).map { step ->
            val y = -step * 0.1f
            NormalizedPoint(0.3f - y * lean, y)
        }
        return GlyphSample(
            id = "$glyph-0",
            glyph = glyph,
            variantIndex = 0,
            strokes = listOf(NormalizedStroke(points)),
            advanceWidth = 1f,
            recordedAt = 0L,
        )
    }

    private fun GlyphSample.minY(): Float = strokes.flatMap { it.points }.minOf { it.y }

    private fun GlyphSample.maxY(): Float = strokes.flatMap { it.points }.maxOf { it.y }

    private fun GlyphSample.minX(): Float = strokes.flatMap { it.points }.minOf { it.x }

    private fun GlyphSample.maxX(): Float = strokes.flatMap { it.points }.maxOf { it.x }

    private fun GlyphSample.gaps(): List<Float> = strokes.flatMap { stroke ->
        stroke.points.zipWithNext { a, b ->
            val dx = b.x - a.x
            val dy = b.y - a.y
            sqrt(dx * dx + dy * dy)
        }
    }

    private companion object {
        const val BASELINE = 200f
    }
}
