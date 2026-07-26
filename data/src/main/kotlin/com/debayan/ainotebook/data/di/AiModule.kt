package com.debayan.ainotebook.data.di

import com.debayan.ainotebook.data.ai.AiEngineImpl
import com.debayan.ainotebook.data.ai.LlamaInferenceEngine
import com.debayan.ainotebook.domain.provider.AiEngine
import com.debayan.ainotebook.domain.provider.InferenceEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the AI engine and its underlying inference backend. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindInferenceEngine(impl: LlamaInferenceEngine): InferenceEngine

    @Binds
    @Singleton
    abstract fun bindAiEngine(impl: AiEngineImpl): AiEngine
}
