package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Upsert
    suspend fun upsert(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE folderId = :id")
    suspend fun getById(id: String): FolderEntity?

    @Query("UPDATE folders SET name = :name WHERE folderId = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM folders WHERE folderId = :id")
    suspend fun deleteById(id: String)
}
