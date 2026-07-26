package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.debayan.ainotebook.data.local.room.entity.StrokePointEntity

@Dao
interface StrokePointDao {

    /** Bulk insert of freshly captured points (auto-generated ids), used for stroke persistence. */
    @Insert
    suspend fun insertAll(points: List<StrokePointEntity>)

    @Query("SELECT * FROM stroke_points WHERE strokeId = :strokeId ORDER BY sequenceNumber ASC")
    suspend fun getByStroke(strokeId: String): List<StrokePointEntity>

    @Query("DELETE FROM stroke_points WHERE strokeId = :strokeId")
    suspend fun deleteByStroke(strokeId: String)
}
