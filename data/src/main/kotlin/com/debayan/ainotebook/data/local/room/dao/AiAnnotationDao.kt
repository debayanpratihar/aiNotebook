package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.AiAnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAnnotationDao {

    @Upsert
    suspend fun upsert(annotation: AiAnnotationEntity)

    @Delete
    suspend fun delete(annotation: AiAnnotationEntity)

    @Query("SELECT * FROM ai_annotations WHERE pageId = :pageId ORDER BY generatedAt ASC")
    fun observeByPage(pageId: String): Flow<List<AiAnnotationEntity>>

    @Query("DELETE FROM ai_annotations WHERE annotationId = :id")
    suspend fun deleteById(id: String)
}
