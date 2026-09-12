package com.debayan.ainotebook.domain.model.ai

/**
 * A request to solve what the user wrote.
 *
 * [recognizedText] is the corrected reading, not raw ink: correction happens before solving so the
 * model is never asked the wrong question. [allowCloud] carries the user's consent for this specific
 * request, which is why it is a per-request flag rather than only a stored preference — a one-tap
 * "escalate to cloud" must not silently change the global setting.
 */
data class SolveRequest(
    val recognizedText: String,
    val pageContext: String = "",
    val allowCloud: Boolean,
    val allowLocalModel: Boolean = true,
    /** Skip the response cache; set by an explicit "regenerate". */
    val forceFresh: Boolean = false,
    val params: AiGenerationParams = AiGenerationParams.DEFAULT,
)

/**
 * Streamed progress of one solve.
 *
 * Deliberately richer than [AiGenerationState]: it carries the instant offline answer
 * ([QuickAnswer]) separately from the full explanation, because showing *something* correct in under
 * a second is what makes the app feel fast, and it names the [SolveRoute] that served the answer so
 * the user always knows whether their content left the device.
 */
sealed interface SolveState {

    data object Idle : SolveState

    /** Classifying and routing. Sub-perceptual in the normal case. */
    data object Analyzing : SolveState

    /**
     * An exact answer from the offline math engine, available immediately. Steps may still be
     * streaming in behind it, and a cloud explanation may follow if the user asks for one.
     */
    data class QuickAnswer(
        val answer: String,
        val steps: List<String>,
        val expression: String,
    ) : SolveState

    /** A route was chosen and work has started. */
    data class Working(val route: SolveRoute, val label: String) : SolveState

    /** Tokens arriving; [text] is the accumulated answer. */
    data class Streaming(val route: SolveRoute, val text: String) : SolveState

    /** Terminal success. */
    data class Done(
        val route: SolveRoute,
        val text: String,
        val usage: CloudUsage? = null,
        val servedFromCache: Boolean = false,
        val elapsedMs: Long = 0L,
    ) : SolveState

    /**
     * Terminal failure. [recoverable] distinguishes "try again" from "you need to configure
     * something", and [route] records how far it got before giving up.
     */
    data class Failed(
        val message: String,
        val route: SolveRoute? = null,
        val recoverable: Boolean = true,
        val fixAction: SolveRoute.Unavailable.FixAction = SolveRoute.Unavailable.FixAction.NONE,
    ) : SolveState
}
