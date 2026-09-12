package com.debayan.ainotebook.domain.model.ai

/** What kind of problem the user wrote, as judged before any model runs. */
enum class ProblemKind {
    /** Pure numeric evaluation: `12 × 47 + 3`. */
    ARITHMETIC,

    /** Solve-for-x, factoring, simplification — symbolic but closed-form. */
    ALGEBRA,

    /** Derivatives, integrals, limits, series. */
    CALCULUS,

    /** Prose that contains a computation: "if a train leaves at…". */
    WORD_PROBLEM,

    /** A question with no computation in it. */
    FREEFORM_QUESTION,

    /** Notes with no question at all — summarize or continue rather than solve. */
    PROSE,
    ;

    /** Whether the offline math engine can plausibly handle this without any model. */
    val isMathematical: Boolean get() = this == ARITHMETIC || this == ALGEBRA || this == CALCULUS
}

/** Where a problem should be solved. */
sealed interface SolveRoute {

    /**
     * The offline math engine. Instant, exact, needs no model and no network, and works on the
     * cheapest device the app supports. Always preferred when it applies.
     */
    data object OfflineMath : SolveRoute

    /** A cloud provider, identified so the UI can attribute the answer. */
    data class Cloud(val providerId: String, val providerName: String) : SolveRoute

    /** The on-device language model. */
    data class Local(val backend: InferenceBackend) : SolveRoute

    /**
     * Nothing can serve this request. [reason] is written for the user, and [fixAction] tells the UI
     * which call-to-action to offer, so a dead end always comes with a way out.
     */
    data class Unavailable(val reason: String, val fixAction: FixAction) : SolveRoute {
        enum class FixAction { ADD_API_KEY, DOWNLOAD_MODEL, GO_ONLINE, NONE }
    }
}

/**
 * The classifier's verdict on one problem.
 *
 * [complexity] is 0..1 and blends token count, operator depth, and whether symbolic manipulation is
 * required. It exists to keep a trivial sum from being shipped to a 70B model and a multi-step
 * proof from being handed to a 0.5B one.
 */
data class ProblemAssessment(
    val kind: ProblemKind,
    val complexity: Float,
    val requiresSymbolicSolving: Boolean,
    val estimatedPromptTokens: Int,
    /** Normalized expression when the text parsed as math, ready for the offline engine. */
    val normalizedExpression: String? = null,
    /** Plain-language explanation of the routing decision, shown in the answer card's detail row. */
    val rationale: String = "",
) {
    val isTrivial: Boolean get() = complexity < TRIVIAL_THRESHOLD

    val isHeavy: Boolean get() = complexity >= HEAVY_THRESHOLD

    companion object {
        const val TRIVIAL_THRESHOLD: Float = 0.2f
        const val HEAVY_THRESHOLD: Float = 0.6f
    }
}
