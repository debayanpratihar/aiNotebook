package com.debayan.ainotebook.domain.provider

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.AiGenerationParams
import com.debayan.ainotebook.domain.model.ai.InferenceConfig
import kotlinx.coroutines.flow.Flow

/**
 * Low-level, backend-agnostic contract for local text generation. The production implementation is
 * backed by llama.cpp (GGUF) via JNI. Kept in the domain so nothing above depends on the native
 * backend directly.
 */
interface InferenceEngine {

    /** Path of the currently loaded model, or null if none is loaded. */
    val loadedModelPath: String?

    suspend fun loadModel(modelPath: String, config: InferenceConfig): AppResult<Unit>

    /**
     * Streams generated tokens for [prompt]. Cancelling the collector stops generation and frees the
     * decode loop (the backend is told to stop).
     */
    fun generate(prompt: String, params: AiGenerationParams): Flow<String>

    suspend fun unload()
}
