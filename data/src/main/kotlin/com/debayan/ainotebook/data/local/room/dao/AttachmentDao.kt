package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Upsert
    suspend fun upsert(attachment: AttachmentEntity)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE notebookId = :notebookId ORDER BY importedAt DESC")
    fun observeByNotebook(notebookId: String): Flow<List<AttachmentEntity>>

    @Query("DELETE FROM attachments WHERE attachmentId = :id")
    suspend fun deleteById(id: String)
}
