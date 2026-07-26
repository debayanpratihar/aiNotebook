package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.local.room.dao.FolderDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.Folder
import com.debayan.ainotebook.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [FolderRepository] backed by Room. */
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val dispatchers: DispatcherProvider,
) : FolderRepository {

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createFolder(folder: Folder): AppResult<Unit> =
        dispatchers.runDbCatching { folderDao.upsert(folder.toEntity()) }

    override suspend fun renameFolder(id: String, name: String): AppResult<Unit> =
        dispatchers.runDbCatching { folderDao.rename(id, name) }

    override suspend fun deleteFolder(id: String): AppResult<Unit> =
        dispatchers.runDbCatching { folderDao.deleteById(id) }
}
