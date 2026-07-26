package com.debayan.ainotebook.data.repository

import androidx.room.withTransaction
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
import com.debayan.ainotebook.data.local.room.dao.StrokeDao
import com.debayan.ainotebook.data.local.room.dao.StrokePointDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toPointEntities
import com.debayan.ainotebook.data.mapper.toStrokeEntity
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.repository.StrokeRepository
import com.debayan.ainotebook.data.util.runDbCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [StrokeRepository] over Room. A stroke and its points are written atomically (replacing any prior
 * points for that id). Deleting a stroke cascades to its points via the foreign key.
 */
class StrokeRepositoryImpl @Inject constructor(
    private val database: AiNotebookDatabase,
    private val strokeDao: StrokeDao,
    private val strokePointDao: StrokePointDao,
    private val dispatchers: DispatcherProvider,
) : StrokeRepository {

    override fun observeStrokes(pageId: String): Flow<List<Stroke>> =
        strokeDao.observeStrokesWithPointsByPage(pageId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveStroke(stroke: Stroke): AppResult<Unit> =
        dispatchers.runDbCatching {
            database.withTransaction {
                strokeDao.upsert(stroke.toStrokeEntity())
                strokePointDao.deleteByStroke(stroke.id)
                strokePointDao.insertAll(stroke.toPointEntities())
            }
        }

    override suspend fun deleteStroke(strokeId: String): AppResult<Unit> =
        dispatchers.runDbCatching { strokeDao.deleteById(strokeId) }

    override suspend fun deleteStrokes(strokeIds: List<String>): AppResult<Unit> =
        dispatchers.runDbCatching {
            database.withTransaction {
                strokeIds.forEach { strokeDao.deleteById(it) }
            }
        }
}
