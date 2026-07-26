package com.debayan.ainotebook.domain.provider

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.AiGenerationRequest
import com.debayan.ainotebook.domain.model.ai.AiGenerationState
import kotlinx.coroutines.flow.Flow

/**
 * High-level AI orchestrator (the "AI Manager"): selects the active model, loads it on demand, builds
 * the prompt, and streams [AiGenerationState]. The single entry point the presentation layer uses.
 */
interface AiEngine {

    /** Runs one generation, emitting state as it progresses. Cancel the collector to stop. */
    fun generate(request: AiGenerationRequest): Flow<AiGenerationState>

    /** Loads the active model ahead of time to reduce first-token latency. */
    suspend fun preloadActiveModel(): AppResult<Unit>

    /** Releases model memory when the AI is idle (memory-management spec). */
    suspend fun releaseModel()
}
