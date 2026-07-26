package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["notebookId"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("notebookId")],
)
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val notebookId: String,
    val type: String,
    val filePath: String,
    val importedAt: Long,
)
