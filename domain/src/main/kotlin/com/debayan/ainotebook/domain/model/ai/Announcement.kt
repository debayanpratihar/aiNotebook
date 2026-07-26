package com.debayan.ainotebook.domain.model.ai

/** A message from the remote configuration (announcements.json), shown in the model manager. */
data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val publishedAt: Long,
)

/** A release-notes entry from the remote configuration (changelog.json). */
data class ChangelogEntry(
    val versionName: String,
    val versionCode: Int,
    val notes: List<String>,
    val releasedAt: Long,
)
