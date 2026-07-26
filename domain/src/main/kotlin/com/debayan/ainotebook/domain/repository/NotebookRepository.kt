package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.Notebook
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for notebooks. Implemented over Room in the data layer.
 *
 * Observation methods return [Flow] (reactive, single source of truth); mutations return
 * [AppResult] so callers handle typed failures explicitly.
 */
interface NotebookRepository {

    fun observeNotebooks(): Flow<List<Notebook>>

    fun observeNotebook(id: String): Flow<Notebook?>

    suspend fun createNotebook(notebook: Notebook): AppResult<Unit>

    suspend fun updateNotebook(notebook: Notebook): AppResult<Unit>

    suspend fun deleteNotebook(id: String): AppResult<Unit>

    suspend fun setFavorite(id: String, favorite: Boolean): AppResult<Unit>

    suspend fun setArchived(id: String, archived: Boolean): AppResult<Unit>
}
