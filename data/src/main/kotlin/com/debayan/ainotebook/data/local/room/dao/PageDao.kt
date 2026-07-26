package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Upsert
    suspend fun upsert(page: PageEntity)

    @Upsert
    suspend fun upsertAll(pages: List<PageEntity>)

    @Delete
    suspend fun delete(page: PageEntity)

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageNumber ASC")
    fun observeByNotebook(notebookId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE pageId = :id")
    suspend fun getById(id: String): PageEntity?

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageNumber ASC LIMIT 1")
    suspend fun getFirstPage(notebookId: String): PageEntity?

    @Query("SELECT COUNT(*) FROM pages WHERE notebookId = :notebookId")
    suspend fun countForNotebook(notebookId: String): Int

    @Query("DELETE FROM pages WHERE pageId = :id")
    suspend fun deleteById(id: String)
}
