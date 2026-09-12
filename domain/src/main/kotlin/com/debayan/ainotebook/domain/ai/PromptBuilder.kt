package com.debayan.ainotebook.domain.ai

import com.debayan.ainotebook.domain.model.ai.ProblemAssessment
import com.debayan.ainotebook.domain.model.ai.ProblemKind
import com.debayan.ainotebook.domain.recognition.MathTokenNormalizer
import javax.inject.Inject

/**
 * How much prompt the target can afford to be told.
 *
 * On-device, prompt length is not free advice — it is prefill work the user waits through before the
 * first token appears, and on a 0.5B model a long instruction also crowds out the problem itself. A
 * cloud model prefills in parallel on hardware that makes the difference invisible, so it gets the
 * fuller wording that produces better-structured answers.
 */
enum class PromptDetail { TERSE, FULL }

/**
 * A system/user prompt pair, kept split so each backend can apply its own chat template.
 */
data class ModelPrompt(val system: String, val user: String) {

    /**
     * Renders the pair in ChatML, the format the recommended Qwen2.5-Instruct GGUF builds expect.
     *
     * Backends whose model uses different turn markers — Gemma's `<start_of_turn>`, for one — should
     * take [system] and [user] and render their own: literal `<|im_start|>` text in a Gemma context
     * is tokens spent on a string the model was never trained to see.
     */
    fun asChatMl(): String = buildString {
        append("<|im_start|>system\n")
        append(system)
        append("<|im_end|>\n<|im_start|>user\n")
        append(user)
        append("<|im_end|>\n<|im_start|>assistant\n")
    }
}

/**
 * Builds every prompt the app sends to a model, local or cloud.
 *
 * Three things shape the output beyond the wording itself.
 *
 * The answer is drawn back onto the page as handwriting strokes, which can only render a line of
 * characters. Markdown, tables, LaTeX and stacked fractions have no stroke representation at all, so
 * every prompt forbids them and asks for a final line marked with [PromptBuilder.ANSWER_MARKER] that
 * the answer card can lift out as the result.
 *
 * The stable parts come first — system prompt, then page context, then the one changing problem line
 * last. Two problems written on the same page therefore share a byte-identical prefix, which is what
 * lets [com.debayan.ainotebook.domain.model.ai.InferenceConfig.reusePromptCache] skip re-prefilling
 * the page on every follow-up instead of paying for it again.
 *
 * Math reaches the model as the normalized expression rather than the raw reading, so the model is
 * asked the same question the app believes it read, without the recognizer's leftover `l`-for-`1`
 * and `x`-for-`×` ambiguities.
 */
class PromptBuilder @Inject constructor() {

    /**
     * The pre-routing local entry point used by [com.debayan.ainotebook.domain.provider.AiEngine],
     * where no assessment has been computed. Routed solves should use [buildLocal] or [buildCloud],
     * which can tailor the instruction to the kind of problem.
     *
     * A blank [userInstruction] means "do something sensible with this page", which is the notes
     * behaviour rather than the question behaviour.
     */
    fun build(userInstruction: String, contextText: String): String = prompt(
        kind = if (userInstruction.isBlank()) ProblemKind.PROSE else ProblemKind.FREEFORM_QUESTION,
        expression = null,
        problem = userInstruction,
        pageContext = contextText,
        contextLimit = LOCAL_CONTEXT_CHARS,
        detail = PromptDetail.TERSE,
    ).asChatMl()

    /** [buildLocal] without the ChatML wrapper, for a backend that applies its own template. */
    fun buildLocalPrompt(
        assessment: ProblemAssessment,
        recognizedText: String,
        pageContext: String = "",
        detail: PromptDetail = PromptDetail.TERSE,
    ): ModelPrompt = prompt(
        kind = assessment.kind,
        expression = assessment.normalizedExpression,
        problem = recognizedText,
        pageContext = pageContext,
        contextLimit = LOCAL_CONTEXT_CHARS,
        detail = detail,
    )

    /**
     * Builds the on-device prompt, ready for
     * [com.debayan.ainotebook.domain.provider.InferenceEngine.generate].
     *
     * [detail] defaults to [PromptDetail.TERSE] because the local tier is a small model on a phone;
     * pass [PromptDetail.FULL] for a 7B-class local model, where the extra prefill buys more than it
     * costs.
     */
    fun buildLocal(
        assessment: ProblemAssessment,
        recognizedText: String,
        pageContext: String = "",
        detail: PromptDetail = PromptDetail.TERSE,
    ): String = buildLocalPrompt(assessment, recognizedText, pageContext, detail).asChatMl()

    /**
     * Builds the cloud prompt. The pair maps straight onto
     * [com.debayan.ainotebook.domain.model.ai.CloudCompletionRequest]'s `systemPrompt` and
     * `userPrompt`, so no client has to re-split it.
     */
    fun buildCloud(
        assessment: ProblemAssessment,
        recognizedText: String,
        pageContext: String = "",
        detail: PromptDetail = PromptDetail.FULL,
    ): ModelPrompt = prompt(
        kind = assessment.kind,
        expression = assessment.normalizedExpression,
        problem = recognizedText,
        pageContext = pageContext,
        contextLimit = CLOUD_CONTEXT_CHARS,
        detail = detail,
    )

    /**
     * Builds the minimum that can answer the problem: the kind, the one problem line, and the answer
     * already computed on-device when there is one. No page context, ever.
     *
     * This is the prompt for escalating to the cloud after an offline or local attempt. The user is
     * asking for help with one problem, not publishing their notebook, so escalation sends the
     * problem rather than the page — and it is also the cheapest prompt available, which matters when
     * the tokens are billed to the user's own key.
     *
     * When [offlineAnswer] is supplied the model is asked to check it rather than to start over,
     * which both grounds the explanation in the exact answer already shown on the page and stops the
     * card from contradicting itself a second later.
     */
    fun buildProgressiveOffload(
        assessment: ProblemAssessment,
        recognizedText: String,
        offlineAnswer: String? = null,
        detail: PromptDetail = PromptDetail.FULL,
    ): ModelPrompt {
        val statement = problemLine(
            kind = assessment.kind,
            expression = assessment.normalizedExpression,
            recognizedText = recognizedText,
            limit = OFFLOAD_PROBLEM_CHARS,
        )
        val user = buildString {
            append(Prompts.KIND_LABEL).append(": ").append(kindTag(assessment.kind)).append('\n')
            append(statement)
            if (!offlineAnswer.isNullOrBlank()) {
                append('\n')
                append(Prompts.verifyLine(offlineAnswer.trim()))
            }
        }
        return ModelPrompt(system = systemPrompt(assessment.kind, detail), user = user)
    }

    private fun prompt(
        kind: ProblemKind,
        expression: String?,
        problem: String,
        pageContext: String,
        contextLimit: Int,
        detail: PromptDetail,
    ): ModelPrompt {
        val user = buildString {
            val context = contextWindow(pageContext, contextLimit)
            if (context.isNotEmpty()) {
                append(Prompts.CONTEXT_HEADER).append('\n').append(context).append("\n\n")
            }
            append(problemLine(kind, expression, problem, PROBLEM_CHARS))
        }
        return ModelPrompt(system = systemPrompt(kind, detail), user = user)
    }

    private fun systemPrompt(kind: ProblemKind, detail: PromptDetail): String {
        val terse = detail == PromptDetail.TERSE
        val identity = if (terse) Prompts.IDENTITY_TERSE else Prompts.IDENTITY_FULL
        val task = when (kind) {
            ProblemKind.ARITHMETIC -> if (terse) Prompts.ARITHMETIC_TERSE else Prompts.ARITHMETIC_FULL
            ProblemKind.ALGEBRA -> if (terse) Prompts.ALGEBRA_TERSE else Prompts.ALGEBRA_FULL
            ProblemKind.CALCULUS -> if (terse) Prompts.CALCULUS_TERSE else Prompts.CALCULUS_FULL
            ProblemKind.WORD_PROBLEM -> if (terse) Prompts.WORD_PROBLEM_TERSE else Prompts.WORD_PROBLEM_FULL
            ProblemKind.FREEFORM_QUESTION -> if (terse) Prompts.QUESTION_TERSE else Prompts.QUESTION_FULL
            ProblemKind.PROSE -> if (terse) Prompts.PROSE_TERSE else Prompts.PROSE_FULL
        }
        val format = if (terse) Prompts.FORMAT_TERSE else Prompts.FORMAT_FULL
        return "$identity $task\n$format"
    }

    /**
     * The one line that changes between problems on the same page.
     *
     * [expression] is preferred for math because it is the reading the math engine and the candidate
     * reranker already agreed on. Math the offline engine rejected arrives without one — a
     * non-polynomial integral, say — and is normalized here so it still reaches the model as an
     * expression instead of as raw ink.
     */
    private fun problemLine(
        kind: ProblemKind,
        expression: String?,
        recognizedText: String,
        limit: Int,
    ): String {
        val text = recognizedText.trim().take(limit)
        val accepted = expression?.trim().orEmpty()
        val body = when {
            !kind.isMathematical -> text
            accepted.isNotEmpty() -> accepted
            else -> MathTokenNormalizer.normalize(text).ifBlank { text }
        }
        if (body.isBlank()) return Prompts.DEFAULT_INSTRUCTION
        return "${label(kind)}: $body"
    }

    private fun label(kind: ProblemKind): String = when (kind) {
        ProblemKind.ARITHMETIC, ProblemKind.ALGEBRA, ProblemKind.CALCULUS -> Prompts.LABEL_EXPRESSION
        ProblemKind.WORD_PROBLEM -> Prompts.LABEL_PROBLEM
        ProblemKind.FREEFORM_QUESTION -> Prompts.LABEL_QUESTION
        ProblemKind.PROSE -> Prompts.LABEL_NOTES
    }

    private fun kindTag(kind: ProblemKind): String =
        kind.name.lowercase().replace('_', ' ')

    /**
     * Keeps the **end** of the page rather than the start.
     *
     * The user writes top to bottom, so the problem they just asked about sits at the bottom and the
     * lines immediately above it are the ones that give it meaning. Truncating from the front, as a
     * plain `take` does, throws away exactly the context that was relevant. The cut is then nudged
     * past the first word break so the model is never handed half a word.
     */
    private fun contextWindow(pageContext: String, limit: Int): String {
        val trimmed = pageContext.trim()
        if (trimmed.length <= limit) return trimmed
        val tail = trimmed.takeLast(limit)
        val breakAt = tail.indexOfFirst { it == ' ' || it == '\n' }
        return if (breakAt in 0..MAX_WORD_SCAN) tail.substring(breakAt + 1).trim() else tail.trim()
    }

    companion object {
        /**
         * Prefix of the final line of every answer. Public because the answer card splits the result
         * off the working with it, and the handwriting renderer draws the result larger — both break
         * silently if the marker drifts away from the wording in the prompt.
         */
        const val ANSWER_MARKER: String = "Answer:"

        /** Page context sent to a cloud model: roughly a full notebook page. */
        const val CLOUD_CONTEXT_CHARS: Int = 4_000

        /**
         * Page context sent to a local model.
         *
         * Far smaller than the cloud budget by necessity, not by preference: 4,000 characters is
         * about 1,000 tokens, which on its own overflows the 768-token window of
         * [com.debayan.ainotebook.domain.model.ai.InferenceConfig.LOW_MEMORY] and would leave no room
         * for either the question or the answer.
         */
        const val LOCAL_CONTEXT_CHARS: Int = 1_100

        /** Cap on the problem itself; longer than this is a page, not a problem. */
        const val PROBLEM_CHARS: Int = 2_000

        /** Cap on the problem in an escalation, where transmitting the minimum is the point. */
        const val OFFLOAD_PROBLEM_CHARS: Int = 1_200

        /** Longest word the context cut will step over before giving up and cutting mid-word. */
        private const val MAX_WORD_SCAN = 40
    }
}

/**
 * Every prompt string the app sends to a model.
 *
 * Collected in one object so the whole of what the app says on the user's behalf can be read and
 * reviewed in one screen, rather than being reconstructed from fragments scattered across builders.
 */
private object Prompts {

    const val IDENTITY_TERSE = "You answer problems written by hand in a notebook."

    const val IDENTITY_FULL =
        "You are the assistant inside a handwriting notebook. The user wrote this by hand and it was " +
            "recognized into text, so expect the occasional misread character and answer the most " +
            "plausible intended question instead of remarking on the notation."

    /**
     * The output contract, and the reason for it: an answer is rendered back onto the page as
     * handwriting strokes, so anything that needs two-dimensional layout — a table, a stacked
     * fraction, a LaTeX environment — cannot be drawn at all and comes out as literal punctuation in
     * the middle of the user's notes.
     */
    val FORMAT_TERSE =
        "Plain text only: no markdown, no tables, no LaTeX. One short step per line. " +
            "End with the line \"${PromptBuilder.ANSWER_MARKER} <result>\"."

    val FORMAT_FULL =
        "Write plain linear text. Your reply is drawn back onto the page in the user's own " +
            "handwriting, so markdown, tables, LaTeX and stacked fractions cannot be rendered at " +
            "all — write 3/4, x^2 and sqrt(2) inline. Keep each step on its own line of at most " +
            "twelve words. End with a single line \"${PromptBuilder.ANSWER_MARKER} <result>\" " +
            "holding the result and nothing else."

    const val ARITHMETIC_TERSE = "Evaluate the expression exactly. At most two lines of working."

    const val ARITHMETIC_FULL =
        "Evaluate the expression exactly. Show the intermediate values a student would write down, " +
            "and keep every figure exact unless a rounding was asked for."

    const val ALGEBRA_TERSE = "Solve for the unknown. One rearrangement per line."

    const val ALGEBRA_FULL =
        "Solve for the unknown. Name the method in a short clause, show one rearrangement per line, " +
            "and list every root when there is more than one."

    const val CALCULUS_TERSE = "Differentiate or integrate as asked. Name the rule, then the result."

    const val CALCULUS_FULL =
        "Differentiate or integrate as asked. Name the rule you apply, show the intermediate " +
            "expression, and add the constant of integration to every indefinite integral."

    const val WORD_PROBLEM_TERSE = "Pull out the numbers, compute, and answer with the unit."

    const val WORD_PROBLEM_FULL =
        "State what is given, set up the calculation, evaluate it, and give the answer with its " +
            "unit. Do not restate the problem back to the user."

    const val QUESTION_TERSE = "Answer in at most three sentences."

    /**
     * Answering the likeliest reading rather than asking for clarification is deliberate: the user
     * is looking at a page, not a chat box, and a question written back onto their notes is a dead
     * end they have to erase.
     */
    const val QUESTION_FULL =
        "Answer directly and concretely in at most four sentences. If the question is ambiguous, " +
            "answer the most likely reading rather than asking the user to clarify."

    const val PROSE_TERSE = "Continue or summarize these notes in at most three sentences."

    const val PROSE_FULL =
        "These notes contain no question. Continue them in the same voice if they trail off, or " +
            "summarize them if they look complete. At most four sentences."

    const val DEFAULT_INSTRUCTION = "Continue these notes helpfully and concisely."

    const val CONTEXT_HEADER = "Page so far:"

    const val KIND_LABEL = "Kind"
    const val LABEL_EXPRESSION = "Expression"
    const val LABEL_PROBLEM = "Problem"
    const val LABEL_QUESTION = "Question"
    const val LABEL_NOTES = "Notes"

    fun verifyLine(offlineAnswer: String): String =
        "The on-device engine answered: $offlineAnswer. Confirm or correct that, then show the working."
}
