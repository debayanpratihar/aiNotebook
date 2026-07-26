package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import kotlinx.coroutines.flow.Flow

/** Registry of locally installed models. */
interface ModelRepository {

    fun observeInstalledModels(): Flow<List<InstalledModel>>

    fun observeActiveModel(): Flow<InstalledModel?>

    suspend fun getModel(id: String): InstalledModel?

    suspend fun registerInstalledModel(model: InstalledModel): AppResult<Unit>

    /** Makes [id] the sole active model (deactivating any other), atomically. */
    suspend fun setActiveModel(id: String): AppResult<Unit>

    suspend fun markUsed(id: String): AppResult<Unit>

    suspend fun deleteModel(id: String): AppResult<Unit>
}
