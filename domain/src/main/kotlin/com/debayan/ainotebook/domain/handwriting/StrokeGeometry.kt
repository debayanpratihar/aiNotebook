package com.debayan.ainotebook.domain.handwriting

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The polyline and transform maths shared by glyph capture and glyph playback.
 *
 * Kept free of Android and of any model type beyond [StrokePoint] so both ends of the handwriting
 * engine — normalizing ink the user just wrote, and emitting ink the AI is about to write — run
 * through identical arithmetic. When the two ends disagree by even a rounding convention, stored
 * glyphs come back subtly wider or shorter than they were drawn.
 */
object StrokeGeometry {

    /**
     * A 2x3 affine map, laid out row-major: `x' = scaleX·x + shearX·y + translateX`.
     *
     * Immutable and composed with [then] rather than mutated in place, because a glyph's transform
     * is built once per placement and then used from two passes — measuring and emitting — which
     * must see exactly the same matrix or the reserved box will not match the ink.
     */
    data class Affine(
        val scaleX: Float,
        val shearX: Float,
        val translateX: Float,
        val shearY: Float,
        val scaleY: Float,
        val translateY: Float,
    ) {
        fun mapX(x: Float, y: Float): Float = scaleX * x + shearX * y + translateX

        fun mapY(x: Float, y: Float): Float = shearY * x + scaleY * y + translateY

        fun map(point: StrokePoint): StrokePoint = point.copy(
            x = mapX(point.x, point.y),
            y = mapY(point.x, point.y),
        )

        /** This transform applied first, [next] second. */
        fun then(next: Affine): Affine = Affine(
            scaleX = next.scaleX * scaleX + next.shearX * shearY,
            shearX = next.scaleX * shearX + next.shearX * scaleY,
            translateX = next.scaleX * translateX + next.shearX * translateY + next.translateX,
            shearY = next.shearY * scaleX + next.scaleY * shearY,
            scaleY = next.shearY * shearX + next.scaleY * scaleY,
            translateY = next.shearY * translateX + next.scaleY * translateY + next.translateY,
        )

        companion object {
            val IDENTITY = Affine(1f, 0f, 0f, 0f, 1f, 0f)

            fun translation(dx: Float, dy: Float): Affine = Affine(1f, 0f, dx, 0f, 1f, dy)

            fun scale(sx: Float, sy: Float): Affine = Affine(sx, 0f, 0f, 0f, sy, 0f)

            fun rotation(degrees: Float): Affine {
                if (degrees == 0f) return IDENTITY
                val radians = degrees * DEGREES_TO_RADIANS
                val c = cos(radians)
                val s = sin(radians)
                return Affine(c, -s, 0f, s, c, 0f)
            }

            /** Rotation about ([cx], [cy]) — the glyph's own centre, so jitter spins it in place. */
            fun rotationAbout(cx: Float, cy: Float, degrees: Float): Affine {
                if (degrees == 0f) return IDENTITY
                return translation(-cx, -cy).then(rotation(degrees)).then(translation(cx, cy))
            }

            /**
             * Horizontal shear about `y = 0`, leaning everything above the baseline forward by
             * [degrees].
             *
             * A rotation would tip the baseline itself and turn a line of text into a slope; a shear
             * leaves every baseline horizontal and only leans the strokes, which is what an italic
             * hand actually does.
             */
            fun slant(degrees: Float): Affine {
                if (degrees == 0f) return IDENTITY
                return Affine(1f, -tan(degrees * DEGREES_TO_RADIANS), 0f, 0f, 1f, 0f)
            }
        }
    }

    fun distance(a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    fun polylineLength(points: List<StrokePoint>): Float {
        var total = 0f
        for (i in 1 until points.size) total += distance(points[i - 1], points[i])
        return total
    }

    /**
     * Drops points closer than [minSpacing] to the previously kept one.
     *
     * Digitizers repeat the same coordinate while the pen rests, and a run of coincident points
     * makes every downstream step lie: arc length reads short, tangents come out as zero vectors,
     * and the spline picks up a kink where the pen merely paused.
     */
    fun dropDuplicates(points: List<StrokePoint>, minSpacing: Float): List<StrokePoint> {
        if (points.size < 2) return points
        val threshold = max(minSpacing, 0f)
        val out = ArrayList<StrokePoint>(points.size)
        out += points[0]
        for (i in 1 until points.size) {
            if (distance(out[out.lastIndex], points[i]) > threshold) out += points[i]
        }
        return out
    }

    /**
     * Rewrites [points] so consecutive points sit [spacing] apart along the path, keeping the true
     * final point.
     *
     * Raw capture spacing encodes pen *speed*, not shape: the same letter written quickly arrives as
     * a handful of far-apart points and slowly as a dense crowd. Arc-length resampling removes that
     * difference so two recordings of one glyph are comparable, and so playback has an even point
     * density instead of clumps that read as pressure changes.
     */
    fun resample(points: List<StrokePoint>, spacing: Float): List<StrokePoint> {
        if (points.size < 2 || spacing <= 0f || !spacing.isFinite()) return points
        val out = ArrayList<StrokePoint>((polylineLength(points) / spacing).toInt().coerceIn(2, MAX_RESAMPLE_POINTS))
        out += points[0]
        var cursor = points[0]
        var walked = 0f
        var i = 1
        while (i < points.size) {
            val target = points[i]
            val segment = distance(cursor, target)
            if (walked + segment >= spacing && segment > 0f) {
                val t = (spacing - walked) / segment
                cursor = lerp(cursor, target, t)
                out += cursor
                walked = 0f
                if (out.size >= MAX_RESAMPLE_POINTS) break
            } else {
                walked += segment
                cursor = target
                i++
            }
        }
        val tail = points[points.size - 1]
        if (out.size < 2) {
            out += tail
        } else if (distance(out[out.lastIndex], tail) > spacing * TAIL_MERGE_FRACTION) {
            out += tail
        } else {
            // Snap rather than append: the endpoint matters (it is the cursive join origin) but a
            // stub segment a fraction of the spacing long would show up as a blob under a round cap.
            out[out.lastIndex] = tail
        }
        return out
    }

    /**
     * Densifies [points] along the Catmull-Rom spline through them, [samplesPerSpan] new points per
     * original span.
     *
     * Evaluates the exact curve the renderer draws: `StrokeSmoothing` emits each span as a cubic
     * Bézier with controls offset by `(p2 - p0)·tension / 6`, and this samples that same Bézier. Any
     * other spline formulation here would store a glyph that differs from the one the user watched
     * themselves draw.
     */
    fun catmullRom(
        points: List<StrokePoint>,
        samplesPerSpan: Int,
        tension: Float = DEFAULT_TENSION,
    ): List<StrokePoint> {
        if (points.size < 3 || samplesPerSpan < 1 || tension <= 0f) return points
        val last = points.size - 1
        val out = ArrayList<StrokePoint>(points.size + last * samplesPerSpan)
        out += points[0]
        for (i in 0 until last) {
            val p0 = points[if (i == 0) 0 else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 > last) last else i + 2]
            val c1x = p1.x + (p2.x - p0.x) * tension / 6f
            val c1y = p1.y + (p2.y - p0.y) * tension / 6f
            val c2x = p2.x - (p3.x - p1.x) * tension / 6f
            val c2y = p2.y - (p3.y - p1.y) * tension / 6f
            for (s in 1..samplesPerSpan) {
                val t = s.toFloat() / samplesPerSpan
                out += StrokePoint(
                    x = cubic(p1.x, c1x, c2x, p2.x, t),
                    y = cubic(p1.y, c1y, c2y, p2.y, t),
                    pressure = p1.pressure + (p2.pressure - p1.pressure) * t,
                    timestamp = p1.timestamp + ((p2.timestamp - p1.timestamp) * t).toLong(),
                )
            }
        }
        return out
    }

    /**
     * Samples a cubic Bézier into [samples] segments, endpoints included.
     *
     * Cursive joins are built as Béziers rather than as an interpolating spline because a Bézier
     * stays inside the convex hull of its control points. That containment is what lets the layout
     * pass reserve space for a join from four points, without evaluating the curve twice.
     */
    fun cubicBezier(
        start: StrokePoint,
        control1: StrokePoint,
        control2: StrokePoint,
        end: StrokePoint,
        samples: Int,
    ): List<StrokePoint> {
        val steps = samples.coerceIn(1, MAX_BEZIER_SAMPLES)
        val out = ArrayList<StrokePoint>(steps + 1)
        out += start
        for (s in 1..steps) {
            val t = s.toFloat() / steps
            out += StrokePoint(
                x = cubic(start.x, control1.x, control2.x, end.x, t),
                y = cubic(start.y, control1.y, control2.y, end.y, t),
                pressure = start.pressure + (end.pressure - start.pressure) * t,
                timestamp = start.timestamp + ((end.timestamp - start.timestamp) * t).toLong(),
            )
        }
        return out
    }

    /** Tight box around every point of every stroke; [BoundingBox.EMPTY] when there is no ink. */
    fun boundsOfStrokes(strokes: List<List<StrokePoint>>): BoundingBox {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (stroke in strokes) {
            for (point in stroke) {
                if (point.x < left) left = point.x
                if (point.x > right) right = point.x
                if (point.y < top) top = point.y
                if (point.y > bottom) bottom = point.y
            }
        }
        if (left > right || top > bottom) return BoundingBox.EMPTY
        return BoundingBox(left, top, right, bottom)
    }

    private fun lerp(a: StrokePoint, b: StrokePoint, t: Float): StrokePoint = StrokePoint(
        x = a.x + (b.x - a.x) * t,
        y = a.y + (b.y - a.y) * t,
        pressure = a.pressure + (b.pressure - a.pressure) * t,
        timestamp = a.timestamp + ((b.timestamp - a.timestamp) * t).toLong(),
    )

    private fun cubic(p0: Float, c1: Float, c2: Float, p1: Float, t: Float): Float {
        val u = 1f - t
        return u * u * u * p0 + 3f * u * u * t * c1 + 3f * u * t * t * c2 + t * t * t * p1
    }

    /** Matches `SmoothingMode.MEDIUM`, the tension the canvas renders user ink with by default. */
    const val DEFAULT_TENSION: Float = 0.8f

    /** A final segment shorter than this fraction of the spacing is snapped onto instead of added. */
    private const val TAIL_MERGE_FRACTION: Float = 0.35f

    /** Ceilings that keep a pathological input (spacing far below the ink size) bounded. */
    private const val MAX_RESAMPLE_POINTS: Int = 4096
    private const val MAX_BEZIER_SAMPLES: Int = 256
}

/** File-level so the nested [StrokeGeometry.Affine] factories can reach it. */
private val DEGREES_TO_RADIANS: Float = (PI / 180.0).toFloat()
