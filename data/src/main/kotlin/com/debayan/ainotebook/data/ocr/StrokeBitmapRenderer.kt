package com.debayan.ainotebook.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.debayan.ainotebook.domain.model.canvas.Stroke
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Rasterizes a page's strokes onto a white [Bitmap] for OCR. This is a lightweight renderer (plain
 * polylines, no smoothing) — OCR does not need the on-screen fidelity of the canvas engine. The
 * output is padded and scaled to a bounded size to keep recognition effective and memory sane.
 */
class StrokeBitmapRenderer @Inject constructor() {

    fun render(strokes: List<Stroke>): Bitmap? {
        if (strokes.isEmpty()) return null

        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (stroke in strokes) {
            val box = stroke.boundingBox
            if (box.left < left) left = box.left
            if (box.top < top) top = box.top
            if (box.right > right) right = box.right
            if (box.bottom > bottom) bottom = box.bottom
        }

        val contentWidth = max(right - left, 1f)
        val contentHeight = max(bottom - top, 1f)
        val scale = (MAX_DIMENSION / max(contentWidth, contentHeight)).coerceIn(MIN_SCALE, MAX_SCALE)

        val width = (contentWidth * scale).roundToInt().coerceIn(1, MAX_DIMENSION) + PADDING * 2
        val height = (contentHeight * scale).roundToInt().coerceIn(1, MAX_DIMENSION) + PADDING * 2

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate(PADDING.toFloat(), PADDING.toFloat())
        canvas.scale(scale, scale)
        canvas.translate(-left, -top)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            paint.color = stroke.color.toInt()
            paint.strokeWidth = max(stroke.width, MIN_STROKE_WIDTH)
            path.rewind()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (index in 1 until stroke.points.size) {
                val point = stroke.points[index]
                path.lineTo(point.x, point.y)
            }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    private companion object {
        const val MAX_DIMENSION = 2048
        const val PADDING = 24
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 2f
        const val MIN_STROKE_WIDTH = 1f
    }
}
