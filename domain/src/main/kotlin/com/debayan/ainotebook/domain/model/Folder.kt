package com.debayan.ainotebook.domain.model

/**
 * A folder used to organise notebooks. Nested folders are a future capability; for now a folder
 * is a flat grouping identified by [id].
 */
data class Folder(
    val id: String,
    val name: String,
    val color: Long = 0L,
    val createdAt: Long,
)
