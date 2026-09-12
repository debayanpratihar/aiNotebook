package com.debayan.ainotebook.domain.model.ai

/**
 * Wire protocol of a cloud AI provider.
 *
 * Deliberately a protocol rather than a vendor: the overwhelming majority of providers (OpenAI,
 * Groq, OpenRouter, Together, DeepSeek, Mistral, Fireworks, and every self-hosted LM Studio / Ollama
 * / vLLM endpoint) speak the same `/chat/completions` shape, so one client covers them all and the
 * user only has to supply a base URL, a model id, and a key. Gemini and Anthropic get their own
 * entries because their request/response bodies genuinely differ.
 */
enum class AiProviderKind {
    /** `POST {baseUrl}/chat/completions`, `Authorization: Bearer <key>`, SSE `data:` deltas. */
    OPENAI_COMPATIBLE,

    /** `POST {baseUrl}/models/{model}:streamGenerateContent`, key via `x-goog-api-key`. */
    GEMINI,

    /** `POST {baseUrl}/messages`, key via `x-api-key` + `anthropic-version`. */
    ANTHROPIC,
    ;

    /** Label shown in the provider picker. */
    val displayName: String
        get() = when (this) {
            OPENAI_COMPATIBLE -> "OpenAI-compatible"
            GEMINI -> "Google Gemini"
            ANTHROPIC -> "Anthropic Claude"
        }

    /**
     * Help text under the provider picker. Written for someone pasting a key from a dashboard, not
     * for someone who already knows the protocol.
     */
    val hint: String
        get() = when (this) {
            OPENAI_COMPATIBLE ->
                "Works with OpenAI, Groq, OpenRouter, Together, DeepSeek, Mistral, and any " +
                    "self-hosted server that exposes /chat/completions."
            GEMINI -> "Use a Google AI Studio key. Free tier available."
            ANTHROPIC -> "Use an Anthropic Console key."
        }

    /** Pre-filled base URL when the user picks this kind; they can always override it. */
    val defaultBaseUrl: String
        get() = when (this) {
            OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
            GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
            ANTHROPIC -> "https://api.anthropic.com/v1"
        }

    /** Pre-filled model id, chosen to be a cheap/fast default rather than the flagship. */
    val defaultModelId: String
        get() = when (this) {
            OPENAI_COMPATIBLE -> "gpt-4o-mini"
            GEMINI -> "gemini-2.0-flash"
            ANTHROPIC -> "claude-3-5-haiku-latest"
        }

    /** Where to get a key, surfaced as a tappable link in the add-provider sheet. */
    val keyConsoleUrl: String
        get() = when (this) {
            OPENAI_COMPATIBLE -> "https://platform.openai.com/api-keys"
            GEMINI -> "https://aistudio.google.com/apikey"
            ANTHROPIC -> "https://console.anthropic.com/settings/keys"
        }

    companion object {
        /**
         * Presets offered as one-tap choices in the add-provider sheet, so the common case is a name
         * and a key rather than a URL the user has to look up. Each is just a pre-filled
         * [AiProviderKind] + base URL + model id.
         */
        val presets: List<ProviderPreset> = listOf(
            ProviderPreset(
                label = "Groq",
                kind = OPENAI_COMPATIBLE,
                baseUrl = "https://api.groq.com/openai/v1",
                modelId = "llama-3.3-70b-versatile",
                keyConsoleUrl = "https://console.groq.com/keys",
                note = "Free tier, very fast. Recommended for solving written problems.",
            ),
            ProviderPreset(
                label = "Google Gemini",
                kind = GEMINI,
                baseUrl = GEMINI.defaultBaseUrl,
                modelId = GEMINI.defaultModelId,
                keyConsoleUrl = GEMINI.keyConsoleUrl,
                note = "Free tier available.",
            ),
            ProviderPreset(
                label = "OpenAI",
                kind = OPENAI_COMPATIBLE,
                baseUrl = OPENAI_COMPATIBLE.defaultBaseUrl,
                modelId = OPENAI_COMPATIBLE.defaultModelId,
                keyConsoleUrl = OPENAI_COMPATIBLE.keyConsoleUrl,
                note = "Paid. Uses your own usage quota.",
            ),
            ProviderPreset(
                label = "Anthropic Claude",
                kind = ANTHROPIC,
                baseUrl = ANTHROPIC.defaultBaseUrl,
                modelId = ANTHROPIC.defaultModelId,
                keyConsoleUrl = ANTHROPIC.keyConsoleUrl,
                note = "Paid. Strong step-by-step explanations.",
            ),
            ProviderPreset(
                label = "OpenRouter",
                kind = OPENAI_COMPATIBLE,
                baseUrl = "https://openrouter.ai/api/v1",
                modelId = "meta-llama/llama-3.3-70b-instruct",
                keyConsoleUrl = "https://openrouter.ai/keys",
                note = "One key, many models. Some are free.",
            ),
            ProviderPreset(
                label = "Other / self-hosted",
                kind = OPENAI_COMPATIBLE,
                baseUrl = "",
                modelId = "",
                keyConsoleUrl = "",
                note = "Paste any endpoint that exposes /chat/completions, e.g. Ollama or LM Studio.",
            ),
        )
    }
}

/** A one-tap starting point in the add-provider sheet. Every field stays user-editable. */
data class ProviderPreset(
    val label: String,
    val kind: AiProviderKind,
    val baseUrl: String,
    val modelId: String,
    val keyConsoleUrl: String,
    val note: String,
)
