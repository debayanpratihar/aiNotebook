package com.debayan.ainotebook.feature.canvas.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.ToolType
import kotlin.math.floor

/**
 * Draws a pre-built world-space [path] with the given ink attributes. Intended to be called inside a
 * `withTransform` block that has already applied the camera (so [width] is in world units).
 * Highlighter strokes use a multiply blend so underlying content stays visible.
 */
fun DrawScope.drawStrokePath(
    path: Path,
    colorArgb: Long,
    width: Float,
    opacity: Float,
    highlighter: Boolean,
) {
    val base = Color(colorArgb.toInt())
    val ink = base.copy(alpha = base.alpha * opacity.coerceIn(0f, 1f))
    drawPath(
        path = path,
        color = ink,
        style = StrokeStyle(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (highlighter) BlendMode.Multiply else BlendMode.SrcOver,
    )
}

fun DrawScope.drawStroke(stroke: Stroke, path: Path) = drawStrokePath(
    path = path,
    colorArgb = stroke.color,
    width = stroke.width,
    opacity = stroke.opacity,
    highlighter = stroke.tool == ToolType.HIGHLIGHTER,
)

/**
 * Draws the page background template in **screen** space (so lines stay a constant 1px regardless of
 * zoom). Only the currently visible grid lines are emitted.
 */
fun DrawScope.drawTemplate(
    template: CanvasTemplate,
    camera: Camera,
    spacingWorld: Float,
    color: Color,
) {
    if (template == CanvasTemplate.BLANK) return

    val topLeftWorld = camera.screenToWorld(Offset.Zero)
    val bottomRightWorld = camera.screenToWorld(Offset(size.width, size.height))
    val startX = floor(topLeftWorld.x / spacingWorld) * spacingWorld
    val startY = floor(topLeftWorld.y / spacingWorld) * spacingWorld

    if (template == CanvasTemplate.DOT) {
        var wx = startX
        while (wx <= bottomRightWorld.x) {
            var wy = startY
            while (wy <= bottomRightWorld.y) {
                drawCircle(color = color, radius = 2f, center = camera.worldToScreen(Offset(wx, wy)))
                wy += spacingWorld
            }
            wx += spacingWorld
        }
        return
    }

    // RULED and GRID both draw horizontal rules; GRID adds verticals.
    var wy = startY
    while (wy <= bottomRightWorld.y) {
        val sy = camera.worldToScreen(Offset(0f, wy)).y
        drawLine(color, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
        wy += spacingWorld
    }
    if (template == CanvasTemplate.GRID) {
        var wx = startX
        while (wx <= bottomRightWorld.x) {
            val sx = camera.worldToScreen(Offset(wx, 0f)).x
            drawLine(color, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1f)
            wx += spacingWorld
        }
    }
}
