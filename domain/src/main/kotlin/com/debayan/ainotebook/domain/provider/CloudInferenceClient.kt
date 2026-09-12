package com.debayan.ainotebook.domain.provider

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.AiProviderConfig
import com.debayan.ainotebook.domain.model.ai.AiProviderKind
import com.debayan.ainotebook.domain.model.ai.CloudCompletionRequest
import com.debayan.ainotebook.domain.model.ai.CloudEvent
import com.debayan.ainotebook.domain.model.ai.ProviderHealth
import kotlinx.coroutines.flow.Flow

/**
 * One cloud wire protocol. Implementations are selected by [handles], so adding a vendor with a
 * genuinely different body shape means adding a client, not editing the orchestrator.
 *
 * [apiKey] is passed per call and never retained: the only durable copy lives in the platform
 * keystore.
 */
interface CloudInferenceClient {

    fun handles(kind: AiProviderKind): Boolean

    /**
     * Streams a completion. The flow must fail with
     * [com.debayan.ainotebook.domain.provider.CloudRequestException] so the caller can inspect the
     * classified [com.debayan.ainotebook.domain.model.ai.CloudFailure] and decide whether failing
     * over to the next provider is appropriate.
     */
    fun stream(
        provider: AiProviderConfig,
        apiKey: String,
        request: CloudCompletionRequest,
    ): Flow<CloudEvent>

    /**
     * Cheapest possible round-trip that proves the key, URL, and model id all work together. Backs
     * the "Test connection" button and the startup latency probe.
     */
    suspend fun healthCheck(
        provider: AiProviderConfig,
        apiKey: String,
    ): AppResult<ProviderHealth>
}

/**
 * Carries a classified [com.debayan.ainotebook.domain.model.ai.CloudFailure] out of a streaming
 * flow. A plain [java.io.IOException] would lose the transient/permanent distinction that failover
 * depends on.
 */
class CloudRequestException(
    val failure: com.debayan.ainotebook.domain.model.ai.CloudFailure,
) : Exception(failure.message)
