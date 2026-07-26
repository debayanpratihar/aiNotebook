package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Searchable OCR/text content per page. A conventional table for Phase 1; a full-text (FTS4)
 * variant is planned for the OCR phase when search moves beyond simple lookups.
 */
@Entity(
    tableName = "search_index",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["notebookId"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["pageId"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("notebookId"),
        Index("pageId"),
    ],
)
data class SearchIndexEntity(
    @PrimaryKey val indexId: String,
    val notebookId: String,
    val pageId: String,
    val recognizedText: String,
    val lastIndexedAt: Long,
)
