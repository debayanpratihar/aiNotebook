package com.debayan.ainotebook.domain.ai

import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ProblemAssessment
import com.debayan.ainotebook.domain.model.ai.SolveRoute

/**
 * Decides what a problem is and where it should be solved, before anything expensive runs.
 *
 * Heuristic and synchronous by design. Using a model to decide whether to use a model would add the
 * very latency this exists to avoid, and the signals that matter — operator depth, symbolic
 * requirements, token count, available RAM — are all cheaply measurable.
 */
interface ComplexityClassifier {

    /** Classifies [text] without deciding a route. */
    fun assess(text: String, pageContext: String = ""): ProblemAssessment

    /**
     * Chooses a route for [assessment] given what is actually available right now.
     *
     * Ordering follows the product rule that math is always answered offline and everything else
     * prefers the cloud when the user has configured a key: the offline engine is exact and instant,
     * whereas a 0.5B model on a 4 GB phone is neither. The local model is the fallback for when
     * there is no key or no network, not the default.
     */
    fun route(
        assessment: ProblemAssessment,
        cloudAllowed: Boolean,
        cloudProvidersAvailable: Boolean,
        localModelAvailable: Boolean,
        device: DeviceCapabilities,
    ): SolveRoute
}
