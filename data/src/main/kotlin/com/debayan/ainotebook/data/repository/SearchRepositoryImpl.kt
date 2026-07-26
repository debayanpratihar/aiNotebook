package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.local.room.dao.SearchIndexDao
import com.debayan.ainotebook.data.local.room.entity.SearchIndexEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.search.SearchResult
import com.debayan.ainotebook.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [SearchRepository] over the `search_index` table. One row per page (keyed by page id), so
 * re-indexing a page replaces its prior text.
 */
class SearchRepositoryImpl @Inject constructor(
    private val searchIndexDao: SearchIndexDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : SearchRepository {

    override suspend fun indexPage(
        notebookId: String,
        pageId: String,
        recognizedText: String,
    ): AppResult<Unit> = dispatchers.runDbCatching {
        searchIndexDao.upsert(
            SearchIndexEntity(
                indexId = pageId,
                notebookId = notebookId,
                pageId = pageId,
                recognizedText = recognizedText,
                lastIndexedAt = timeProvider.now(),
            ),
        )
    }

    override fun search(query: String): Flow<List<SearchResult>> =
        searchIndexDao.search(query).map { rows ->
            rows.map { SearchResult(it.notebookId, it.pageId, it.recognizedText) }
        }

    override suspend fun clearPageIndex(pageId: String): AppResult<Unit> =
        dispatchers.runDbCatching { searchIndexDao.deleteByPage(pageId) }

    override suspend fun clearNotebookIndex(notebookId: String): AppResult<Unit> =
        dispatchers.runDbCatching { searchIndexDao.deleteByNotebook(notebookId) }
}
