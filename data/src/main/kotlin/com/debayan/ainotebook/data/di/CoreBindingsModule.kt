package com.debayan.ainotebook.data.di

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.logging.Logger
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.dispatcher.DefaultDispatcherProvider
import com.debayan.ainotebook.data.logging.AndroidLogger
import com.debayan.ainotebook.data.time.SystemTimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds cross-cutting infrastructure abstractions to their production implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindLogger(impl: AndroidLogger): Logger

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
