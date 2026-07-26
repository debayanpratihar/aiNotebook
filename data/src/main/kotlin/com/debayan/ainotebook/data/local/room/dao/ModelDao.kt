package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {

    @Upsert
    suspend fun upsert(model: ModelEntity)

    @Query("SELECT * FROM models ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ModelEntity?>

    @Query("SELECT * FROM models WHERE modelId = :id")
    suspend fun getById(id: String): ModelEntity?

    @Query("UPDATE models SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE models SET isActive = 1 WHERE modelId = :id")
    suspend fun activate(id: String)

    @Query("UPDATE models SET lastUsedAt = :timestamp WHERE modelId = :id")
    suspend fun markUsed(id: String, timestamp: Long)

    @Query("DELETE FROM models WHERE modelId = :id")
    suspend fun deleteById(id: String)
}
