package com.debayan.ainotebook.feature.canvas.render

import androidx.compose.ui.geometry.Offset

/**
 * Maps between infinite **world** coordinates and on-screen pixels for the canvas viewport.
 *
 * `screen = world * scale + offset`. [scale] is the zoom factor, clamped to the spec's 10%–1000%
 * range; [offset] is the screen-space translation of the world origin.
 */
data class Camera(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
) {
    fun screenToWorld(screen: Offset): Offset =
        Offset((screen.x - offset.x) / scale, (screen.y - offset.y) / scale)

    fun worldToScreen(world: Offset): Offset =
        Offset(world.x * scale + offset.x, world.y * scale + offset.y)

    /**
     * Applies a pinch/pan gesture: zooms by [zoom] about [centroid] (keeping the world point under
     * the centroid stationary), then translates by [pan]. Zoom is clamped to [MIN_SCALE]..[MAX_SCALE].
     */
    fun transformed(centroid: Offset, pan: Offset, zoom: Float): Camera {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val worldAtCentroid = screenToWorld(centroid)
        val rescaledOffset = Offset(
            x = centroid.x - worldAtCentroid.x * newScale,
            y = centroid.y - worldAtCentroid.y * newScale,
        )
        return Camera(newScale, rescaledOffset + pan)
    }

    val zoomPercent: Int get() = (scale * 100f).toInt()

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 10f
    }
}
