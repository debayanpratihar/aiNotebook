package com.debayan.ainotebook.domain.provider

import com.debayan.ainotebook.domain.model.ai.SolveRequest
import com.debayan.ainotebook.domain.model.ai.SolveState
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point the UI uses to answer what the user wrote.
 *
 * Owns classification, routing, cloud failover, local fallback, and caching, so no view model ever
 * has to know which engine served a result. Replaces direct [AiEngine] use on the solve path;
 * [AiEngine] remains the lower-level local-generation contract underneath it.
 */
interface ProblemSolver {

    /** Runs one solve, emitting [SolveState] as it progresses. Cancel the collector to stop. */
    fun solve(request: SolveRequest): Flow<SolveState>

    /**
     * Whether a cloud provider is configured and usable. The UI needs this to decide between
     * offering "Solve" and offering "Add an API key".
     */
    suspend fun hasUsableCloudProvider(): Boolean

    /** Whether a local model is installed and its backend can run. */
    suspend fun hasUsableLocalModel(): Boolean
}
