package com.debayan.ainotebook.data.export

import kotlinx.serialization.Serializable

/**
 * Serialized representation of a notebook for the native `.ainb` package. Vector content (pages,
 * layers, strokes, points, AI annotations) is preserved losslessly. Tags and attachments are added
 * to the package format alongside their features; the versioned manifest supports future migrations.
 */
@Serializable
data class NotebookPackageDto(
    val manifest: PackageManifestDto,
    val notebook: NotebookDto,
    val pages: List<PageDto> = emptyList(),
    val layers: List<LayerDto> = emptyList(),
    val strokes: List<StrokeDto> = emptyList(),
    val annotations: List<AnnotationDto> = emptyList(),
)

@Serializable
data class PackageManifestDto(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
)

@Serializable
data class NotebookDto(
    val notebookId: String,
    val title: String,
    val description: String? = null,
    val color: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val pageCount: Int = 0,
)

@Serializable
data class PageDto(
    val pageId: String,
    val pageNumber: Int,
    val zoomLevel: Float = 1f,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class LayerDto(
    val layerId: String,
    val pageId: String,
    val name: String,
    val orderIndex: Int,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
)

@Serializable
data class StrokeDto(
    val strokeId: String,
    val layerId: String,
    val toolType: String,
    val color: Long,
    val width: Float,
    val opacity: Float = 1f,
    val bboxLeft: Float = 0f,
    val bboxTop: Float = 0f,
    val bboxRight: Float = 0f,
    val bboxBottom: Float = 0f,
    val createdAt: Long = 0L,
    val points: List<PointDto> = emptyList(),
)

@Serializable
data class PointDto(
    val sequenceNumber: Int,
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = 0L,
)

@Serializable
data class AnnotationDto(
    val annotationId: String,
    val pageId: String,
    val promptSummary: String? = null,
    val modelName: String? = null,
    val generatedAt: Long = 0L,
    val regionLeft: Float = 0f,
    val regionTop: Float = 0f,
    val regionRight: Float = 0f,
    val regionBottom: Float = 0f,
    val editable: Boolean = true,
)
