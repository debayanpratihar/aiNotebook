package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.FolderEntity
import com.debayan.ainotebook.domain.model.Folder

/** Maps between the [FolderEntity] persistence row and the [Folder] domain model. */

fun FolderEntity.toDomain(): Folder = Folder(
    id = folderId,
    name = name,
    color = color,
    createdAt = createdAt,
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    folderId = id,
    name = name,
    color = color,
    createdAt = createdAt,
)
