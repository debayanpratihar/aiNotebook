package com.debayan.ainotebook.domain.usecase.ai

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.AiGenerationRequest
import com.debayan.ainotebook.domain.model.ai.AiGenerationState
import com.debayan.ainotebook.domain.provider.AiEngine
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams an AI response for a request. Collect it to write; cancel the collector to stop. */
class GenerateAiResponseUseCase @Inject constructor(
    private val aiEngine: AiEngine,
    dispatchers: DispatcherProvider,
) : FlowUseCase<AiGenerationRequest, AiGenerationState>(dispatchers.default) {

    override fun execute(params: AiGenerationRequest): Flow<AiGenerationState> =
        aiEngine.generate(params)
}
