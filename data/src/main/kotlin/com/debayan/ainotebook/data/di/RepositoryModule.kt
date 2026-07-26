package com.debayan.ainotebook.data.di

import com.debayan.ainotebook.data.repository.FolderRepositoryImpl
import com.debayan.ainotebook.data.repository.NotebookRepositoryImpl
import com.debayan.ainotebook.data.repository.SettingsRepositoryImpl
import com.debayan.ainotebook.domain.repository.FolderRepository
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.repository.SettingsRepository
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
}
