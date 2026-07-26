package com.debayan.ainotebook.data.repository

import androidx.room.withTransaction
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
import com.debayan.ainotebook.data.local.room.dao.ModelDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** [ModelRepository] over Room. Activation deactivates all other models in one transaction. */
class ModelRepositoryImpl @Inject constructor(
    private val database: AiNotebookDatabase,
    private val modelDao: ModelDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : ModelRepository {

    override fun observeInstalledModels(): Flow<List<InstalledModel>> =
        modelDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveModel(): Flow<InstalledModel?> =
        modelDao.observeActive().map { it?.toDomain() }

    override suspend fun getModel(id: String): InstalledModel? =
        withContext(dispatchers.io) { modelDao.getById(id)?.toDomain() }

    override suspend fun registerInstalledModel(model: InstalledModel): AppResult<Unit> =
        dispatchers.runDbCatching { modelDao.upsert(model.toEntity()) }

    override suspend fun setActiveModel(id: String): AppResult<Unit> =
        dispatchers.runDbCatching {
            database.withTransaction {
                modelDao.clearActive()
                modelDao.activate(id)
            }
        }

    override suspend fun markUsed(id: String): AppResult<Unit> =
        dispatchers.runDbCatching { modelDao.markUsed(id, timeProvider.now()) }

    override suspend fun deleteModel(id: String): AppResult<Unit> =
        dispatchers.runDbCatching {
            // Remove the on-disk file (best effort) before dropping the registry row.
            modelDao.getById(id)?.let { model -> runCatching { File(model.localPath).delete() } }
            modelDao.deleteById(id)
        }
}
