package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.LayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LayerDao {

    @Upsert
    suspend fun upsert(layer: LayerEntity)

    @Delete
    suspend fun delete(layer: LayerEntity)

    @Query("SELECT * FROM layers WHERE pageId = :pageId ORDER BY orderIndex ASC")
    fun observeByPage(pageId: String): Flow<List<LayerEntity>>

    @Query("DELETE FROM layers WHERE layerId = :id")
    suspend fun deleteById(id: String)
}
