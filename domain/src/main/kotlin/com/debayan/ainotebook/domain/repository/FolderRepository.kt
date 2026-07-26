package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.Folder
import kotlinx.coroutines.flow.Flow

/** Repository contract for folders. Implemented over Room in the data layer. */
interface FolderRepository {

    fun observeFolders(): Flow<List<Folder>>

    suspend fun createFolder(folder: Folder): AppResult<Unit>

    suspend fun renameFolder(id: String, name: String): AppResult<Unit>

    suspend fun deleteFolder(id: String): AppResult<Unit>
}
