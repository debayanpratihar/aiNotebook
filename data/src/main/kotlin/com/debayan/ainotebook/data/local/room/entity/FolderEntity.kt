package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val name: String,
    val color: Long = 0L,
    val createdAt: Long,
)
