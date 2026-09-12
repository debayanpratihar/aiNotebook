package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.handwriting.StrokeGeometry.Affine
import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.handwriting.GlyphLibrary
import com.debayan.ainotebook.domain.model.handwriting.GlyphSample
import com.debayan.ainotebook.domain.model.handwriting.NormalizedPoint
import com.debayan.ainotebook.domain.model.handwriting.SynthesisStyle
import com.debayan.ainotebook.domain.model.handwriting.SynthesizedStroke
import com.debayan.ainotebook.domain.model.handwriting.SynthesizedText
import com.debayan.ainotebook.domain.model.handwriting.TypedFallbackRun
import com.debayan.ainotebook.domain.model.handwriting.graphemes
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Lays text out as the user's own glyphs: wrapping, kerning, cursive joins, jitter and fallbacks.
 *
 * Both entry points run the identical layout pass and differ only in what they do with it, so
 * [measure] can never disagree with [synthesize] about where the text ends up — a mismatch there
 * shows up as an answer that overruns the space the canvas reserved for it.
 *
 * The class holds no state between calls: every call builds its own layout, so two pages can
 * synthesize concurrently.
 */
class DefaultHandwritingSynthesizer @Inject constructor() : HandwritingSynthesizer {

    override fun synthesize(
        text: String,
        library: GlyphLibrary,
        style: SynthesisStyle,
        originX: Float,
        baselineY: Float,
    ): SynthesizedText {
        val layout = Layouter(library, style).build(text)
        if (layout.items.isEmpty() && !layout.hasBox) return SynthesizedText.EMPTY

        // The layout pass works relative to the first baseline so that measuring needs no origin;
        // placing it is one translation composed onto every glyph's transform.
        val toWorld = Affine.translation(originX, baselineY)
        val dotLengthPx = layout.xHeightPx * DOT_LENGTH
        val strokes = ArrayList<SynthesizedStroke>(layout.items.size)
        val typedRuns = ArrayList<TypedFallbackRun>()

        for (item in layout.items) {
            when (item) {
                is Placed.Ink -> {
                    val transform = item.transform.then(toWorld)
                    for (stroke in item.sample.strokes) {
                        val points = worldPoints(stroke.points, transform, dotLengthPx)
                        if (points.size >= GlyphSample.MIN_POINTS) strokes += SynthesizedStroke(points)
                    }
                }

                is Placed.Join -> {
                    val points = StrokeGeometry.cubicBezier(
                        start = toWorld.map(item.start),
                        control1 = toWorld.map(item.control1),
                        control2 = toWorld.map(item.control2),
                        end = toWorld.map(item.end),
                        samples = item.samples,
                    )
                    if (points.size >= GlyphSample.MIN_POINTS) {
                        strokes += SynthesizedStroke(points, isCursiveJoin = true)
                    }
                }

                is Placed.Typed -> typedRuns += TypedFallbackRun(
                    text = item.text,
                    originX = originX + item.x,
                    baselineY = baselineY + item.baseline,
                    fontSizePx = item.fontSizePx,
                    reason = item.reason,
                )
            }
        }

        return SynthesizedText(
            strokes = strokes,
            typedRuns = typedRuns,
            missingGlyphs = layout.missingGlyphs,
            bounds = if (layout.hasBox) {
                BoundingBox(
                    left = originX + layout.left,
                    top = baselineY + layout.top,
                    right = originX + layout.right,
                    bottom = baselineY + layout.bottom,
                )
            } else {
                BoundingBox.EMPTY
            },
            lineCount = layout.lineCount,
        )
    }

    override fun measure(
        text: String,
        library: GlyphLibrary,
        style: SynthesisStyle,
    ): SynthesisMeasurement {
        val layout = Layouter(library, style).build(text)
        return SynthesisMeasurement(
            width = layout.width,
            height = layout.height,
            lineCount = layout.lineCount,
        )
    }

    /**
     * A stored stroke of one point is a tap — the dot of an "i", a full stop — and a single point
     * has no direction to draw along, so it is opened into the shortest segment that still renders
     * as a dot under a round cap.
     */
    private fun worldPoints(
        points: List<NormalizedPoint>,
        transform: Affine,
        dotLengthPx: Float,
    ): List<StrokePoint> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<StrokePoint>(max(points.size, GlyphSample.MIN_POINTS))
        for (point in points) {
            out += StrokePoint(
                x = transform.mapX(point.x, point.y),
                y = transform.mapY(point.x, point.y),
                pressure = point.pressure.coerceIn(0f, 1f),
            )
        }
        if (out.size == 1) {
            val only = out[0]
            out += only.copy(x = only.x + dotLengthPx)
        }
        return out
    }
}

/** One thing the layout pass decided to put somewhere, in coordinates relative to the origin. */
private sealed interface Placed {

    class Ink(val sample: GlyphSample, val transform: Affine) : Placed

    class Join(
        val start: StrokePoint,
        val control1: StrokePoint,
        val control2: StrokePoint,
        val end: StrokePoint,
        val samples: Int,
    ) : Placed

    class Typed(
        val text: String,
        val x: Float,
        val baseline: Float,
        val fontSizePx: Float,
        val reason: TypedFallbackRun.FallbackReason,
    ) : Placed
}

private class LayoutResult(
    val items: List<Placed>,
    val missingGlyphs: Set<String>,
    val lineCount: Int,
    val hasBox: Boolean,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val xHeightPx: Float,
) {
    val width: Float get() = if (hasBox) right - left else 0f
    val height: Float get() = if (hasBox) bottom - top else 0f
}

/** A glyph resolved to a variant, with the pen step it costs. */
private class Resolved(val glyph: String, val sample: GlyphSample?, val advancePx: Float)

/** Ink extents of one sample in glyph space. */
private class Extents(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val centerX: Float get() = (minX + maxX) * 0.5f
    val centerY: Float get() = (minY + maxY) * 0.5f
}

/** What a cursive join needs to know about the glyph it would start from. */
private class InkContext(
    val glyph: String,
    val sample: GlyphSample,
    val transform: Affine,
    val ink: Extents,
)

/**
 * The single pass that decides where every glyph goes. One instance per call, discarded after.
 *
 * Style values arrive from persisted user settings and from model output, so each one is bounded
 * here rather than trusted: a zero x-height or a negative line spacing would not throw, it would
 * quietly produce invisible or overlapping ink that looks like a rendering bug.
 */
private class Layouter(
    private val library: GlyphLibrary,
    style: SynthesisStyle,
) {

    private val xHeight: Float = if (style.xHeightPx.isFinite()) {
        style.xHeightPx.coerceAtLeast(MIN_X_HEIGHT_PX)
    } else {
        DEFAULT_X_HEIGHT_PX
    }

    private val lineSpacingPx =
        xHeight * style.lineSpacing.finiteOr(DEFAULT_LINE_SPACING).coerceAtLeast(MIN_LINE_SPACING)
    private val letterSpacingPx =
        xHeight * style.letterSpacing.finiteOr(0f).coerceIn(-MAX_TIGHT_TRACKING, MAX_LETTER_SPACING)
    private val wordSpacingPx =
        xHeight * style.wordSpacing.finiteOr(DEFAULT_WORD_SPACING).coerceIn(MIN_WORD_SPACING, MAX_WORD_SPACING)
    private val maxWidthPx =
        if (style.maxWidthPx.isFinite() && style.maxWidthPx > 0f) style.maxWidthPx else Float.MAX_VALUE
    private val slant =
        Affine.slant(style.slantDegrees.finiteOr(0f).coerceIn(-MAX_SLANT_DEGREES, MAX_SLANT_DEGREES))
    private val jitterAmount = style.jitterAmount.finiteOr(0f).coerceIn(0f, MAX_JITTER)
    private val seed = style.randomSeed
    private val cursive = style.cursiveJoins
    private val fallbackAdvancePx = xHeight * library.medianAdvanceWidth
        .finiteOr(GlyphLibrary.DEFAULT_ADVANCE_WIDTH)
        .coerceIn(MIN_ADVANCE_WIDTH, MAX_ADVANCE_WIDTH)
    private val fontSizePx = xHeight * EM_PER_X_HEIGHT
    private val minPenStepPx = xHeight * MIN_PEN_STEP
    private val joinSamplingPx = xHeight * GlyphNormalizer.RESAMPLE_SPACING

    private val items = ArrayList<Placed>()
    private val missing = LinkedHashSet<String>()
    private val occurrences = HashMap<String, Int>()

    private var lineIndex = -1
    private var penX = 0f
    private var pendingSpacePx = 0f
    private var jitterIndex = 0
    private var previous: InkContext? = null

    private var pendingFallback: StringBuilder? = null
    private var pendingFallbackX = 0f
    private var pendingFallbackBaseline = 0f

    private var hasBox = false
    private var boxLeft = 0f
    private var boxTop = 0f
    private var boxRight = 0f
    private var boxBottom = 0f

    fun build(text: String): LayoutResult {
        val source = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd()
        if (source.isNotBlank()) {
            val lines = source.split('\n')
            var fenced = false
            for (i in lines.indices) {
                val line = lines[i]
                if (line.trimStart().startsWith(CODE_FENCE)) {
                    // Fence markers delimit, they are not content; drawing them would put three
                    // backticks in the middle of a handwritten answer.
                    fenced = !fenced
                    continue
                }
                openLine()
                when {
                    line.isBlank() -> Unit
                    fenced || isUnsupported(line, lines.getOrNull(i - 1), lines.getOrNull(i + 1)) ->
                        placeUnsupported(line.trim())

                    else -> placeLine(line)
                }
                finishLine()
            }
        }
        return LayoutResult(
            items = items,
            missingGlyphs = missing,
            lineCount = lineIndex + 1,
            hasBox = hasBox,
            left = boxLeft,
            top = boxTop,
            right = boxRight,
            bottom = boxBottom,
            xHeightPx = xHeight,
        )
    }

    private fun placeLine(line: String) {
        val graphemes = line.graphemes()
        var i = 0
        while (i < graphemes.size) {
            if (graphemes[i].isBlank()) {
                // Held rather than applied: a space that turns out to sit at a wrap point must not
                // leave an indent at the start of the next line.
                pendingSpacePx += wordSpacingPx
                flushFallback()
                previous = null
                i++
                continue
            }
            var end = i
            while (end < graphemes.size && !graphemes[end].isBlank()) end++
            placeWord(graphemes.subList(i, end))
            i = end
        }
    }

    private fun placeWord(word: List<String>) {
        val resolved = word.map { resolve(it) }
        var wordWidth = 0f
        for (item in resolved) wordWidth += item.advancePx

        if (penX + pendingSpacePx + wordWidth > maxWidthPx) {
            if (penX > 0f) newLine() else pendingSpacePx = 0f
        }
        commitPendingSpace()

        for (item in resolved) {
            // Only reachable for a word that does not fit a line of its own; anything that fitted as
            // a whole fits at every prefix, so ordinary words never break here.
            if (penX > 0f && penX + item.advancePx > maxWidthPx) newLine()
            place(item)
        }
    }

    private fun resolve(glyph: String): Resolved {
        val occurrence = occurrences[glyph] ?: 0
        occurrences[glyph] = occurrence + 1
        val sample = library.sampleFor(glyph, occurrence)
        val advance = if (sample == null) {
            fallbackAdvancePx
        } else {
            xHeight * sample.advanceWidth
                .finiteOr(GlyphLibrary.DEFAULT_ADVANCE_WIDTH)
                .coerceIn(MIN_ADVANCE_WIDTH, MAX_ADVANCE_WIDTH)
        }
        return Resolved(glyph, sample, (advance + letterSpacingPx).coerceAtLeast(minPenStepPx))
    }

    private fun place(item: Resolved) {
        val sample = item.sample
        if (sample == null) {
            placeFallback(item)
            return
        }
        flushFallback()

        val ink = extentsOf(sample)
        val index = jitterIndex++
        val offsetX = jitterAmount * xHeight * JITTER_OFFSET_X * noise(seed, index, CHANNEL_OFFSET_X)
        val offsetY = jitterAmount * xHeight * JITTER_OFFSET_Y * noise(seed, index, CHANNEL_OFFSET_Y)
        val scale = xHeight * (1f + jitterAmount * JITTER_SCALE * noise(seed, index, CHANNEL_SCALE))
            .coerceAtLeast(MIN_SCALE_FACTOR)
        val rotation = jitterAmount * JITTER_ROTATION_DEGREES * noise(seed, index, CHANNEL_ROTATION)

        // Rotation is about the glyph's own centre so jitter turns it in place instead of swinging
        // it off the baseline; the shear comes after the scale because it is about the baseline of
        // the line, which the following translation is what finally moves to.
        val transform = Affine.rotationAbout(ink.centerX, ink.centerY, rotation)
            .then(Affine.scale(scale, scale))
            .then(slant)
            .then(Affine.translation(penX + offsetX, baseline() + offsetY))

        val context = InkContext(item.glyph, sample, transform, ink)
        appendJoin(context)
        items += Placed.Ink(sample, transform)
        includeTransformedInk(transform, ink)
        previous = context
        penX += item.advancePx
    }

    /**
     * A character with no recorded sample. Consecutive ones accumulate into a single run: a reply
     * containing an un-trained symbol should show one marked stretch of typed text, not a per-letter
     * mosaic of them.
     */
    private fun placeFallback(item: Resolved) {
        missing += item.glyph
        previous = null
        val open = pendingFallback
        if (open == null) {
            pendingFallback = StringBuilder(item.glyph)
            pendingFallbackX = penX
            pendingFallbackBaseline = baseline()
        } else {
            open.append(item.glyph)
        }
        includeTypedBox(penX, penX + item.advancePx, baseline(), rows = 1)
        penX += item.advancePx
    }

    private fun placeUnsupported(line: String) {
        flushFallback()
        previous = null
        val estimated = line.length * fallbackAdvancePx
        val rows = if (maxWidthPx == Float.MAX_VALUE) 1 else max(1, ceil(estimated / maxWidthPx).toInt())
        val right = penX + min(estimated, maxWidthPx)
        items += Placed.Typed(line, penX, baseline(), fontSizePx, TypedFallbackRun.FallbackReason.UNSUPPORTED_CONTENT)
        includeTypedBox(penX, right, baseline(), rows)
        // The run will wrap inside the font renderer, so claim the baselines it will consume rather
        // than letting the next line of the answer land on top of it.
        repeat(rows - 1) { openLine() }
    }

    private fun flushFallback() {
        val open = pendingFallback ?: return
        pendingFallback = null
        if (open.isEmpty()) return
        items += Placed.Typed(
            text = open.toString(),
            x = pendingFallbackX,
            baseline = pendingFallbackBaseline,
            fontSizePx = fontSizePx,
            reason = TypedFallbackRun.FallbackReason.MISSING_GLYPH,
        )
    }

    /**
     * Adds the connector between [current] and the glyph before it, when the geometry allows one.
     *
     * Every test here exists to refuse a join rather than to allow one. A letter that should have
     * been joined and was not is invisible to a reader; a join drawn from the top of an "o" down
     * across the next letter is the first thing they will see.
     */
    private fun appendJoin(current: InkContext) {
        if (!cursive) return
        val prior = previous ?: return
        if (!joinable(prior.glyph) || !joinable(current.glyph)) return
        val exit = prior.sample.exitPoint ?: return
        val entry = current.sample.entryPoint ?: return
        if (abs(exit.y) > JOIN_EXIT_TOLERANCE || abs(entry.y) > JOIN_ENTRY_TOLERANCE) return
        // The pen has to leave from the right of one letter and arrive at the left of the next;
        // anything else sweeps back across ink that is already down.
        if (exit.x < prior.ink.maxX - JOIN_EXIT_INSET) return
        if (entry.x > current.ink.minX + JOIN_ENTRY_INSET) return

        val startX = prior.transform.mapX(exit.x, exit.y)
        val startY = prior.transform.mapY(exit.x, exit.y)
        val endX = current.transform.mapX(entry.x, entry.y)
        val endY = current.transform.mapY(entry.x, entry.y)
        val run = endX - startX
        val rise = endY - startY
        if (run <= 0f || run > xHeight * MAX_JOIN_RUN) return
        if (abs(rise) > xHeight * MAX_JOIN_RISE) return
        val length = sqrt(run * run + rise * rise)
        if (length < xHeight * MIN_JOIN_LENGTH || length > xHeight * MAX_JOIN_LENGTH) return

        val sag = xHeight * JOIN_SAG
        val pressure = ((exit.pressure + entry.pressure) * 0.5f * JOIN_PRESSURE)
            .coerceIn(MIN_JOIN_PRESSURE, 1f)
        val start = StrokePoint(startX, startY, pressure)
        val control1 = StrokePoint(startX + run * JOIN_CONTROL_RUN, startY + sag, pressure)
        val control2 = StrokePoint(endX - run * JOIN_CONTROL_RUN, endY + sag, pressure)
        val end = StrokePoint(endX, endY, pressure)
        items += Placed.Join(
            start = start,
            control1 = control1,
            control2 = control2,
            end = end,
            samples = max(MIN_JOIN_SAMPLES, ceil(length / joinSamplingPx).toInt()),
        )
        // A cubic stays inside its control hull, so four points bound the whole connector.
        includePoint(start.x, start.y)
        includePoint(control1.x, control1.y)
        includePoint(control2.x, control2.y)
        includePoint(end.x, end.y)
    }

    private fun joinable(glyph: String): Boolean =
        glyph.length == 1 && glyph[0] in 'a'..'z'

    /**
     * Content handwriting cannot carry. Detection is deliberately narrow: mistaking a line of maths
     * for a table costs the user their answer in their own hand, while missing a table only means an
     * ugly handwritten row.
     */
    private fun isUnsupported(line: String, previous: String?, next: String?): Boolean {
        val trimmed = line.trim()
        if (trimmed.count { it == '\t' } >= MIN_TABLE_TABS) return true

        val pipes = trimmed.count { it == '|' }
        if (pipes >= MIN_TABLE_PIPES) {
            // `|x| + |y| = 1` has more pipes than most table rows, so the pipes alone prove nothing.
            // A neighbouring rule row does, and so does the padded `a | b` cell separator.
            if (isRuleRow(trimmed) || isRuleRow(previous?.trim()) || isRuleRow(next?.trim())) return true
            if (pipes >= MIN_UNAIDED_TABLE_PIPES && trimmed.contains(PADDED_CELL_SEPARATOR)) return true
        }

        if (trimmed.count { it in CODE_PUNCTUATION } >= MIN_CODE_PUNCTUATION) return true
        if (trimmed.endsWith(';') && trimmed.contains('(')) return true
        return false
    }

    private fun isRuleRow(line: String?): Boolean {
        if (line == null || line.length < MIN_RULE_DASHES) return false
        if (line.count { it == '-' } < MIN_RULE_DASHES) return false
        return line.all { it == '-' || it == '|' || it == ':' || it == ' ' }
    }

    private fun openLine() {
        lineIndex++
        penX = 0f
        pendingSpacePx = 0f
        previous = null
    }

    private fun finishLine() {
        flushFallback()
        previous = null
        pendingSpacePx = 0f
        // The block starts at the pen origin and runs to where the pen stopped, even on a line whose
        // ink sits inside that: callers place answers against the advance box, not the ink.
        includePoint(0f, baseline())
        includePoint(penX, baseline())
    }

    private fun newLine() {
        finishLine()
        openLine()
    }

    private fun commitPendingSpace() {
        if (pendingSpacePx > 0f) {
            penX += pendingSpacePx
            pendingSpacePx = 0f
        }
    }

    private fun baseline(): Float = max(lineIndex, 0) * lineSpacingPx

    private fun extentsOf(sample: GlyphSample): Extents {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (stroke in sample.strokes) {
            for (point in stroke.points) {
                if (point.x < minX) minX = point.x
                if (point.x > maxX) maxX = point.x
                if (point.y < minY) minY = point.y
                if (point.y > maxY) maxY = point.y
            }
        }
        if (minX > maxX || minY > maxY) return Extents(0f, 0f, 0f, 0f)
        return Extents(minX, minY, maxX, maxY)
    }

    /**
     * Boxes the glyph by its transformed corners. For a rotated glyph that is marginally wider than
     * the ink itself, which is the right way to be wrong: the reserved space stays a superset of
     * what gets drawn, and measuring agrees with drawing because both read it from here.
     */
    private fun includeTransformedInk(transform: Affine, ink: Extents) {
        includePoint(transform.mapX(ink.minX, ink.minY), transform.mapY(ink.minX, ink.minY))
        includePoint(transform.mapX(ink.maxX, ink.minY), transform.mapY(ink.maxX, ink.minY))
        includePoint(transform.mapX(ink.minX, ink.maxY), transform.mapY(ink.minX, ink.maxY))
        includePoint(transform.mapX(ink.maxX, ink.maxY), transform.mapY(ink.maxX, ink.maxY))
    }

    private fun includeTypedBox(left: Float, right: Float, baseline: Float, rows: Int) {
        includePoint(left, baseline - xHeight * TYPED_ASCENT)
        includePoint(right, baseline + (rows - 1) * lineSpacingPx + xHeight * TYPED_DESCENT)
    }

    private fun includePoint(x: Float, y: Float) {
        if (!hasBox) {
            hasBox = true
            boxLeft = x
            boxRight = x
            boxTop = y
            boxBottom = y
            return
        }
        if (x < boxLeft) boxLeft = x
        if (x > boxRight) boxRight = x
        if (y < boxTop) boxTop = y
        if (y > boxBottom) boxBottom = y
    }
}

/**
 * Jitter for glyph [index] on [channel], in `[-1, 1)`, from [seed] alone.
 *
 * A stateful generator would be enough for one pass, but not for this one: the same reply is
 * re-synthesized after an undo, after a page reload, and again at export resolution, and all three
 * have to reproduce the same ink. Addressing the noise by index rather than by draw order means the
 * result does not depend on how much of the text was laid out before it. `Math.random()` and
 * `Random.Default` cannot satisfy that and must never appear here.
 *
 * The mixing function is SplitMix64's finalizer, which decorrelates neighbouring indices — without
 * it, consecutive glyphs would drift in the same direction and the line would visibly bow.
 */
private fun noise(seed: Long, index: Int, channel: Int): Float {
    var z = seed + index.toLong() * GOLDEN_GAMMA + channel.toLong() * CHANNEL_GAMMA
    z = (z xor (z ushr 30)) * MIX_MULTIPLIER_A
    z = (z xor (z ushr 27)) * MIX_MULTIPLIER_B
    z = z xor (z ushr 31)
    return (z ushr 11).toFloat() / MANTISSA_SCALE * 2f - 1f
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

private const val MIN_X_HEIGHT_PX = 0.5f
private const val DEFAULT_X_HEIGHT_PX = 22f
private const val DEFAULT_LINE_SPACING = 2.6f

/** Below one x-height the previous line's descenders land in the next line's ascenders. */
private const val MIN_LINE_SPACING = 1f

private const val DEFAULT_WORD_SPACING = 0.6f
private const val MIN_WORD_SPACING = 0.15f
private const val MAX_WORD_SPACING = 4f
private const val MAX_TIGHT_TRACKING = 0.3f
private const val MAX_LETTER_SPACING = 2f
private const val MIN_ADVANCE_WIDTH = 0.25f
private const val MAX_ADVANCE_WIDTH = 4f

/** Guarantees the pen always moves, so breaking an over-long word terminates. */
private const val MIN_PEN_STEP = 0.05f

private const val MAX_SLANT_DEGREES = 40f
private const val MAX_JITTER = 0.5f
private const val JITTER_OFFSET_X = 0.5f

/** Baseline wobble reads as human more than any other cue, so it gets the largest share. */
private const val JITTER_OFFSET_Y = 0.7f

private const val JITTER_SCALE = 0.6f
private const val JITTER_ROTATION_DEGREES = 30f
private const val MIN_SCALE_FACTOR = 0.5f
private const val DOT_LENGTH = 0.02f

private const val CHANNEL_OFFSET_X = 0
private const val CHANNEL_OFFSET_Y = 1
private const val CHANNEL_SCALE = 2
private const val CHANNEL_ROTATION = 3

/** Inter's x-height is about 0.55 em, so this makes fallback text match the handwriting's height. */
private const val EM_PER_X_HEIGHT = 1.83f

private const val TYPED_ASCENT = 1.3f
private const val TYPED_DESCENT = 0.4f

private const val JOIN_EXIT_TOLERANCE = 0.35f
private const val JOIN_ENTRY_TOLERANCE = 0.55f
private const val JOIN_EXIT_INSET = 0.45f
private const val JOIN_ENTRY_INSET = 0.45f
private const val MAX_JOIN_RUN = 0.85f
private const val MAX_JOIN_RISE = 0.5f
private const val MAX_JOIN_LENGTH = 0.9f

/** Letters this close already touch; a connector between them would be a blob. */
private const val MIN_JOIN_LENGTH = 0.05f

private const val JOIN_SAG = 0.06f
private const val JOIN_CONTROL_RUN = 0.35f
private const val JOIN_PRESSURE = 0.75f
private const val MIN_JOIN_PRESSURE = 0.2f
private const val MIN_JOIN_SAMPLES = 4

private const val CODE_FENCE = "```"
private const val CODE_PUNCTUATION = "{};"
private const val MIN_CODE_PUNCTUATION = 2
private const val PADDED_CELL_SEPARATOR = " | "
private const val MIN_TABLE_PIPES = 2
private const val MIN_UNAIDED_TABLE_PIPES = 3
private const val MIN_TABLE_TABS = 2
private const val MIN_RULE_DASHES = 3

private const val GOLDEN_GAMMA = -0x61C8864680B583EBL
private const val CHANNEL_GAMMA = 0x27220A95L
private const val MIX_MULTIPLIER_A = -0x40A7B892E31B1A47L
private const val MIX_MULTIPLIER_B = -0x6B2FB644ECCEEE15L
private const val MANTISSA_SCALE = 9.007199254740992E15f
