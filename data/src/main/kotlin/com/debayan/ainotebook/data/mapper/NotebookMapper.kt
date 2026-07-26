package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.NotebookEntity
import com.debayan.ainotebook.domain.model.Notebook

/** Maps between the [NotebookEntity] persistence row and the [Notebook] domain model. */

fun NotebookEntity.toDomain(): Notebook = Notebook(
    id = notebookId,
    title = title,
    description = description,
    coverThumbnailPath = coverThumbnail,
    folderId = folderId,
    templateId = templateId,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFavorite = isFavorite,
    isArchived = isArchived,
    pageCount = pageCount,
)

fun Notebook.toEntity(): NotebookEntity = NotebookEntity(
    notebookId = id,
    title = title,
    description = description,
    coverThumbnail = coverThumbnailPath,
    folderId = folderId,
    templateId = templateId,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFavorite = isFavorite,
    isArchived = isArchived,
    pageCount = pageCount,
)
