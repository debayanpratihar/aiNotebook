package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.canvas.Layer
import com.debayan.ainotebook.domain.model.canvas.Page
import kotlinx.coroutines.flow.Flow

/** Repository contract for pages. Implemented transactionally over Room in the data layer. */
interface PageRepository {

    fun observePages(notebookId: String): Flow<List<Page>>

    suspend fun getPage(pageId: String): Page?

    suspend fun getFirstPage(notebookId: String): Page?

    /**
     * Atomically creates [page] together with its [defaultLayer] and increments the owning
     * notebook's page count / touch timestamp. All three writes succeed or fail together.
     */
    suspend fun createPage(page: Page, defaultLayer: Layer): AppResult<Unit>

    suspend fun deletePage(pageId: String): AppResult<Unit>
}
