package com.debayan.ainotebook.feature.canvas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.feature.canvas.engine.BrushSettings
import com.debayan.ainotebook.feature.canvas.engine.CanvasToolMode
import com.debayan.ainotebook.feature.canvas.engine.StrokeSmoothing
import com.debayan.ainotebook.feature.canvas.render.Camera
import com.debayan.ainotebook.feature.canvas.render.CanvasTemplate
import com.debayan.ainotebook.feature.canvas.render.drawStroke
import com.debayan.ainotebook.feature.canvas.render.drawStrokePath
import com.debayan.ainotebook.feature.canvas.render.drawTemplate

private const val TEMPLATE_SPACING = 48f
private const val MIN_WORLD_DISTANCE = 1.2f
private const val MIN_WORLD_DISTANCE_SQ = MIN_WORLD_DISTANCE * MIN_WORLD_DISTANCE

/**
 * The drawing surface. Renders committed [strokes] plus the in-progress stroke as crisp vectors
 * under a GPU camera transform (no permanent rasterization).
 *
 * Gestures: one pointer draws (or erases); two or more pointers pan and pinch-zoom. If a second
 * pointer lands mid-stroke the in-progress stroke is discarded and the gesture becomes a transform,
 * so pinching never leaves a stray mark.
 *
 * Note: this is a straightforward full-viewport vector redraw. Tile-based dirty rendering is the
 * Phase 4 performance optimization.
 */
@Composable
fun InfiniteCanvas(
    strokes: List<Stroke>,
    toolMode: CanvasToolMode,
    brush: BrushSettings,
    template: CanvasTemplate,
    templateColor: Color,
    onStrokeCompleted: (List<StrokePoint>) -> Unit,
    onEraseAt: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    onZoomChanged: (Float) -> Unit = {},
) {
    var camera by remember { mutableStateOf(Camera()) }
    val activePoints = remember { mutableStateListOf<StrokePoint>() }

    // Latest callbacks, so the long-lived gesture coroutine never calls a stale lambda.
    val currentOnStrokeCompleted by rememberUpdatedState(onStrokeCompleted)
    val currentOnEraseAt by rememberUpdatedState(onEraseAt)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    // Pre-build smoothed world paths for committed strokes; recompute only when they change.
    val committedPaths = remember(strokes, brush.smoothing) {
        strokes.associate { it.id to StrokeSmoothing.buildPath(it.points, brush.smoothing) }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(toolMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var transforming = false
                    activePoints.clear()

                    if (toolMode == CanvasToolMode.DRAW) {
                        val world = camera.screenToWorld(down.position)
                        activePoints.add(StrokePoint(world.x, world.y, down.pressure, down.uptimeMillis))
                    } else {
                        val world = camera.screenToWorld(down.position)
                        currentOnEraseAt(world.x, world.y)
                    }
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount == 0) break

                        if (pressedCount >= 2) {
                            if (!transforming) {
                                transforming = true
                                activePoints.clear() // discard any nascent stroke
                            }
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            if (zoom != 1f || pan != Offset.Zero) {
                                camera = camera.transformed(centroid, pan, zoom)
                                currentOnZoomChanged(camera.scale)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (!transforming) {
                            val change = event.changes.firstOrNull { it.pressed }
                            if (change != null) {
                                val world = camera.screenToWorld(change.position)
                                if (toolMode == CanvasToolMode.DRAW) {
                                    activePoints.addDeduped(world, change.pressure, change.uptimeMillis)
                                } else {
                                    currentOnEraseAt(world.x, world.y)
                                }
                                change.consume()
                            }
                        }
                    }

                    if (!transforming && toolMode == CanvasToolMode.DRAW && activePoints.isNotEmpty()) {
                        currentOnStrokeCompleted(activePoints.toList())
                    }
                    activePoints.clear()
                }
            },
    ) {
        drawTemplate(template, camera, TEMPLATE_SPACING, templateColor)

        withTransform({
            translate(camera.offset.x, camera.offset.y)
            scale(camera.scale, camera.scale, pivot = Offset.Zero)
        }) {
            strokes.forEach { stroke ->
                committedPaths[stroke.id]?.let { path -> drawStroke(stroke, path) }
            }
            if (activePoints.isNotEmpty()) {
                val activePath = StrokeSmoothing.buildPath(activePoints, brush.smoothing)
                drawStrokePath(
                    path = activePath,
                    colorArgb = brush.color,
                    width = brush.width,
                    opacity = brush.effectiveOpacity,
                    highlighter = brush.isHighlighter,
                )
            }
        }
    }
}

/** Appends a point unless it is within the dedup radius of the previous one (world space). */
private fun SnapshotStateList<StrokePoint>.addDeduped(world: Offset, pressure: Float, time: Long) {
    val last = lastOrNull()
    if (last != null) {
        val dx = world.x - last.x
        val dy = world.y - last.y
        if (dx * dx + dy * dy < MIN_WORLD_DISTANCE_SQ) return
    }
    add(StrokePoint(world.x, world.y, pressure, time))
}
