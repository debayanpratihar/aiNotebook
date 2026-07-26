package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction row implementing the many-to-many relationship between notebooks and tags.
 * The composite (notebookId, tagId) is the primary key.
 */
@Entity(
    tableName = "notebook_tags",
    primaryKeys = ["notebookId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["notebookId"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("notebookId"), Index("tagId")],
)
data class NotebookTagCrossRef(
    val notebookId: String,
    val tagId: String,
)
