package com.debayan.ainotebook.domain.model.ai

/**
 * A provider-neutral completion request. Each [com.debayan.ainotebook.domain.provider.CloudInferenceClient]
 * translates this into its own wire format, so the orchestrator never branches on vendor.
 */
data class CloudCompletionRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val maxOutputTokens: Int = 1024,
    val temperature: Float = 0.2f,
    val topP: Float = 0.95f,
    /**
     * Streaming is on by default because first-token latency is what the user perceives as speed;
     * the health probe turns it off to keep its test request as small as possible.
     */
    val stream: Boolean = true,
)

/** One event in a streaming cloud completion. */
sealed interface CloudEvent {
    /** An incremental chunk of generated text. */
    data class Delta(val text: String) : CloudEvent

    /** Terminal event carrying token accounting when the provider reports it. */
    data class Done(val usage: CloudUsage?) : CloudEvent
}

/**
 * Token accounting for one cloud call. Providers report this inconsistently (some only on the final
 * SSE frame, some not at all), so every field is nullable and the UI degrades to "—" rather than
 * showing a wrong number.
 */
data class CloudUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
) {
    val totalTokens: Int? =
        if (promptTokens == null && completionTokens == null) null
        else (promptTokens ?: 0) + (completionTokens ?: 0)
}

/** Result of probing a provider: used to order failover and to back the "Test connection" button. */
data class ProviderHealth(
    val providerId: String,
    val reachable: Boolean,
    val latencyMs: Long?,
    val detail: String,
)

/**
 * Why a cloud attempt failed, classified by whether trying the *next* provider could plausibly help.
 *
 * This distinction is the whole point of the type: rotating to another key on a 429 or a 503 is
 * correct, but rotating on a 401 or a malformed model id just burns every key the user has and
 * reports the wrong problem back to them.
 */
sealed interface CloudFailure {
    /** Whether failover to the next provider is worth attempting. */
    val isTransient: Boolean

    val message: String

    /** 429 — quota or rate limit. Failover, and prefer a different account. */
    data class RateLimited(override val message: String, val retryAfterSeconds: Long?) : CloudFailure {
        override val isTransient: Boolean get() = true
    }

    /** 5xx — provider-side fault. Failover. */
    data class ServerError(override val message: String, val statusCode: Int) : CloudFailure {
        override val isTransient: Boolean get() = true
    }

    /** Socket/DNS/read timeout. Failover. */
    data class Network(override val message: String) : CloudFailure {
        override val isTransient: Boolean get() = true
    }

    /** 401/403 — the key itself is bad. Do not failover; tell the user to fix the key. */
    data class Unauthorized(override val message: String) : CloudFailure {
        override val isTransient: Boolean get() = false
    }

    /** 400/404 — bad model id, bad URL, or an unparseable response. Do not failover. */
    data class BadRequest(override val message: String, val statusCode: Int?) : CloudFailure {
        override val isTransient: Boolean get() = false
    }
}
