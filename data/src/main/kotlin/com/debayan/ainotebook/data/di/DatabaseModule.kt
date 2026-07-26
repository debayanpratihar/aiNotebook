package com.debayan.ainotebook.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.debayan.ainotebook.core.AppConstants
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
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
import com.debayan.ainotebook.data.local.room.migration.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and its DAOs.
 *
 * The database uses WAL journaling for concurrent read/write throughput. Foreign-key enforcement is
 * enabled by Room by default. Only explicit [DatabaseMigrations] are applied — there is no
 * destructive fallback, per the database spec.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiNotebookDatabase =
        Room.databaseBuilder(
            context,
            AiNotebookDatabase::class.java,
            AppConstants.DATABASE_NAME,
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()

    @Provides fun provideNotebookDao(db: AiNotebookDatabase): NotebookDao = db.notebookDao()
    @Provides fun provideFolderDao(db: AiNotebookDatabase): FolderDao = db.folderDao()
    @Provides fun providePageDao(db: AiNotebookDatabase): PageDao = db.pageDao()
    @Provides fun provideLayerDao(db: AiNotebookDatabase): LayerDao = db.layerDao()
    @Provides fun provideStrokeDao(db: AiNotebookDatabase): StrokeDao = db.strokeDao()
    @Provides fun provideStrokePointDao(db: AiNotebookDatabase): StrokePointDao = db.strokePointDao()
    @Provides fun provideAiAnnotationDao(db: AiNotebookDatabase): AiAnnotationDao = db.aiAnnotationDao()
    @Provides fun provideTemplateDao(db: AiNotebookDatabase): TemplateDao = db.templateDao()
    @Provides fun provideAttachmentDao(db: AiNotebookDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun provideTagDao(db: AiNotebookDatabase): TagDao = db.tagDao()
    @Provides fun provideSearchIndexDao(db: AiNotebookDatabase): SearchIndexDao = db.searchIndexDao()
    @Provides fun provideAppMetadataDao(db: AiNotebookDatabase): AppMetadataDao = db.appMetadataDao()
    @Provides fun provideModelDao(db: AiNotebookDatabase): ModelDao = db.modelDao()
}
