package com.debayan.ainotebook.data.repository

import androidx.room.withTransaction
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
import com.debayan.ainotebook.data.local.room.dao.LayerDao
import com.debayan.ainotebook.data.local.room.dao.NotebookDao
import com.debayan.ainotebook.data.local.room.dao.PageDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.canvas.Layer
import com.debayan.ainotebook.domain.model.canvas.Page
import com.debayan.ainotebook.domain.repository.PageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [PageRepository] over Room. Page creation and deletion are wrapped in a single transaction so the
 * page, its default layer, and the owning notebook's page-count stay consistent.
 */
class PageRepositoryImpl @Inject constructor(
    private val database: AiNotebookDatabase,
    private val pageDao: PageDao,
    private val layerDao: LayerDao,
    private val notebookDao: NotebookDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : PageRepository {

    override fun observePages(notebookId: String): Flow<List<Page>> =
        pageDao.observeByNotebook(notebookId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPage(pageId: String): Page? =
        withContext(dispatchers.io) { pageDao.getById(pageId)?.toDomain() }

    override suspend fun getFirstPage(notebookId: String): Page? =
        withContext(dispatchers.io) { pageDao.getFirstPage(notebookId)?.toDomain() }

    override suspend fun createPage(page: Page, defaultLayer: Layer): AppResult<Unit> =
        dispatchers.runDbCatching {
            database.withTransaction {
                pageDao.upsert(page.toEntity())
                layerDao.upsert(defaultLayer.toEntity())
                notebookDao.changePageCount(page.notebookId, delta = 1, updatedAt = timeProvider.now())
            }
        }

    override suspend fun deletePage(pageId: String): AppResult<Unit> =
        dispatchers.runDbCatching {
            database.withTransaction {
                val page = pageDao.getById(pageId) ?: return@withTransaction
                pageDao.deleteById(pageId)
                notebookDao.changePageCount(page.notebookId, delta = -1, updatedAt = timeProvider.now())
            }
        }
}
