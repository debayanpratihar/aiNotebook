package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["notebookId"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("notebookId"),
        Index(value = ["notebookId", "pageNumber"]),
        Index("templateId"),
    ],
)
data class PageEntity(
    @PrimaryKey val pageId: String,
    val notebookId: String,
    val pageNumber: Int,
    val templateId: String? = null,
    val zoomLevel: Float = 1f,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val createdAt: Long,
    val updatedAt: Long,
)
