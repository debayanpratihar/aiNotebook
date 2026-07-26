package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.Announcement
import com.debayan.ainotebook.domain.model.ai.ChangelogEntry
import com.debayan.ainotebook.domain.model.ai.ModelCatalog

/**
 * Reads the remote configuration hosted on GitHub Pages. This is the only network access the app
 * performs besides model downloads; it never uploads anything.
 */
interface ConfigRepository {

    suspend fun getCatalog(): AppResult<ModelCatalog>

    suspend fun getAnnouncements(): AppResult<List<Announcement>>

    suspend fun getChangelog(): AppResult<List<ChangelogEntry>>
}
