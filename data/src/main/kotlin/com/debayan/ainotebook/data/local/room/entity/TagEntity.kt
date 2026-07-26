package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val tagId: String,
    val name: String,
    val color: Long = 0L,
)
