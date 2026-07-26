package com.debayan.ainotebook.feature.canvas.engine

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.StrokePoint

/**
 * Accumulates the points of an in-progress stroke in world coordinates, discarding samples that are
 * closer than [minWorldDistance] to the previous one (the spec's "discard duplicate points"). On
 * completion it produces an immutable domain [Stroke].
 *
 * Not thread-safe: intended to be driven from a single input/render loop.
 */
class StrokeBuilder(
    private val minWorldDistance: Float = DEFAULT_MIN_DISTANCE,
) {
    private val points = mutableListOf<StrokePoint>()

    val isEmpty: Boolean get() = points.isEmpty()

    fun start(point: StrokePoint) {
        points.clear()
        points.add(point)
    }

    /** Adds a point unless it is within the dedup radius of the last one. Returns true if accepted. */
    fun add(point: StrokePoint): Boolean {
        val last = points.lastOrNull()
        if (last != null) {
            val dx = point.x - last.x
            val dy = point.y - last.y
            if (dx * dx + dy * dy < minWorldDistance * minWorldDistance) return false
        }
        points.add(point)
        return true
    }

    fun currentPoints(): List<StrokePoint> = points.toList()

    fun build(id: String, layerId: String, brush: BrushSettings, createdAt: Long): Stroke? {
        if (points.isEmpty()) return null
        val snapshot = points.toList()
        return Stroke(
            id = id,
            layerId = layerId,
            tool = brush.tool,
            color = brush.color,
            width = brush.width,
            opacity = brush.effectiveOpacity,
            points = snapshot,
            boundingBox = BoundingBox.fromPoints(snapshot),
            createdAt = createdAt,
        )
    }

    fun clear() = points.clear()

    companion object {
        const val DEFAULT_MIN_DISTANCE: Float = 1.2f
    }
}
