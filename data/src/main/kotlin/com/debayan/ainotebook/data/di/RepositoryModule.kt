package com.debayan.ainotebook.data.di

import com.debayan.ainotebook.data.repository.ConfigRepositoryImpl
import com.debayan.ainotebook.data.repository.FolderRepositoryImpl
import com.debayan.ainotebook.data.repository.LayerRepositoryImpl
import com.debayan.ainotebook.data.repository.ModelRepositoryImpl
import com.debayan.ainotebook.data.repository.NotebookRepositoryImpl
import com.debayan.ainotebook.data.repository.PageRepositoryImpl
import com.debayan.ainotebook.data.repository.SettingsRepositoryImpl
import com.debayan.ainotebook.data.repository.StrokeRepositoryImpl
import com.debayan.ainotebook.domain.repository.ConfigRepository
import com.debayan.ainotebook.domain.repository.FolderRepository
import com.debayan.ainotebook.domain.repository.LayerRepository
import com.debayan.ainotebook.domain.repository.ModelRepository
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.repository.PageRepository
import com.debayan.ainotebook.domain.repository.SettingsRepository
import com.debayan.ainotebook.domain.repository.StrokeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain repository contracts to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNotebookRepository(impl: NotebookRepositoryImpl): NotebookRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindPageRepository(impl: PageRepositoryImpl): PageRepository

    @Binds
    @Singleton
    abstract fun bindLayerRepository(impl: LayerRepositoryImpl): LayerRepository

    @Binds
    @Singleton
    abstract fun bindStrokeRepository(impl: StrokeRepositoryImpl): StrokeRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository
}
