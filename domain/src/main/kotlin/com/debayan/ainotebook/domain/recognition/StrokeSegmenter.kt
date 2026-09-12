package com.debayan.ainotebook.domain.recognition

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.recognition.InkSegment
import com.debayan.ainotebook.domain.model.recognition.SegmentationGranularity
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Groups loose strokes into lines and words, in reading order, before anything tries to recognize
 * them.
 *
 * A recognizer handed a whole page at once has to infer line breaks, word boundaries and a baseline
 * from the ink itself, and it infers them badly; handed one word's strokes it has a well-posed
 * problem. Everything here exists to make that split without ever being wrong in a way the user can
 * see.
 *
 * Three properties are load-bearing.
 *
 * Lines are clustered on a *median* vertical band rather than on raw bounding boxes, because a line's
 * strokes are not the same height: a `g` descends below the baseline and an `o` does not, so one
 * descender would otherwise stretch a line's band into the next line and start swallowing it. The
 * median of the member tops and bottoms ignores the outliers that stretching comes from.
 *
 * Word gaps are measured in units of the line's own x-height, never in pixels. An absolute threshold
 * is correct at exactly one zoom level on exactly one screen density, and silently splits every word
 * in half everywhere else.
 *
 * Segmentation is total: every input stroke leaves in exactly one segment. A stroke that cannot be
 * placed gets a line of its own rather than being dropped, because a dropped stroke is a character
 * missing from the user's expression and no downstream stage can tell that it is missing.
 */
class StrokeSegmenter @Inject constructor() {

    /**
     * Segments [strokes], which may arrive in any order — reading order is established here, not
     * assumed from the input.
     *
     * [SegmentationGranularity.WORD] is the default and the best choice for prose. Dense math wants
     * [SegmentationGranularity.LINE]: the gaps a person leaves around `+` and `=` are as wide as the
     * gaps between words, so a per-word split there hands the recognizer single operators to identify
     * out of context and leaves the reranker nothing to parse.
     */
    fun segment(
        strokes: List<Stroke>,
        granularity: SegmentationGranularity = SegmentationGranularity.WORD,
    ): List<InkSegment> {
        if (strokes.isEmpty()) return emptyList()
        val items = strokes.map { itemOf(it) }
        val reference = referenceHeight(items)
        val lines = buildLines(items, reference)

        return when (granularity) {
            SegmentationGranularity.WHOLE ->
                listOf(segmentOf(lines.flatMap { readingOrder(it) }, index = 0, lineIndex = 0))

            SegmentationGranularity.LINE -> lines.mapIndexed { lineIndex, line ->
                segmentOf(readingOrder(line), index = lineIndex, lineIndex = lineIndex)
            }

            SegmentationGranularity.WORD -> {
                val segments = mutableListOf<InkSegment>()
                lines.forEachIndexed { lineIndex, line ->
                    for (word in splitWords(line, reference)) {
                        segments += segmentOf(word, index = segments.size, lineIndex = lineIndex)
                    }
                }
                segments
            }
        }
    }

    /**
     * Recomputes the box from the points rather than trusting [Stroke.boundingBox].
     *
     * The stored box is written once when the stroke is built, so anything that later moves or scales
     * the points leaves it stale — and a stale box here does not produce a slightly worse reading, it
     * puts the stroke on the wrong line. The stored box is still the fallback for a stroke with no
     * points at all, which is the only case where nothing better exists.
     */
    private fun itemOf(stroke: Stroke): Item {
        val box = if (stroke.points.isEmpty()) {
            stroke.boundingBox
        } else {
            BoundingBox.fromPoints(stroke.points)
        }
        return Item(stroke, box)
    }

    /**
     * The median stroke height of the page, used as the scale for every threshold below.
     *
     * Median rather than mean because a page of handwriting always contains a few strokes that are
     * nothing like the rest — a long underline, a single dot — and the mean chases them. Width is the
     * fallback for a page of pure horizontal rules, whose height is zero.
     */
    private fun referenceHeight(items: List<Item>): Float {
        val heights = items.map { it.height }.filter { it > 0f }
        if (heights.isNotEmpty()) return median(heights)
        val widths = items.map { it.width }.filter { it > 0f }
        if (widths.isNotEmpty()) return median(widths)
        return 1f
    }

    /**
     * Builds the lines in four passes, in this order for a reason each time.
     *
     * Only substantial strokes seed lines. The small ones — i-dots, the bar of a fraction, an equals
     * sign, a decimal point — carry almost no vertical information, so letting them define a band is
     * how a dot above an `i` becomes its own line.
     *
     * Adjacent lines are then merged, which repairs the case where the sweep opened two bands for
     * what is one line. Small clusters sitting just above or below a bigger line are absorbed into it,
     * which is what keeps an exponent or a fraction numerator attached to the expression it belongs
     * to. Only then are the small strokes assigned, to lines that have already settled.
     */
    private fun buildLines(items: List<Item>, reference: Float): List<Line> {
        val substantialCut = reference * SUBSTANTIAL_HEIGHT_RATIO
        val core = items.filter { it.height >= substantialCut }
        val seeds = if (core.isEmpty()) items else core
        val fragments = if (core.isEmpty()) emptyList() else items.filter { it.height < substantialCut }
        val minBand = reference * MIN_BAND_RATIO

        val seeded = mutableListOf<Line>()
        for (item in seeds.sortedWith(LINE_SWEEP)) {
            val host = bestLine(seeded, item, minBand)
            if (host != null) host.addCore(item) else seeded += Line().apply { addCore(item) }
        }

        val lines = absorbStacked(mergeAdjacent(seeded, minBand))
        for (item in fragments.sortedWith(READING_ORDER)) {
            val host = bestFragmentHost(lines, item)
            if (host != null) host.addFragment(item) else lines += Line().apply { addFragment(item) }
        }
        return lines.sortedWith(LINE_ORDER)
    }

    /**
     * The line whose band this stroke overlaps most, or null when it overlaps none of them enough.
     *
     * The threshold is a fraction of the *smaller* of the two heights, which is what lets a tall
     * bracket join a line of normal-height glyphs while still keeping the tail of a `y` from joining
     * the line below it.
     */
    private fun bestLine(lines: List<Line>, item: Item, minBand: Float): Line? {
        var best: Line? = null
        var bestRatio = 0f
        for (line in lines) {
            val ratio = bandOverlapRatio(item.top, item.bottom, line.coreTop, line.coreBottom, minBand)
            if (ratio >= LINE_OVERLAP_RATIO && ratio > bestRatio) {
                bestRatio = ratio
                best = line
            }
        }
        return best
    }

    private fun mergeAdjacent(lines: List<Line>, minBand: Float): MutableList<Line> {
        val merged = mutableListOf<Line>()
        for (line in lines.sortedWith(LINE_ORDER)) {
            val previous = merged.lastOrNull()
            val ratio = if (previous == null) {
                0f
            } else {
                bandOverlapRatio(previous.coreTop, previous.coreBottom, line.coreTop, line.coreBottom, minBand)
            }
            if (previous != null && ratio >= LINE_OVERLAP_RATIO) previous.absorb(line) else merged += line
        }
        return merged
    }

    /**
     * Folds a small cluster that sits above or below a bigger line into that line: `x²`, a fraction
     * numerator, a hurried caret.
     *
     * Every condition here earns its place by excluding one real page. The height limit is what saves
     * columnar arithmetic — in long multiplication each row is a genuine line of the same height as
     * the one above it, and absorbing those would fuse the whole sum into one unreadable segment. The
     * stroke-count and width limits keep a short title from being pulled into the paragraph under it.
     * Absorbed strokes join as fragments so they cannot drag the host line's band upward and start a
     * cascade.
     */
    private fun absorbStacked(lines: MutableList<Line>): MutableList<Line> {
        if (lines.size < 2) return lines
        val absorbed = BooleanArray(lines.size)
        for (index in lines.indices) {
            if (absorbed[index]) continue
            val host = stackHost(lines, index, absorbed) ?: continue
            absorbed[index] = true
            for (member in lines[index].members) host.addFragment(member)
        }
        val remaining = mutableListOf<Line>()
        for (index in lines.indices) if (!absorbed[index]) remaining += lines[index]
        return remaining
    }

    private fun stackHost(lines: List<Line>, index: Int, absorbed: BooleanArray): Line? {
        val candidate = lines[index]
        if (candidate.size > MAX_STACKED_STROKES) return null
        var best: Line? = null
        var bestGap = Float.MAX_VALUE
        for (neighbour in intArrayOf(index - 1, index + 1)) {
            if (neighbour < 0 || neighbour >= lines.size || absorbed[neighbour]) continue
            val host = lines[neighbour]
            if (!absorbs(host, candidate)) continue
            val gap = verticalGap(host, candidate)
            if (gap < bestGap) {
                bestGap = gap
                best = host
            }
        }
        return best
    }

    private fun absorbs(host: Line, candidate: Line): Boolean {
        val hostHeight = max(host.coreHeight, EPSILON)
        val hostWidth = max(host.width, EPSILON)
        return candidate.size <= host.size &&
            candidate.coreHeight <= hostHeight * STACK_HEIGHT_RATIO &&
            candidate.width <= hostWidth * STACK_WIDTH_RATIO &&
            verticalGap(host, candidate) <= hostHeight * STACK_VERTICAL_GAP_RATIO &&
            horizontalGap(host, candidate) <= hostHeight * STACK_HORIZONTAL_GAP_RATIO
    }

    /**
     * Places a small stroke on the line it most overlaps horizontally.
     *
     * Horizontal overlap, not vertical proximity, is the deciding signal — that is the whole point of
     * this pass. A dot on an `i` and the bar over a fraction both sit *entirely above* the band of the
     * line they belong to, so vertical position alone would either orphan them or attach them to the
     * line above. What identifies their owner is that they sit directly over it. Vertical distance
     * only breaks ties, for the strokes that sit over two lines equally.
     */
    private fun bestFragmentHost(lines: List<Line>, item: Item): Line? {
        var best: Line? = null
        var bestOverlap = -Float.MAX_VALUE
        var bestDistance = Float.MAX_VALUE
        for (line in lines) {
            val height = max(line.coreHeight, EPSILON)
            val reachTop = line.coreTop - height * FRAGMENT_ASCENT_RATIO
            val reachBottom = line.coreBottom + height * FRAGMENT_DESCENT_RATIO
            if (item.bottom < reachTop || item.top > reachBottom) continue
            val overlap = min(item.right, line.right) - max(item.left, line.left)
            val distance = abs(item.centerY - line.coreCenter)
            if (overlap > bestOverlap || (overlap == bestOverlap && distance < bestDistance)) {
                bestOverlap = overlap
                bestDistance = distance
                best = line
            }
        }
        return best
    }

    /**
     * Splits a line on horizontal gaps wider than a fraction of its x-height.
     *
     * Gaps are measured against the running right edge of the current word rather than the previous
     * stroke's, so a stroke that reaches back over its neighbour stays with it: the cross of a `t`,
     * the dot of an `i`, the two bars of an `=`, the bar of a fraction, a superscript. Those strokes
     * produce a negative gap, and a negative gap is never a word boundary — which is exactly the rule
     * that keeps an equals sign from being recognized as a word on its own.
     */
    private fun splitWords(line: Line, reference: Float): List<List<Item>> {
        val threshold = xHeight(line, reference) * WORD_GAP_RATIO
        val words = mutableListOf<MutableList<Item>>()
        var right = -Float.MAX_VALUE
        for (item in readingOrder(line)) {
            val current = words.lastOrNull()
            if (current == null || item.left - right > threshold) {
                words += mutableListOf(item)
            } else {
                current += item
            }
            right = max(right, item.right)
        }
        return words
    }

    /**
     * The line's own x-height, estimated from its substantial strokes only.
     *
     * Per line rather than per page because handwriting size varies down a page — a heading, then
     * body text, then a squeezed-in afterthought — and a page-wide threshold splits the small line
     * into characters while running the large one together.
     */
    private fun xHeight(line: Line, reference: Float): Float {
        val substantialCut = reference * SUBSTANTIAL_HEIGHT_RATIO
        val heights = line.members.map { it.height }
        val substantial = heights.filter { it >= substantialCut }
        val usable = substantial.ifEmpty { heights.filter { it > 0f } }
        if (usable.isEmpty()) return reference
        val estimate = median(usable)
        return if (estimate > 0f) estimate else reference
    }

    private fun readingOrder(line: Line): List<Item> = line.members.sortedWith(READING_ORDER)

    /**
     * Identity derives from the first stroke in the segment rather than from its position on the page,
     * so a correction the user made to one word survives writing a new line above it. Positional ids
     * would renumber every segment on the page and re-apply the edit to the wrong word.
     */
    private fun segmentOf(items: List<Item>, index: Int, lineIndex: Int): InkSegment {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (item in items) {
            left = min(left, item.left)
            top = min(top, item.top)
            right = max(right, item.right)
            bottom = max(bottom, item.bottom)
        }
        return InkSegment(
            id = SEGMENT_ID_PREFIX + items.first().stroke.id,
            index = index,
            lineIndex = lineIndex,
            strokes = items.map { it.stroke },
            bounds = BoundingBox(left, top, right, bottom),
        )
    }

    /**
     * Overlap of two vertical bands as a fraction of the shorter one, with both bands inflated to a
     * floor first.
     *
     * The floor is what makes a flat stroke comparable at all: the two bars of an `=`, a minus sign
     * and a fraction rule all have a box height of nearly zero, so their true overlap with anything —
     * including each other — is zero, and without inflation each one becomes its own line.
     */
    private fun bandOverlapRatio(
        topA: Float,
        bottomA: Float,
        topB: Float,
        bottomB: Float,
        minBand: Float,
    ): Float {
        val heightA = max(bottomA - topA, minBand)
        val heightB = max(bottomB - topB, minBand)
        val centerA = (topA + bottomA) / 2f
        val centerB = (topB + bottomB) / 2f
        val overlap = min(centerA + heightA / 2f, centerB + heightB / 2f) -
            max(centerA - heightA / 2f, centerB - heightB / 2f)
        if (overlap <= 0f) return 0f
        return overlap / max(min(heightA, heightB), EPSILON)
    }

    private fun verticalGap(a: Line, b: Line): Float =
        max(0f, max(a.coreTop, b.coreTop) - min(a.coreBottom, b.coreBottom))

    private fun horizontalGap(a: Line, b: Line): Float =
        max(0f, max(a.left, b.left) - min(a.right, b.right))

    companion object {
        /** Prefix on [InkSegment.id], so a segment id is never mistaken for a stroke id. */
        const val SEGMENT_ID_PREFIX: String = "seg-"

        /**
         * Height, relative to the page's median, below which a stroke is treated as a mark attached
         * to something rather than as a glyph of its own. Half catches dots, bars and commas while
         * leaving a lower-case `o` — which is about x-height — as a glyph.
         */
        private const val SUBSTANTIAL_HEIGHT_RATIO = 0.5f

        /** Fraction of the shorter band that must overlap for two bands to be the same line. */
        private const val LINE_OVERLAP_RATIO = 0.5f

        /** Minimum band height, relative to the page's median, used when comparing flat strokes. */
        private const val MIN_BAND_RATIO = 0.15f

        /**
         * Word gap, in x-heights. Intra-word gaps in handwriting sit well below half an x-height and
         * deliberate word gaps well above it, which is the widest margin any single threshold gets.
         */
        private const val WORD_GAP_RATIO = 0.5f

        /** How far above a line's band a small stroke may sit and still belong to it: an i-dot. */
        private const val FRAGMENT_ASCENT_RATIO = 0.9f

        /** And how far below: a comma, the tail of a hurried `q`. */
        private const val FRAGMENT_DESCENT_RATIO = 0.6f

        /** A cluster of more strokes than this is a line in its own right, not an exponent. */
        private const val MAX_STACKED_STROKES = 2

        private const val STACK_HEIGHT_RATIO = 0.8f
        private const val STACK_WIDTH_RATIO = 0.8f
        private const val STACK_VERTICAL_GAP_RATIO = 0.4f
        private const val STACK_HORIZONTAL_GAP_RATIO = 0.5f

        private const val EPSILON = 1e-4f

        /**
         * The sweep order for seeding lines. Vertical centre first so lines are discovered top to
         * bottom; the stroke id last so that ink written in a different order still segments
         * identically, which is what makes the result reproducible across a save and reload.
         */
        private val LINE_SWEEP =
            compareBy<Item>({ it.centerY }, { it.left }, { it.stroke.id })

        private val READING_ORDER =
            compareBy<Item>({ it.left }, { it.top }, { it.stroke.id })

        private val LINE_ORDER = compareBy<Line>({ it.coreTop }, { it.left })
    }
}

/** One stroke plus the box actually used to place it. */
private class Item(val stroke: Stroke, box: BoundingBox) {
    val left: Float = box.left
    val top: Float = box.top
    val right: Float = box.right
    val bottom: Float = box.bottom
    val width: Float = box.width
    val height: Float = box.height
    val centerY: Float = (box.top + box.bottom) / 2f
}

/**
 * A text line under construction.
 *
 * Core members — the glyph-sized strokes — define the band; fragments join the line without moving
 * it. The member tops and bottoms are kept sorted so the median band is a lookup rather than a sort
 * on every one of the many overlap tests a page's worth of strokes performs.
 */
private class Line {
    private val coreMembers = mutableListOf<Item>()
    private val fragmentMembers = mutableListOf<Item>()
    private val coreTops = mutableListOf<Float>()
    private val coreBottoms = mutableListOf<Float>()

    var left: Float = Float.MAX_VALUE
        private set
    var right: Float = -Float.MAX_VALUE
        private set
    private var top: Float = Float.MAX_VALUE
    private var bottom: Float = -Float.MAX_VALUE

    val members: List<Item> get() = coreMembers + fragmentMembers

    val size: Int get() = coreMembers.size + fragmentMembers.size

    val width: Float get() = right - left

    /** Falls back to the full extent for a line made only of fragments, e.g. a lone dash. */
    val coreTop: Float get() = if (coreTops.isEmpty()) top else medianOfSorted(coreTops)

    val coreBottom: Float get() = if (coreBottoms.isEmpty()) bottom else medianOfSorted(coreBottoms)

    val coreHeight: Float get() = coreBottom - coreTop

    val coreCenter: Float get() = (coreTop + coreBottom) / 2f

    fun addCore(item: Item) {
        coreMembers += item
        insertSorted(coreTops, item.top)
        insertSorted(coreBottoms, item.bottom)
        extend(item)
    }

    fun addFragment(item: Item) {
        fragmentMembers += item
        extend(item)
    }

    fun absorb(other: Line) {
        for (item in other.coreMembers) addCore(item)
        for (item in other.fragmentMembers) addFragment(item)
    }

    private fun extend(item: Item) {
        if (item.left < left) left = item.left
        if (item.right > right) right = item.right
        if (item.top < top) top = item.top
        if (item.bottom > bottom) bottom = item.bottom
    }
}

private fun insertSorted(values: MutableList<Float>, value: Float) {
    val found = values.binarySearch(value)
    values.add(if (found < 0) -found - 1 else found, value)
}

private fun medianOfSorted(sorted: List<Float>): Float {
    val size = sorted.size
    if (size == 0) return 0f
    val middle = size / 2
    return if (size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2f
}

private fun median(values: List<Float>): Float = medianOfSorted(values.sorted())
