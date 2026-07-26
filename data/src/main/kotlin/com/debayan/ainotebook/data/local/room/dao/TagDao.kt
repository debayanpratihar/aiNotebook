package com.debayan.ainotebook.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.debayan.ainotebook.data.local.room.entity.NotebookTagCrossRef
import com.debayan.ainotebook.data.local.room.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("DELETE FROM tags WHERE tagId = :id")
    suspend fun deleteById(id: String)

    @Upsert
    suspend fun linkTag(crossRef: NotebookTagCrossRef)

    @Delete
    suspend fun unlinkTag(crossRef: NotebookTagCrossRef)

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN notebook_tags nt ON t.tagId = nt.tagId
        WHERE nt.notebookId = :notebookId
        ORDER BY t.name ASC
        """,
    )
    fun observeTagsForNotebook(notebookId: String): Flow<List<TagEntity>>
}
