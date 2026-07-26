package com.debayan.ainotebook.domain.model.ai

/** Sampling parameters for a single generation request. */
data class AiGenerationParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
) {
    companion object {
        val DEFAULT = AiGenerationParams()
    }
}
