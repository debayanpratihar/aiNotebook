package com.debayan.ainotebook.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import com.debayan.ainotebook.domain.model.canvas.Stroke
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders notebook pages to a multi-page PDF, drawing strokes as vector paths onto each page's
 * canvas (so text/lines stay crisp). Each page is sized to its content, scaled down if it exceeds a
 * maximum dimension.
 */
class PdfExporter @Inject constructor() {

    fun export(pages: List<List<Stroke>>, destination: File) {
        val document = PdfDocument()
        try {
            val effectivePages = pages.ifEmpty { listOf(emptyList()) }
            effectivePages.forEachIndexed { index, strokes ->
                val layout = layoutFor(strokes)
                val pageInfo = PdfDocument.PageInfo.Builder(layout.width, layout.height, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                paintStrokes(page.canvas, strokes, layout)
                document.finishPage(page)
            }
            destination.parentFile?.mkdirs()
            BufferedOutputStream(FileOutputStream(destination)).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun layoutFor(strokes: List<Stroke>): Layout {
        if (strokes.isEmpty()) return Layout(DEFAULT_WIDTH, DEFAULT_HEIGHT, 1f, PADDING.toFloat(), PADDING.toFloat())

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
        val scale = min(1f, MAX_DIMENSION / max(contentWidth, contentHeight))
        val width = (contentWidth * scale).roundToInt() + PADDING * 2
        val height = (contentHeight * scale).roundToInt() + PADDING * 2
        return Layout(width, height, scale, PADDING - left * scale, PADDING - top * scale)
    }

    private fun paintStrokes(canvas: Canvas, strokes: List<Stroke>, layout: Layout) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            paint.color = stroke.color.toInt()
            paint.strokeWidth = max(stroke.width * layout.scale, MIN_STROKE_WIDTH)
            path.rewind()
            val first = stroke.points.first()
            path.moveTo(first.x * layout.scale + layout.dx, first.y * layout.scale + layout.dy)
            for (index in 1 until stroke.points.size) {
                val point = stroke.points[index]
                path.lineTo(point.x * layout.scale + layout.dx, point.y * layout.scale + layout.dy)
            }
            canvas.drawPath(path, paint)
        }
    }

    private data class Layout(val width: Int, val height: Int, val scale: Float, val dx: Float, val dy: Float)

    private companion object {
        const val DEFAULT_WIDTH = 1240 // A4 @ ~150dpi
        const val DEFAULT_HEIGHT = 1754
        const val MAX_DIMENSION = 2000f
        const val PADDING = 24
        const val MIN_STROKE_WIDTH = 0.5f
    }
}
