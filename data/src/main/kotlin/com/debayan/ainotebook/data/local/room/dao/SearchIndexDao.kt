package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchIndexDao {

    @Upsert
    suspend fun upsert(entry: SearchIndexEntity)

    @Query("SELECT * FROM search_index WHERE recognizedText LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<SearchIndexEntity>>

    @Query("SELECT recognizedText FROM search_index WHERE pageId = :pageId LIMIT 1")
    suspend fun getTextForPage(pageId: String): String?

    @Query("DELETE FROM search_index WHERE notebookId = :notebookId")
    suspend fun deleteByNotebook(notebookId: String)

    @Query("DELETE FROM search_index WHERE pageId = :pageId")
    suspend fun deleteByPage(pageId: String)
}
