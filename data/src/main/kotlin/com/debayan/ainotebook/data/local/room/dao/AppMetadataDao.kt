package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.AppMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetadataDao {

    @Upsert
    suspend fun upsert(metadata: AppMetadataEntity)

    @Query("SELECT * FROM app_metadata WHERE id = 1")
    suspend fun get(): AppMetadataEntity?

    @Query("SELECT * FROM app_metadata WHERE id = 1")
    fun observe(): Flow<AppMetadataEntity?>
}
