package com.debayan.ainotebook.domain.model.canvas

/**
 * Axis-aligned bounding rectangle of a stroke in world coordinates, used for hit-testing, tile
 * culling, and erase operations without walking every point.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    fun intersects(other: BoundingBox): Boolean =
        left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

    companion object {
        val EMPTY = BoundingBox(0f, 0f, 0f, 0f)

        /** Computes the tight bounding box enclosing [points]; [EMPTY] when the list is empty. */
        fun fromPoints(points: List<StrokePoint>): BoundingBox {
            if (points.isEmpty()) return EMPTY
            var left = Float.MAX_VALUE
            var top = Float.MAX_VALUE
            var right = -Float.MAX_VALUE
            var bottom = -Float.MAX_VALUE
            for (point in points) {
                if (point.x < left) left = point.x
                if (point.x > right) right = point.x
                if (point.y < top) top = point.y
                if (point.y > bottom) bottom = point.y
            }
            return BoundingBox(left, top, right, bottom)
        }
    }
}
