package com.debayan.ainotebook.domain.model

/**
 * A notebook: the top-level container the user creates, organises, and draws in.
 *
 * This is the layer-independent representation. The data layer maps it to/from its Room entity;
 * the presentation layer renders it. Timestamps are epoch milliseconds; [color] is a packed ARGB
 * value stored as a [Long] to avoid `Int` sign issues.
 */
data class Notebook(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverThumbnailPath: String? = null,
    val folderId: String? = null,
    val templateId: String? = null,
    val color: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val pageCount: Int = 0,
)
