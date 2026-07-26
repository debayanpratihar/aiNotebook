package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.search.SearchResult
import kotlinx.coroutines.flow.Flow

/**
 * The OCR-backed search index over recognized page text. Indexing is non-destructive — it only ever
 * stores recognized text, never modifying the original strokes.
 */
interface SearchRepository {

    /** Stores (or replaces) the recognized text for a page. */
    suspend fun indexPage(notebookId: String, pageId: String, recognizedText: String): AppResult<Unit>

    fun search(query: String): Flow<List<SearchResult>>

    suspend fun clearPageIndex(pageId: String): AppResult<Unit>

    suspend fun clearNotebookIndex(notebookId: String): AppResult<Unit>
}
