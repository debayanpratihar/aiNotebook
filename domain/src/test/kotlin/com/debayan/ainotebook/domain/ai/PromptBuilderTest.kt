package com.debayan.ainotebook.domain.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val builder = PromptBuilder()

    @Test
    fun build_includesInstructionContextAndChatMlMarkers() {
        val prompt = builder.build(userInstruction = "Solve x + 2 = 5", contextText = "lecture notes")

        assertTrue(prompt.contains("Solve x + 2 = 5"))
        assertTrue(prompt.contains("lecture notes"))
        assertTrue(prompt.contains("<|im_start|>system"))
        assertTrue(prompt.contains("<|im_start|>user"))
        assertTrue(prompt.contains("<|im_start|>assistant"))
    }

    @Test
    fun build_blankInstruction_stillProducesValidPrompt() {
        val prompt = builder.build(userInstruction = "", contextText = "")
        assertTrue(prompt.contains("<|im_start|>assistant"))
        assertTrue(prompt.isNotBlank())
    }
}
