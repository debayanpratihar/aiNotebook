package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Upsert
    suspend fun upsertAll(templates: List<TemplateEntity>)

    @Query("SELECT * FROM templates ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE templateId = :id")
    suspend fun getById(id: String): TemplateEntity?

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int
}
