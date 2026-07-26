package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.StrokeEntity
import com.debayan.ainotebook.data.local.room.relation.StrokeWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface StrokeDao {

    @Upsert
    suspend fun upsert(stroke: StrokeEntity)

    @Upsert
    suspend fun upsertAll(strokes: List<StrokeEntity>)

    @Delete
    suspend fun delete(stroke: StrokeEntity)

    @Query("SELECT * FROM strokes WHERE layerId = :layerId ORDER BY createdAt ASC")
    fun observeByLayer(layerId: String): Flow<List<StrokeEntity>>

    /** All strokes on a page (across every layer), each with its points, ordered oldest-first. */
    @Transaction
    @Query(
        """
        SELECT s.* FROM strokes s
        INNER JOIN layers l ON s.layerId = l.layerId
        WHERE l.pageId = :pageId
        ORDER BY s.createdAt ASC
        """,
    )
    fun observeStrokesWithPointsByPage(pageId: String): Flow<List<StrokeWithPoints>>

    @Query("SELECT * FROM strokes WHERE layerId = :layerId ORDER BY createdAt ASC")
    suspend fun getByLayer(layerId: String): List<StrokeEntity>

    @Query("DELETE FROM strokes WHERE strokeId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM strokes WHERE layerId = :layerId")
    suspend fun deleteByLayer(layerId: String)
}
