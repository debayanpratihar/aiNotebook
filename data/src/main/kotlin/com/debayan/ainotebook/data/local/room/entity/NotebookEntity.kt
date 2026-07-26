package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notebooks",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("title"),
        Index("updatedAt"),
        Index("isFavorite"),
        Index("folderId"),
        Index("templateId"),
    ],
)
data class NotebookEntity(
    @PrimaryKey val notebookId: String,
    val title: String,
    val description: String? = null,
    val coverThumbnail: String? = null,
    val folderId: String? = null,
    val templateId: String? = null,
    val color: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val pageCount: Int = 0,
)
