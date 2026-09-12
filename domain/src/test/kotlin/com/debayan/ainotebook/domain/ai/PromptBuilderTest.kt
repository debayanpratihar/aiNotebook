package com.debayan.ainotebook.domain.ai

import com.debayan.ainotebook.domain.model.ai.ProblemAssessment
import com.debayan.ainotebook.domain.model.ai.ProblemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun buildLocal_mathUsesTheAcceptedExpressionRatherThanTheRawReading() {
        val prompt = builder.buildLocal(
            assessment = assessment(ProblemKind.ARITHMETIC, expression = "12*47"),
            recognizedText = "l2 × 47",
        )

        assertTrue(prompt.contains("12*47"))
        assertFalse(prompt.contains("l2"))
    }

    @Test
    fun buildLocal_mathWithoutAnAcceptedExpression_stillNormalizesTheReading() {
        val prompt = builder.buildLocal(
            assessment = assessment(ProblemKind.CALCULUS, expression = null),
            recognizedText = "3 × 4",
        )

        assertTrue(prompt.contains("Expression:"))
        assertFalse(prompt.contains("×"))
    }

    @Test
    fun nonMath_keepsTheReadingVerbatim() {
        val prompt = builder.buildCloud(
            assessment = assessment(ProblemKind.WORD_PROBLEM),
            recognizedText = "A train leaves at 3 pm going 40 km/h.",
        )

        assertTrue(prompt.user.contains("A train leaves at 3 pm going 40 km/h."))
    }

    @Test
    fun localSystemPrompt_isShorterThanTheCloudOne() {
        val assessment = assessment(ProblemKind.ALGEBRA, expression = "2*x+3=11")

        val local = builder.buildLocalPrompt(assessment, "2x + 3 = 11")
        val cloud = builder.buildCloud(assessment, "2x + 3 = 11")

        assertTrue(cloud.system.length > local.system.length)
    }

    @Test
    fun everyKind_getsItsOwnNonBlankSystemPromptAtBothDetailLevels() {
        for (detail in PromptDetail.entries) {
            val systems = ProblemKind.entries.map { kind ->
                builder.buildCloud(assessment(kind), "anything", detail = detail).system
            }
            systems.forEach { assertTrue(it.isNotBlank()) }
            assertEquals(ProblemKind.entries.size, systems.toSet().size)
        }
    }

    @Test
    fun everyPrompt_demandsLinearTextAndMarksTheFinalAnswer() {
        for (detail in PromptDetail.entries) {
            for (kind in ProblemKind.entries) {
                val system = builder.buildCloud(assessment(kind), "anything", detail = detail)
                    .system
                    .lowercase()

                assertTrue(system.contains("markdown"))
                assertTrue(system.contains("latex"))
                assertTrue(system.contains(PromptBuilder.ANSWER_MARKER.lowercase()))
            }
        }
    }

    @Test
    fun twoProblemsOnTheSamePage_shareThePromptPrefixThatTheKvCacheReuses() {
        val page = "Homework 4\nShow all working."
        val first = builder.buildCloud(assessment(ProblemKind.ARITHMETIC, "1+1"), "1 + 1", page)
        val second = builder.buildCloud(assessment(ProblemKind.ARITHMETIC, "2+2"), "2 + 2", page)

        assertEquals(first.system, second.system)
        assertTrue(first.user.commonPrefixWith(second.user).contains(page))
    }

    @Test
    fun longPageContext_keepsTheEndOfThePageNotTheStart() {
        val page = "OPENING " + "filler ".repeat(PromptBuilder.CLOUD_CONTEXT_CHARS) + "CLOSING"

        val prompt = builder.buildCloud(assessment(ProblemKind.FREEFORM_QUESTION), "Why?", page)

        assertFalse(prompt.user.contains("OPENING"))
        assertTrue(prompt.user.contains("CLOSING"))
    }

    @Test
    fun progressiveOffload_sendsTheStatementAndTheOfflineAnswerOnly() {
        val prompt = builder.buildProgressiveOffload(
            assessment = assessment(ProblemKind.ALGEBRA, expression = "2*x+3=11"),
            recognizedText = "solve 2x + 3 = 11",
            offlineAnswer = "x = 4",
        )

        assertEquals(3, prompt.user.lines().size)
        assertTrue(prompt.user.contains("algebra"))
        assertTrue(prompt.user.contains("2*x+3=11"))
        assertTrue(prompt.user.contains("x = 4"))
    }

    @Test
    fun progressiveOffload_withoutAnOfflineAnswer_isJustKindAndStatement() {
        val prompt = builder.buildProgressiveOffload(
            assessment = assessment(ProblemKind.WORD_PROBLEM),
            recognizedText = "Two trains leave at noon.",
        )

        assertEquals(2, prompt.user.lines().size)
        assertTrue(prompt.user.contains("word problem"))
        assertTrue(prompt.user.contains("Two trains leave at noon."))
    }

    private fun assessment(kind: ProblemKind, expression: String? = null) = ProblemAssessment(
        kind = kind,
        complexity = 0.3f,
        requiresSymbolicSolving = kind == ProblemKind.ALGEBRA || kind == ProblemKind.CALCULUS,
        estimatedPromptTokens = 240,
        normalizedExpression = expression,
        rationale = "Test assessment.",
    )
}
