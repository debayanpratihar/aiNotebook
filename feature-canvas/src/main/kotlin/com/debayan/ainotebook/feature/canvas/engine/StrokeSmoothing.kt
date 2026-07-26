package com.debayan.ainotebook.feature.canvas.engine

import androidx.compose.ui.graphics.Path
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import com.debayan.ainotebook.domain.model.canvas.StrokePoint

/**
 * Converts a stroke's sampled points into a smooth vector [Path] in world coordinates using a
 * Catmull-Rom spline expressed as cubic Béziers. The spline passes through every original point, so
 * jitter is reduced without altering the writer's path — matching the smoothing spec.
 *
 * [SmoothingMode.OFF] produces straight segments; higher modes increase the tangent influence. The
 * factors stay <= 1 to avoid overshoot artifacts.
 */
object StrokeSmoothing {

    fun buildPath(points: List<StrokePoint>, smoothing: SmoothingMode): Path {
        val path = Path()
        when {
            points.isEmpty() -> return path
            points.size == 1 -> {
                // A single tap: emit a minimal segment so a round-capped stroke renders as a dot.
                val only = points[0]
                path.moveTo(only.x, only.y)
                path.lineTo(only.x + DOT_EPSILON, only.y)
                return path
            }
        }

        path.moveTo(points[0].x, points[0].y)
        val factor = tensionFactor(smoothing)
        if (factor == 0f || points.size == 2) {
            for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
            return path
        }

        val last = points.size - 1
        for (i in 0 until last) {
            val p0 = points[if (i == 0) 0 else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 > last) last else i + 2]

            val c1x = p1.x + (p2.x - p0.x) * factor / 6f
            val c1y = p1.y + (p2.y - p0.y) * factor / 6f
            val c2x = p2.x - (p3.x - p1.x) * factor / 6f
            val c2y = p2.y - (p3.y - p1.y) * factor / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }
        return path
    }

    private fun tensionFactor(smoothing: SmoothingMode): Float = when (smoothing) {
        SmoothingMode.OFF -> 0f
        SmoothingMode.LOW -> 0.5f
        SmoothingMode.MEDIUM -> 0.8f
        SmoothingMode.HIGH -> 1.0f
        SmoothingMode.ADAPTIVE -> 0.8f
    }

    private const val DOT_EPSILON = 0.01f
}
