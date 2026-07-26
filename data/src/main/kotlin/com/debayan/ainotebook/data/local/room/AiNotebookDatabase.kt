package com.debayan.ainotebook.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.debayan.ainotebook.data.local.room.dao.AiAnnotationDao
import com.debayan.ainotebook.data.local.room.dao.AppMetadataDao
import com.debayan.ainotebook.data.local.room.dao.AttachmentDao
import com.debayan.ainotebook.data.local.room.dao.FolderDao
import com.debayan.ainotebook.data.local.room.dao.LayerDao
import com.debayan.ainotebook.data.local.room.dao.ModelDao
import com.debayan.ainotebook.data.local.room.dao.NotebookDao
import com.debayan.ainotebook.data.local.room.dao.PageDao
import com.debayan.ainotebook.data.local.room.dao.SearchIndexDao
import com.debayan.ainotebook.data.local.room.dao.StrokeDao
import com.debayan.ainotebook.data.local.room.dao.StrokePointDao
import com.debayan.ainotebook.data.local.room.dao.TagDao
import com.debayan.ainotebook.data.local.room.dao.TemplateDao
import com.debayan.ainotebook.data.local.room.entity.AiAnnotationEntity
import com.debayan.ainotebook.data.local.room.entity.AppMetadataEntity
import com.debayan.ainotebook.data.local.room.entity.AttachmentEntity
import com.debayan.ainotebook.data.local.room.entity.FolderEntity
import com.debayan.ainotebook.data.local.room.entity.LayerEntity
import com.debayan.ainotebook.data.local.room.entity.ModelEntity
import com.debayan.ainotebook.data.local.room.entity.NotebookEntity
import com.debayan.ainotebook.data.local.room.entity.NotebookTagCrossRef
import com.debayan.ainotebook.data.local.room.entity.PageEntity
import com.debayan.ainotebook.data.local.room.entity.SearchIndexEntity
import com.debayan.ainotebook.data.local.room.entity.StrokeEntity
import com.debayan.ainotebook.data.local.room.entity.StrokePointEntity
import com.debayan.ainotebook.data.local.room.entity.TagEntity
import com.debayan.ainotebook.data.local.room.entity.TemplateEntity

/**
 * The application's single Room database.
 *
 * All entities use only Room-native column types (String / Long / Int / Float / Boolean /
 * @Embedded), so no [androidx.room.TypeConverter]s are required at this schema version. Foreign-key
 * enforcement and WAL journaling are enabled by the builder in the DI layer.
 */
@Database(
    entities = [
        NotebookEntity::class,
        FolderEntity::class,
        PageEntity::class,
        LayerEntity::class,
        StrokeEntity::class,
        StrokePointEntity::class,
        AiAnnotationEntity::class,
        TemplateEntity::class,
        AttachmentEntity::class,
        TagEntity::class,
        NotebookTagCrossRef::class,
        SearchIndexEntity::class,
        AppMetadataEntity::class,
        ModelEntity::class,
    ],
    version = AiNotebookDatabase.VERSION,
    exportSchema = true,
)
abstract class AiNotebookDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao
    abstract fun folderDao(): FolderDao
    abstract fun pageDao(): PageDao
    abstract fun layerDao(): LayerDao
    abstract fun strokeDao(): StrokeDao
    abstract fun strokePointDao(): StrokePointDao
    abstract fun aiAnnotationDao(): AiAnnotationDao
    abstract fun templateDao(): TemplateDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun modelDao(): ModelDao

    companion object {
        const val VERSION: Int = 2
    }
}
