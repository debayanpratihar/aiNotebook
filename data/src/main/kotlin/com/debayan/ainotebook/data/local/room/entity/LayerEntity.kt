package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["pageId"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("pageId"),
        Index(value = ["pageId", "orderIndex"]),
    ],
)
data class LayerEntity(
    @PrimaryKey val layerId: String,
    val pageId: String,
    val name: String,
    val orderIndex: Int,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
)
