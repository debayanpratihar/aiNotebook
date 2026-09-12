package com.debayan.ainotebook.domain.ai

import com.debayan.ainotebook.domain.math.MathProblemType
import com.debayan.ainotebook.domain.math.MathSolver
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.InferenceBackend
import com.debayan.ainotebook.domain.model.ai.InferenceConfig
import com.debayan.ainotebook.domain.model.ai.ProblemAssessment
import com.debayan.ainotebook.domain.model.ai.ProblemKind
import com.debayan.ainotebook.domain.model.ai.SolveRoute
import com.debayan.ainotebook.domain.model.ai.SolveRoute.Unavailable.FixAction
import com.debayan.ainotebook.domain.recognition.MathHeuristics
import com.debayan.ainotebook.domain.recognition.MathTokenNormalizer
import javax.inject.Inject

/**
 * The shipped [ComplexityClassifier].
 *
 * Two decisions make the routing trustworthy rather than merely plausible.
 *
 * The offline-math verdict is taken by *running* the offline engine, never by pattern-matching what
 * math looks like. [MathSolver] only knows single-variable polynomials, so `∫ sin x dx` reads as
 * calculus to any heuristic and is still not something it can answer; guessing from appearance would
 * hand the user an empty card instead of an explanation. The engine itself gets the final word.
 *
 * The string the engine accepted is then carried in [ProblemAssessment.normalizedExpression], so the
 * solve path re-uses the exact input the routing decision was made on. Re-deriving it downstream is
 * how a router and a solver end up disagreeing about whether a problem is solvable at all.
 *
 * Everything here is synchronous and allocation-light because it also runs on the solve-as-you-write
 * path, where it is re-evaluated on every recognition update.
 */
class DefaultComplexityClassifier @Inject constructor(
    private val mathSolver: MathSolver,
) : ComplexityClassifier {

    /**
     * [pageContext] is counted towards the prompt-size estimate and never classified. The
     * surrounding page decides nothing about what the user asked: a sum written under a page of
     * biology notes is still a sum, and letting the context vote would turn it into a word problem.
     */
    override fun assess(text: String, pageContext: String): ProblemAssessment {
        val problem = text.trim()
        if (problem.isEmpty()) return NOTHING_RECOGNIZED

        val kind = MathHeuristics.classify(problem)
        // Both heuristics must agree before the engine is tried at all: looksLikeMath is the cheap
        // gate that keeps a page of prose containing a date or a page number off the math path, while
        // classify decides which flavour of math it is.
        val offline = if (kind.isMathematical && MathHeuristics.looksLikeMath(problem)) {
            offlineMatch(problem)
        } else {
            null
        }

        val problemTokens = estimateTokens(problem.length)
        return ProblemAssessment(
            kind = kind,
            complexity = complexityOf(kind, problemTokens, nestingDepth(problem)),
            requiresSymbolicSolving = kind == ProblemKind.ALGEBRA || kind == ProblemKind.CALCULUS,
            estimatedPromptTokens = promptTokenEstimate(problemTokens, pageContext),
            normalizedExpression = offline?.expression,
            rationale = rationaleFor(kind, offline),
        )
    }

    /**
     * Assumes a reading worth solving. An empty page is never offered a Solve action, so there is no
     * empty-input branch here; [assess] describes that case in its rationale instead.
     */
    override fun route(
        assessment: ProblemAssessment,
        cloudAllowed: Boolean,
        cloudProvidersAvailable: Boolean,
        localModelAvailable: Boolean,
        device: DeviceCapabilities,
    ): SolveRoute {
        // The kind check is redundant for assessments this class produced — it only sets the
        // expression for mathematical kinds — but route is public and may be handed an assessment
        // built elsewhere, and an expression on a word problem must not reach the arithmetic engine.
        if (assessment.kind.isMathematical && assessment.normalizedExpression != null) {
            return SolveRoute.OfflineMath
        }

        if (cloudAllowed && cloudProvidersAvailable) {
            return SolveRoute.Cloud(UNRESOLVED_CLOUD_PROVIDER_ID, UNRESOLVED_CLOUD_PROVIDER_NAME)
        }

        if (localModelAvailable) {
            val lowMemory = device.totalRamMb < InferenceConfig.LOW_MEMORY_RAM_MB
            // Refusing up front beats grinding for a minute and returning mush: on a device this
            // small a heavy problem exhausts the context window before it reaches an answer, and the
            // user has no way to tell a slow answer from a wrong one.
            if (lowMemory && assessment.isHeavy) {
                return SolveRoute.Unavailable(Reason.TOO_HEAVY_FOR_DEVICE, FixAction.ADD_API_KEY)
            }
            return SolveRoute.Local(DEFAULT_LOCAL_BACKEND)
        }

        return when {
            // A key exists but consent was withheld for this request. ADD_API_KEY is the action
            // because it lands on the AI settings screen that holds both the cloud switch and the
            // key field; there is no ENABLE_CLOUD action, and NONE would leave a dead end.
            cloudProvidersAvailable ->
                SolveRoute.Unavailable(Reason.CLOUD_SWITCHED_OFF, FixAction.ADD_API_KEY)
            // Consent is given and nothing is configured to use it, so one paste unblocks them.
            cloudAllowed ->
                SolveRoute.Unavailable(Reason.NO_PROVIDER, FixAction.ADD_API_KEY)
            // No consent and no key: the user has said they want this to stay on the device, so the
            // honest suggestion is the on-device model rather than the one they declined.
            else ->
                SolveRoute.Unavailable(Reason.NO_ENGINE, FixAction.DOWNLOAD_MODEL)
        }
    }

    /** An input string the offline engine has already proven it accepts, and what it made of it. */
    private data class OfflineMatch(val expression: String, val type: MathProblemType)

    /**
     * Offers the engine the normalized reading first and the raw reading second.
     *
     * Two attempts rather than one because normalization is not always an improvement: it can
     * over-correct a genuine variable into a digit, while the raw reading can leave an operator the
     * parser refuses. A problem only one of them unlocks should still be answered offline rather
     * than shipped to a model, and whichever string the engine accepts is the one carried forward.
     */
    private fun offlineMatch(problem: String): OfflineMatch? {
        val candidates = listOf(MathTokenNormalizer.normalize(problem), problem)
            .filter { it.isNotBlank() }
            .distinct()
        for (candidate in candidates) {
            val solution = mathSolver.solve(candidate) ?: continue
            return OfflineMatch(candidate, solution.type)
        }
        return null
    }

    /**
     * Blends the three signals that are cheap to measure and actually predict cost: how much text
     * there is, how deeply nested it is, and whether it needs symbolic work rather than evaluation.
     *
     * Calibrated against [ProblemAssessment.TRIVIAL_THRESHOLD] and
     * [ProblemAssessment.HEAVY_THRESHOLD]: `12 × 47` lands near 0.03, `2x + 3 = 11` near 0.26, a
     * page-long word problem near 0.63, and a nested multi-step integral clear of 0.6. The kind term
     * dominates on purpose — a short question about a triple integral is harder than a long list of
     * additions, and weighting length first would get that backwards.
     */
    private fun complexityOf(kind: ProblemKind, problemTokens: Int, nestingDepth: Int): Float {
        val tokenLoad = (problemTokens / TOKEN_SATURATION).coerceAtMost(1f)
        val depthLoad = (nestingDepth / DEPTH_SATURATION).coerceAtMost(1f)
        val blended = KIND_WEIGHT * kind.symbolicLoad() +
            TOKEN_WEIGHT * tokenLoad +
            DEPTH_WEIGHT * depthLoad
        return blended.coerceIn(0f, 1f)
    }

    /** How much symbolic manipulation each kind demands, independent of how long the text is. */
    private fun ProblemKind.symbolicLoad(): Float = when (this) {
        ProblemKind.ARITHMETIC -> 0.05f
        ProblemKind.ALGEBRA -> 0.45f
        ProblemKind.CALCULUS -> 0.90f
        ProblemKind.WORD_PROBLEM -> 0.60f
        ProblemKind.FREEFORM_QUESTION -> 0.40f
        ProblemKind.PROSE -> 0.20f
    }

    /**
     * Counts the deepest bracket nesting, tolerating the lopsided brackets that recognized ink
     * routinely produces: a dropped closing paren must still score as nested rather than making the
     * whole expression unscoreable, and a stray closing paren must not push the depth negative.
     */
    private fun nestingDepth(text: String): Int {
        var depth = 0
        var deepest = 0
        for (c in text) {
            when (c) {
                '(', '[', '{' -> {
                    depth++
                    if (depth > deepest) deepest = depth
                }
                ')', ']', '}' -> if (depth > 0) depth--
            }
        }
        return deepest
    }

    /**
     * Upper bound on what the prompt will cost, counting the page context only as far as
     * [PromptBuilder.CLOUD_CONTEXT_CHARS] since that is the most any route will actually send.
     *
     * Deliberately biased high. An over-estimate costs a slightly more cautious route; an
     * under-estimate is what lets a prompt silently overflow the context window and truncate the
     * question out of its own prompt.
     */
    private fun promptTokenEstimate(problemTokens: Int, pageContext: String): Int {
        val contextChars = pageContext.trim().length.coerceAtMost(PromptBuilder.CLOUD_CONTEXT_CHARS)
        return SYSTEM_PROMPT_TOKEN_ALLOWANCE + problemTokens + estimateTokens(contextChars)
    }

    /**
     * Four characters per token, rounded up.
     *
     * The standard rule of thumb for English BPE vocabularies, and within about 15% on prose. An
     * exact count needs the target model's tokenizer, which is not loaded when a route is being
     * chosen and is not worth loading to choose one.
     */
    private fun estimateTokens(chars: Int): Int =
        (chars + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    private fun rationaleFor(kind: ProblemKind, offline: OfflineMatch?): String = when {
        offline != null -> when (offline.type) {
            MathProblemType.ARITHMETIC -> Rationale.OFFLINE_ARITHMETIC
            MathProblemType.EQUATION -> Rationale.OFFLINE_EQUATION
            MathProblemType.DERIVATIVE -> Rationale.OFFLINE_DERIVATIVE
            MathProblemType.INTEGRAL -> Rationale.OFFLINE_INTEGRAL
        }
        kind.isMathematical -> Rationale.MATH_BEYOND_OFFLINE
        kind == ProblemKind.WORD_PROBLEM -> Rationale.WORD_PROBLEM
        kind == ProblemKind.FREEFORM_QUESTION -> Rationale.QUESTION
        else -> Rationale.PROSE
    }

    companion object {
        /**
         * Identity carried by [SolveRoute.Cloud] until a provider is actually reached.
         *
         * [route] is given availability as a boolean, not the provider list, so it names the route
         * without claiming which account will serve it. The orchestrator owns failover and replaces
         * both fields with `copy()` once a provider answers; `Cloud AI` is a truthful label to show
         * in the meantime.
         */
        const val UNRESOLVED_CLOUD_PROVIDER_ID: String = ""
        const val UNRESOLVED_CLOUD_PROVIDER_NAME: String = "Cloud AI"

        /**
         * Backend named on [SolveRoute.Local] before the installed model is inspected. Matches the
         * default of [com.debayan.ainotebook.domain.model.UserPreferences.inferenceBackend]; the
         * caller, which knows the model's [com.debayan.ainotebook.domain.model.ai.ModelFormat],
         * replaces it with `format.backend`.
         */
        val DEFAULT_LOCAL_BACKEND: InferenceBackend = InferenceBackend.MEDIAPIPE

        private const val CHARS_PER_TOKEN = 4

        /**
         * Rounded up from the longest system prompt [PromptBuilder] emits — identity, task and
         * output-format rule at [PromptDetail.FULL] — so the estimate covers the scaffolding the
         * caller never sees.
         */
        private const val SYSTEM_PROMPT_TOKEN_ALLOWANCE = 200

        private const val KIND_WEIGHT = 0.55f
        private const val TOKEN_WEIGHT = 0.30f
        private const val DEPTH_WEIGHT = 0.15f

        /** Token count at which length stops adding to the score — roughly half a notebook page. */
        private const val TOKEN_SATURATION = 120f

        /** Three levels of brackets is as tangled as handwritten maths realistically gets. */
        private const val DEPTH_SATURATION = 3f

        /** Nothing was read, so nothing is classified, estimated, or sent anywhere. */
        private val NOTHING_RECOGNIZED = ProblemAssessment(
            kind = ProblemKind.PROSE,
            complexity = 0f,
            requiresSymbolicSolving = false,
            estimatedPromptTokens = 0,
            normalizedExpression = null,
            rationale = Rationale.NOTHING_RECOGNIZED,
        )
    }
}

/**
 * Every rationale the classifier can attach to an assessment, in one place because each one is
 * shown verbatim in the answer card's detail row. One sentence, no jargon, and never an apology.
 */
private object Rationale {
    const val NOTHING_RECOGNIZED = "There's nothing written here to solve yet."
    const val OFFLINE_ARITHMETIC =
        "This is a calculation the built-in math engine works out exactly, on your device."
    const val OFFLINE_EQUATION =
        "This is an equation the built-in math engine solves exactly, on your device."
    const val OFFLINE_DERIVATIVE =
        "This is a derivative the built-in math engine differentiates exactly, on your device."
    const val OFFLINE_INTEGRAL =
        "This is an integral the built-in math engine works out exactly, on your device."
    const val MATH_BEYOND_OFFLINE =
        "This is math the built-in engine can't work out on its own, so it needs an AI model."
    const val WORD_PROBLEM =
        "This is a word problem, so it needs an AI model to read the setup before any arithmetic helps."
    const val QUESTION = "This is a question rather than a calculation, so it goes to an AI model."
    const val PROSE = "These look like notes rather than a problem, so an AI model can continue them."
}

/**
 * Every dead-end explanation, kept beside the rationales for the same reason: the user reads these
 * words, and each one is paired with a call to action that genuinely unblocks them.
 */
private object Reason {
    const val TOO_HEAVY_FOR_DEVICE =
        "This problem needs more memory than an on-device model has here — it would take minutes and " +
            "still be unreliable. An API key answers it in seconds."
    const val CLOUD_SWITCHED_OFF =
        "Cloud AI is switched off and there's no on-device model installed, so nothing can answer " +
            "this yet."
    const val NO_PROVIDER =
        "The built-in math engine can't answer this one, and no AI provider is set up yet."
    const val NO_ENGINE =
        "This needs an AI model, and there's no on-device model installed or provider configured."
}
