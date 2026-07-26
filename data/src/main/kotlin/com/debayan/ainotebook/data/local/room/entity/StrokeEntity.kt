package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "strokes",
    foreignKeys = [
        ForeignKey(
            entity = LayerEntity::class,
            parentColumns = ["layerId"],
            childColumns = ["layerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("layerId")],
)
data class StrokeEntity(
    @PrimaryKey val strokeId: String,
    val layerId: String,
    val toolType: String,
    val color: Long,
    val width: Float,
    val opacity: Float = 1f,
    @Embedded(prefix = "bbox_") val boundingBox: BoundingBoxEmbedded = BoundingBoxEmbedded(),
    val createdAt: Long,
)
