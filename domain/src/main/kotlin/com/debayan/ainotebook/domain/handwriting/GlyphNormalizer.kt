package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.handwriting.GlyphLibrary
import com.debayan.ainotebook.domain.model.handwriting.GlyphSample
import com.debayan.ainotebook.domain.model.handwriting.NormalizedPoint
import com.debayan.ainotebook.domain.model.handwriting.NormalizedStroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Turns ink the user just wrote in a training box into a [GlyphSample] in glyph space.
 *
 * Everything here hangs off the two guides the capture box prints — the baseline and the x-height
 * line. Those are the only shared reference the samples have: scaling each glyph to its own ink
 * bounds instead would make `x`, `h` and `p` identical in height and the synthesized output would
 * read as a ransom note. Vertical position is therefore taken verbatim from the guides, and only
 * the horizontal origin is derived from the ink, since where inside the box the writer chose to
 * start carries no information about the letter.
 */
object GlyphNormalizer {

    /**
     * Normalizes one captured glyph. [strokes] are raw pen traces in the capture box's own
     * coordinates, in the order they were drawn; [boxLeft] and [baselineY] are the printed guides
     * and [xHeightPx] the distance between the baseline and the x-height line.
     *
     * Returns a sample with no strokes when there is nothing usable to store, which the training
     * flow detects through [GlyphSample.isUsable] and turns into a "write that one again" prompt.
     * Throwing here would take down the capture screen over a palm graze.
     */
    fun normalize(
        strokes: List<List<StrokePoint>>,
        boxLeft: Float,
        baselineY: Float,
        xHeightPx: Float,
        glyph: String,
        variantIndex: Int,
        id: String,
        recordedAt: Long,
    ): GlyphSample {
        val xHeight = if (xHeightPx.isFinite() && xHeightPx > 0f) xHeightPx else FALLBACK_X_HEIGHT_PX
        val traces = strokes.mapNotNull { clean(it, xHeight) }
        if (traces.isEmpty()) {
            return GlyphSample(
                id = id,
                glyph = glyph,
                variantIndex = variantIndex,
                strokes = emptyList(),
                advanceWidth = GlyphLibrary.DEFAULT_ADVANCE_WIDTH,
                recordedAt = recordedAt,
            )
        }

        val ink = StrokeGeometry.boundsOfStrokes(traces)
        // Honour how tightly the writer started against the box edge, but cap it: a letter drawn in
        // the middle of the box would otherwise carry a two-x-height indent into every word.
        val leftBearing = (ink.left - boxLeft).coerceIn(
            xHeight * MIN_SIDE_BEARING,
            xHeight * MAX_LEFT_BEARING,
        )
        val rightBearing = xHeight * DEFAULT_SIDE_BEARING
        val originX = ink.left - leftBearing
        val advance = ((ink.width + leftBearing + rightBearing) / xHeight)
            .coerceIn(MIN_ADVANCE_WIDTH, MAX_ADVANCE_WIDTH)

        // Some digitizers report a flat zero for pressure rather than omitting it. Taking that at
        // face value would store a glyph that renders with no width at all, so treat a sample that
        // never rises above the noise floor as "pressure unavailable" and store full pressure.
        val reportsPressure = traces.any { trace -> trace.any { it.pressure > NO_PRESSURE_EPSILON } }

        val normalized = traces.map { trace ->
            val points = ArrayList<NormalizedPoint>(trace.size.coerceAtLeast(GlyphSample.MIN_POINTS))
            for (point in trace) {
                points += NormalizedPoint(
                    x = (point.x - originX) / xHeight,
                    y = (point.y - baselineY) / xHeight,
                    pressure = if (reportsPressure) point.pressure.coerceIn(MIN_PRESSURE, 1f) else 1f,
                )
            }
            if (points.size == 1) {
                // The dot on an "i" and a full stop are single taps. Storing them as one point would
                // make them invisible at synthesis, where a stroke needs two points to be drawn.
                val only = points[0]
                points += only.copy(x = only.x + DOT_LENGTH)
            }
            NormalizedStroke(points)
        }

        return GlyphSample(
            id = id,
            glyph = glyph,
            variantIndex = variantIndex,
            strokes = normalized,
            advanceWidth = advance,
            recordedAt = recordedAt,
        )
    }

    /** [normalize] for callers holding committed [Stroke]s from the capture canvas. */
    fun normalizeStrokes(
        strokes: List<Stroke>,
        boxLeft: Float,
        baselineY: Float,
        xHeightPx: Float,
        glyph: String,
        variantIndex: Int,
        id: String,
        recordedAt: Long,
    ): GlyphSample = normalize(
        strokes = strokes.map { it.points },
        boxLeft = boxLeft,
        baselineY = baselineY,
        xHeightPx = xHeightPx,
        glyph = glyph,
        variantIndex = variantIndex,
        id = id,
        recordedAt = recordedAt,
    )

    /**
     * Measures the writer's forward lean, in degrees, from their own [samples]. Positive leans
     * right; zero when the evidence is too thin to trust.
     *
     * Only ink above the x-height line counts, and only where it runs more vertically than
     * horizontally. That narrows the measurement to ascender stems — the one part of a hand that is
     * supposed to be a straight upright — and excludes bowls and crossbars, whose curvature would
     * average the lean towards nothing. Each stem is oriented upward before it is summed, so a
     * stem drawn top-down and one drawn bottom-up reinforce each other instead of cancelling.
     */
    fun estimateSlantDegrees(samples: List<GlyphSample>): Float {
        var horizontal = 0f
        var vertical = 0f
        var evidence = 0f
        for (sample in samples) {
            if (sample.glyph !in STEM_GLYPHS) continue
            for (stroke in sample.strokes) {
                val points = stroke.points
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    if (a.y > ASCENDER_ZONE_TOP && b.y > ASCENDER_ZONE_TOP) continue
                    var dx = b.x - a.x
                    var dy = b.y - a.y
                    if (abs(dy) < abs(dx) * MIN_VERTICALITY) continue
                    if (dy > 0f) {
                        dx = -dx
                        dy = -dy
                    }
                    val length = sqrt(dx * dx + dy * dy)
                    if (length <= 0f) continue
                    horizontal += dx
                    vertical += -dy
                    evidence += length
                }
            }
        }
        if (evidence < MIN_STEM_EVIDENCE || vertical <= 0f) return 0f
        val degrees = atan2(horizontal, vertical) * RADIANS_TO_DEGREES
        // Under a degree is indistinguishable from sampling noise, and shearing by it only costs
        // precision in the transform.
        if (abs(degrees) < MIN_REPORTED_SLANT) return 0f
        return degrees.coerceIn(-MAX_SLANT_DEGREES, MAX_SLANT_DEGREES)
    }

    /**
     * Duplicate removal, spline densification, then arc-length resampling. Densifying first matters
     * for glyphs written fast: those arrive as a handful of points, and resampling them directly
     * would walk the chords and store a letter with corners the writer never made.
     */
    private fun clean(stroke: List<StrokePoint>, xHeight: Float): List<StrokePoint>? {
        val spacing = xHeight * RESAMPLE_SPACING
        val deduped = StrokeGeometry.dropDuplicates(stroke, xHeight * DEDUPE_SPACING)
        if (deduped.isEmpty()) return null
        if (deduped.size < GlyphSample.MIN_POINTS) return deduped
        val span = StrokeGeometry.polylineLength(deduped) / (deduped.size - 1)
        val samplesPerSpan = ceil(span / spacing).toInt().coerceIn(1, MAX_SAMPLES_PER_SPAN)
        val dense = StrokeGeometry.catmullRom(deduped, samplesPerSpan)
        return StrokeGeometry.resample(dense, spacing)
    }

    /** Stored point spacing, in x-heights. Fine enough for a letter, coarse enough to stay small. */
    const val RESAMPLE_SPACING: Float = 0.06f

    /** Points closer together than this, in x-heights, are the pen resting rather than moving. */
    const val DEDUPE_SPACING: Float = 0.012f

    private const val MIN_SIDE_BEARING: Float = 0.03f
    private const val DEFAULT_SIDE_BEARING: Float = 0.08f
    private const val MAX_LEFT_BEARING: Float = 0.22f

    private const val MIN_ADVANCE_WIDTH: Float = 0.25f
    private const val MAX_ADVANCE_WIDTH: Float = 4f

    private const val DOT_LENGTH: Float = 0.02f
    private const val MAX_SAMPLES_PER_SPAN: Int = 8
    private const val FALLBACK_X_HEIGHT_PX: Float = 1f

    private const val NO_PRESSURE_EPSILON: Float = 0.01f
    private const val MIN_PRESSURE: Float = 0.05f

    /** Glyph-space y above which ink belongs to an ascender rather than the x-height body. */
    private const val ASCENDER_ZONE_TOP: Float = -1.05f
    private const val MIN_VERTICALITY: Float = 1.5f
    private const val MIN_STEM_EVIDENCE: Float = 0.5f
    private const val MIN_REPORTED_SLANT: Float = 1f
    private const val MAX_SLANT_DEGREES: Float = 30f

    private val RADIANS_TO_DEGREES: Float = (180.0 / PI).toFloat()

    /**
     * Characters whose common forms carry one dominant upright stem reaching past the x-height.
     * Round capitals and diagonals are left out: their near-vertical segments lean both ways and
     * only add noise to the average.
     */
    private val STEM_GLYPHS: Set<String> = setOf(
        "b", "d", "f", "h", "k", "l", "t",
        "B", "D", "E", "F", "H", "I", "K", "L", "P", "R", "T",
        "1",
    )
}
