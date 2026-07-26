package com.debayan.ainotebook.domain.model.ai

/**
 * Streamed lifecycle of one AI generation. Consumers render [Writing.text] incrementally and treat
 * cancellation of the stream as a stop (keeping the partial text already received).
 */
sealed interface AiGenerationState {
    data object Idle : AiGenerationState

    /** Loading / preparing the model. */
    data object Preparing : AiGenerationState

    /** Model ready, prompt sent, awaiting the first token. */
    data object Thinking : AiGenerationState

    /** Tokens are arriving; [text] is the accumulated response so far. */
    data class Writing(val text: String) : AiGenerationState

    data class Completed(val text: String) : AiGenerationState

    data class Failed(val message: String) : AiGenerationState
}
