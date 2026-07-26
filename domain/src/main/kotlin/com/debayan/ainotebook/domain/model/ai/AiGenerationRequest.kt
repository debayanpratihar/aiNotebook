package com.debayan.ainotebook.domain.model.ai

/**
 * A request to generate content. [contextText] is the already-extracted notebook context (e.g. OCR
 * text of the visible region) supplied by the caller, keeping the engine free of canvas concerns.
 */
data class AiGenerationRequest(
    val userInstruction: String,
    val contextText: String = "",
    val params: AiGenerationParams = AiGenerationParams.DEFAULT,
)
