package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.NotebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    @Upsert
    suspend fun upsert(notebook: NotebookEntity)

    @Upsert
    suspend fun upsertAll(notebooks: List<NotebookEntity>)

    @Delete
    suspend fun delete(notebook: NotebookEntity)

    @Query("SELECT * FROM notebooks WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE folderId = :folderId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeByFolder(folderId: String): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE notebookId = :id")
    fun observeById(id: String): Flow<NotebookEntity?>

    @Query("SELECT * FROM notebooks WHERE notebookId = :id")
    suspend fun getById(id: String): NotebookEntity?

    @Query("DELETE FROM notebooks WHERE notebookId = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE notebooks SET isFavorite = :favorite, updatedAt = :updatedAt WHERE notebookId = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, updatedAt: Long)

    @Query("UPDATE notebooks SET isArchived = :archived, updatedAt = :updatedAt WHERE notebookId = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)
}
