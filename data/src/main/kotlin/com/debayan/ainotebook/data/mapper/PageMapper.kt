package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.PageEntity
import com.debayan.ainotebook.domain.model.canvas.Page

fun PageEntity.toDomain(): Page = Page(
    id = pageId,
    notebookId = notebookId,
    pageNumber = pageNumber,
    templateId = templateId,
    zoomLevel = zoomLevel,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Page.toEntity(): PageEntity = PageEntity(
    pageId = id,
    notebookId = notebookId,
    pageNumber = pageNumber,
    templateId = templateId,
    zoomLevel = zoomLevel,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
