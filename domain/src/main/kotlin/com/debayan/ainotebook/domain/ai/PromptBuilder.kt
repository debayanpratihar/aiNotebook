package com.debayan.ainotebook.domain.ai

import javax.inject.Inject

/**
 * Builds the model prompt from the user's instruction and notebook context. Emits the ChatML format
 * used by the recommended Qwen2.5-Instruct models. Prompt construction stays internal and invisible
 * to the user, per the AI spec.
 */
class PromptBuilder @Inject constructor() {

    fun build(userInstruction: String, contextText: String): String = buildString {
        append("<|im_start|>system\n")
        append(SYSTEM_PROMPT)
        append("<|im_end|>\n<|im_start|>user\n")
        if (contextText.isNotBlank()) {
            append("Notebook context:\n")
            append(contextText.take(MAX_CONTEXT_CHARS))
            append("\n\n")
        }
        append(userInstruction.ifBlank { DEFAULT_INSTRUCTION })
        append("<|im_end|>\n<|im_start|>assistant\n")
    }

    private companion object {
        const val MAX_CONTEXT_CHARS = 4000
        const val DEFAULT_INSTRUCTION = "Continue these notes helpfully and concisely."
        const val SYSTEM_PROMPT =
            "You are a helpful study assistant writing directly into a handwritten notebook. " +
                "Answer clearly and concisely, matching the style of the surrounding notes."
    }
}
