package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.local.room.dao.NotebookDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [NotebookRepository] backed by Room. Observation maps entity rows to domain models; mutations run
 * on the IO dispatcher and surface typed failures via [AppResult].
 *
 * The `updatedAt` bookkeeping timestamp is stamped in the data layer so persistence stays the single
 * writer of storage metadata.
 */
class NotebookRepositoryImpl @Inject constructor(
    private val notebookDao: NotebookDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : NotebookRepository {

    override fun observeNotebooks(): Flow<List<Notebook>> =
        notebookDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeNotebook(id: String): Flow<Notebook?> =
        notebookDao.observeById(id).map { it?.toDomain() }

    override suspend fun createNotebook(notebook: Notebook): AppResult<Unit> =
        dispatchers.runDbCatching { notebookDao.upsert(notebook.toEntity()) }

    override suspend fun updateNotebook(notebook: Notebook): AppResult<Unit> =
        dispatchers.runDbCatching {
            notebookDao.upsert(notebook.copy(updatedAt = timeProvider.now()).toEntity())
        }

    override suspend fun deleteNotebook(id: String): AppResult<Unit> =
        dispatchers.runDbCatching { notebookDao.deleteById(id) }

    override suspend fun setFavorite(id: String, favorite: Boolean): AppResult<Unit> =
        dispatchers.runDbCatching { notebookDao.setFavorite(id, favorite, timeProvider.now()) }

    override suspend fun setArchived(id: String, archived: Boolean): AppResult<Unit> =
        dispatchers.runDbCatching { notebookDao.setArchived(id, archived, timeProvider.now()) }
}
