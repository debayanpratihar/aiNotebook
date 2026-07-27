package com.debayan.ainotebook.data.export

import androidx.room.withTransaction
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
import com.debayan.ainotebook.data.local.room.dao.AiAnnotationDao
import com.debayan.ainotebook.data.local.room.dao.LayerDao
import com.debayan.ainotebook.data.local.room.dao.NotebookDao
import com.debayan.ainotebook.data.local.room.dao.PageDao
import com.debayan.ainotebook.data.local.room.dao.StrokeDao
import com.debayan.ainotebook.data.local.room.dao.StrokePointDao
import com.debayan.ainotebook.data.local.room.entity.AiAnnotationEntity
import com.debayan.ainotebook.data.local.room.entity.BoundingBoxEmbedded
import com.debayan.ainotebook.data.local.room.entity.LayerEntity
import com.debayan.ainotebook.data.local.room.entity.NotebookEntity
import com.debayan.ainotebook.data.local.room.entity.PageEntity
import com.debayan.ainotebook.data.local.room.entity.StrokeEntity
import com.debayan.ainotebook.data.local.room.entity.StrokePointEntity
import com.debayan.ainotebook.data.local.room.relation.StrokeWithPoints
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Reads and writes the lossless native `.ainb` package (a versioned ZIP containing `notebook.json`).
 *
 * Import is defensive: archive entries are checked for path traversal, the manifest version is
 * validated, and every entity is inserted under a **freshly generated id** inside one transaction —
 * so importing can never overwrite an existing notebook or leave a partial graph behind.
 */
class NativeNotebookPackager @Inject constructor(
    private val database: AiNotebookDatabase,
    private val notebookDao: NotebookDao,
    private val pageDao: PageDao,
    private val layerDao: LayerDao,
    private val strokeDao: StrokeDao,
    private val strokePointDao: StrokePointDao,
    private val aiAnnotationDao: AiAnnotationDao,
    private val json: Json,
    private val timeProvider: TimeProvider,
) {

    suspend fun export(notebookId: String, destination: File): File {
        val notebookEntity = notebookDao.getById(notebookId)
            ?: throw IllegalArgumentException("Notebook $notebookId not found")
        val pageEntities = pageDao.observeByNotebook(notebookId).first()
        val layerEntities = pageEntities.flatMap { layerDao.observeByPage(it.pageId).first() }
        val strokeRows = pageEntities.flatMap { strokeDao.observeStrokesWithPointsByPage(it.pageId).first() }
        val annotationEntities = pageEntities.flatMap { aiAnnotationDao.observeByPage(it.pageId).first() }

        val dto = NotebookPackageDto(
            manifest = PackageManifestDto(
                formatVersion = FORMAT_VERSION,
                schemaVersion = AiNotebookDatabase.VERSION,
                exportedAt = timeProvider.now(),
            ),
            notebook = notebookEntity.toDto(),
            pages = pageEntities.map { it.toDto() },
            layers = layerEntities.map { it.toDto() },
            strokes = strokeRows.map { it.toDto() },
            annotations = annotationEntities.map { it.toDto() },
        )

        destination.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
            zip.putNextEntry(ZipEntry(NOTEBOOK_ENTRY))
            zip.write(json.encodeToString(dto).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return destination
    }

    suspend fun import(source: File): String {
        require(source.exists()) { "Import file does not exist" }

        val packageDto = ZipFile(source).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                require(!name.contains("..") && !name.startsWith("/") && !name.contains(":")) {
                    "Illegal archive entry: $name"
                }
            }
            val entry = zip.getEntry(NOTEBOOK_ENTRY)
                ?: throw IllegalArgumentException("Package is missing $NOTEBOOK_ENTRY")
            val text = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString<NotebookPackageDto>(text)
        }

        validate(packageDto)
        return insert(packageDto)
    }

    private fun validate(dto: NotebookPackageDto) {
        require(dto.manifest.formatVersion in 1..FORMAT_VERSION) {
            "Unsupported package format version ${dto.manifest.formatVersion}"
        }
        require(dto.notebook.notebookId.isNotBlank()) { "Package is missing notebook data" }
    }

    private suspend fun insert(dto: NotebookPackageDto): String {
        val now = timeProvider.now()
        val newNotebookId = UUID.randomUUID().toString()
        val pageIdMap = dto.pages.associate { it.pageId to UUID.randomUUID().toString() }
        val layerIdMap = dto.layers.associate { it.layerId to UUID.randomUUID().toString() }
        val strokeIdMap = dto.strokes.associate { it.strokeId to UUID.randomUUID().toString() }

        val notebook = NotebookEntity(
            notebookId = newNotebookId,
            title = dto.notebook.title.ifBlank { "Imported notebook" },
            description = dto.notebook.description,
            folderId = null,
            templateId = null,
            color = dto.notebook.color,
            createdAt = dto.notebook.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            pageCount = dto.pages.size,
        )
        val pages = dto.pages.mapNotNull { page ->
            val id = pageIdMap[page.pageId] ?: return@mapNotNull null
            PageEntity(
                pageId = id,
                notebookId = newNotebookId,
                pageNumber = page.pageNumber,
                templateId = null,
                zoomLevel = page.zoomLevel,
                canvasWidth = page.canvasWidth,
                canvasHeight = page.canvasHeight,
                createdAt = page.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
            )
        }
        val layers = dto.layers.mapNotNull { layer ->
            val id = layerIdMap[layer.layerId] ?: return@mapNotNull null
            val pageId = pageIdMap[layer.pageId] ?: return@mapNotNull null
            LayerEntity(id, pageId, layer.name, layer.orderIndex, layer.visible, layer.locked, layer.opacity)
        }
        val strokes = dto.strokes.mapNotNull { stroke ->
            val id = strokeIdMap[stroke.strokeId] ?: return@mapNotNull null
            val layerId = layerIdMap[stroke.layerId] ?: return@mapNotNull null
            StrokeEntity(
                strokeId = id,
                layerId = layerId,
                toolType = stroke.toolType,
                color = stroke.color,
                width = stroke.width,
                opacity = stroke.opacity,
                boundingBox = BoundingBoxEmbedded(stroke.bboxLeft, stroke.bboxTop, stroke.bboxRight, stroke.bboxBottom),
                createdAt = stroke.createdAt.takeIf { it > 0 } ?: now,
            )
        }
        val points = dto.strokes.flatMap { stroke ->
            val strokeId = strokeIdMap[stroke.strokeId] ?: return@flatMap emptyList()
            stroke.points.map { point ->
                StrokePointEntity(
                    strokeId = strokeId,
                    sequenceNumber = point.sequenceNumber,
                    x = point.x,
                    y = point.y,
                    pressure = point.pressure,
                    timestamp = point.timestamp,
                )
            }
        }
        val annotations = dto.annotations.mapNotNull { annotation ->
            val pageId = pageIdMap[annotation.pageId] ?: return@mapNotNull null
            AiAnnotationEntity(
                annotationId = UUID.randomUUID().toString(),
                pageId = pageId,
                promptSummary = annotation.promptSummary,
                modelName = annotation.modelName,
                generatedAt = annotation.generatedAt.takeIf { it > 0 } ?: now,
                region = BoundingBoxEmbedded(
                    annotation.regionLeft,
                    annotation.regionTop,
                    annotation.regionRight,
                    annotation.regionBottom,
                ),
                editable = annotation.editable,
            )
        }

        database.withTransaction {
            notebookDao.upsert(notebook)
            pageDao.upsertAll(pages)
            layers.forEach { layerDao.upsert(it) }
            strokeDao.upsertAll(strokes)
            strokePointDao.insertAll(points)
            annotations.forEach { aiAnnotationDao.upsert(it) }
        }
        return newNotebookId
    }

    private fun NotebookEntity.toDto() = NotebookDto(
        notebookId = notebookId,
        title = title,
        description = description,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
        isArchived = isArchived,
        pageCount = pageCount,
    )

    private fun PageEntity.toDto() = PageDto(
        pageId = pageId,
        pageNumber = pageNumber,
        zoomLevel = zoomLevel,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun LayerEntity.toDto() = LayerDto(
        layerId = layerId,
        pageId = pageId,
        name = name,
        orderIndex = orderIndex,
        visible = visible,
        locked = locked,
        opacity = opacity,
    )

    private fun StrokeWithPoints.toDto() = StrokeDto(
        strokeId = stroke.strokeId,
        layerId = stroke.layerId,
        toolType = stroke.toolType,
        color = stroke.color,
        width = stroke.width,
        opacity = stroke.opacity,
        bboxLeft = stroke.boundingBox.left,
        bboxTop = stroke.boundingBox.top,
        bboxRight = stroke.boundingBox.right,
        bboxBottom = stroke.boundingBox.bottom,
        createdAt = stroke.createdAt,
        points = points.sortedBy { it.sequenceNumber }
            .map { PointDto(it.sequenceNumber, it.x, it.y, it.pressure, it.timestamp) },
    )

    private fun AiAnnotationEntity.toDto() = AnnotationDto(
        annotationId = annotationId,
        pageId = pageId,
        promptSummary = promptSummary,
        modelName = modelName,
        generatedAt = generatedAt,
        regionLeft = region.left,
        regionTop = region.top,
        regionRight = region.right,
        regionBottom = region.bottom,
        editable = editable,
    )

    companion object {
        const val FORMAT_VERSION = 1
        const val NOTEBOOK_ENTRY = "notebook.json"
        const val MIME_TYPE = "application/octet-stream"
        const val FILE_EXTENSION = "ainb"
    }
}
