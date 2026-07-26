package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI-generated content attached to a page. Stored separately from user strokes so it can be shown,
 * edited, or removed independently and can participate in undo/redo.
 */
@Entity(
    tableName = "ai_annotations",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["pageId"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pageId")],
)
data class AiAnnotationEntity(
    @PrimaryKey val annotationId: String,
    val pageId: String,
    val promptSummary: String? = null,
    val modelName: String? = null,
    val generatedAt: Long,
    @Embedded(prefix = "region_") val region: BoundingBoxEmbedded = BoundingBoxEmbedded(),
    val editable: Boolean = true,
)
