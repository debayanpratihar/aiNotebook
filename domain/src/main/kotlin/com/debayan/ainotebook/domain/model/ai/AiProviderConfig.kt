package com.debayan.ainotebook.domain.model.ai

/**
 * A cloud AI provider the user has configured.
 *
 * **The API key is deliberately absent from this model.** Keys live only in the platform keystore
 * and are fetched, by id, at the moment a request is built. Everything above the data layer —
 * view models, UI state, logs, crash reports, the export format — handles this type freely and can
 * never leak a secret. [hasKey] is all the UI needs to render "configured" vs "needs a key".
 *
 * [priority] orders failover: the lowest enabled provider with a key is tried first, and the next
 * one takes over on rate-limit / server-error / timeout.
 */
data class AiProviderConfig(
    val id: String,
    /** User-facing label, e.g. "Groq (personal)". Free text so two keys for one vendor are legible. */
    val displayName: String,
    val kind: AiProviderKind,
    /** Base URL without a trailing slash, e.g. `https://api.groq.com/openai/v1`. */
    val baseUrl: String,
    val modelId: String,
    val enabled: Boolean = true,
    /** Lower is tried first. */
    val priority: Int = 0,
    /** True when a key is present in the keystore for this provider. Never the key itself. */
    val hasKey: Boolean = false,
    val maxOutputTokens: Int = 1024,
    /** Extra headers some gateways require (e.g. OpenRouter's `HTTP-Referer`). Never secrets. */
    val extraHeaders: Map<String, String> = emptyMap(),
    val createdAt: Long = 0L,
    // --- Health, updated by the failover coordinator so the UI can show why a provider is skipped.
    val lastSuccessAt: Long? = null,
    val lastLatencyMs: Long? = null,
    val lastErrorMessage: String? = null,
    val consecutiveFailures: Int = 0,
) {
    /**
     * Whether this provider can actually serve a request right now. A provider with no key is shown
     * in the list (so the user can finish setting it up) but never selected.
     */
    val isUsable: Boolean get() = enabled && hasKey && baseUrl.isNotBlank() && modelId.isNotBlank()

    /**
     * Providers that keep failing sort last instead of being disabled, so a transient outage can't
     * permanently retire a key the user still wants. [MAX_TRACKED_FAILURES] caps the penalty so a
     * recovered provider climbs back quickly.
     */
    val healthPenalty: Int get() = consecutiveFailures.coerceAtMost(MAX_TRACKED_FAILURES)

    companion object {
        const val MAX_TRACKED_FAILURES: Int = 3

        /** Strips a trailing slash so URL joining stays predictable across providers. */
        fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/')
    }
}
