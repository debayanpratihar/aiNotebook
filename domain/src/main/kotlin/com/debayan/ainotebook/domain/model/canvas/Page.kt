package com.debayan.ainotebook.domain.model.canvas

/**
 * A page within a notebook. The canvas is infinite, so [canvasWidth]/[canvasHeight] describe the
 * nominal template/export bounds in world units rather than a hard drawing limit. [zoomLevel] is the
 * last-used zoom, restored when the page is reopened.
 */
data class Page(
    val id: String,
    val notebookId: String,
    val pageNumber: Int,
    val templateId: String? = null,
    val zoomLevel: Float = 1f,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        /** Nominal page size in world units (portrait), used when a page is first created. */
        const val DEFAULT_WIDTH: Float = 1080f
        const val DEFAULT_HEIGHT: Float = 1920f
    }
}
