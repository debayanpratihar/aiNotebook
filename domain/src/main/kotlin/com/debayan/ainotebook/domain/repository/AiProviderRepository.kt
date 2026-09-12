package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.AiProviderConfig
import com.debayan.ainotebook.domain.model.ai.ProviderHealth
import kotlinx.coroutines.flow.Flow

/**
 * Stores the user's cloud AI providers and their keys.
 *
 * Config rows live in the app database; keys live only in the platform keystore, keyed by provider
 * id. That split is why [upsert] takes the key as a separate argument and why nothing else on this
 * interface can hand a secret back to the caller except [apiKeyFor], which exists solely for the
 * request builder.
 */
interface AiProviderRepository {

    /** All configured providers in failover order (usable first, then by priority, then health). */
    fun observeProviders(): Flow<List<AiProviderConfig>>

    /**
     * Creates or updates a provider. [apiKey] is written to the keystore only when non-null; pass
     * null to edit a provider's URL or model without touching the stored key, and pass an empty
     * string to clear it.
     */
    suspend fun upsert(config: AiProviderConfig, apiKey: String?): AppResult<Unit>

    /** Removes the provider and wipes its key from the keystore. */
    suspend fun delete(id: String): AppResult<Unit>

    suspend fun setEnabled(id: String, enabled: Boolean): AppResult<Unit>

    /** Persists a new failover order. */
    suspend fun reorder(idsInPriorityOrder: List<String>): AppResult<Unit>

    /** Live round-trip against the provider; also refreshes its stored health. */
    suspend fun testConnection(id: String): AppResult<ProviderHealth>

    /** Providers that can serve a request right now, in the order failover should try them. */
    suspend fun usableProviders(): List<AiProviderConfig>

    /** The key for [id], or null when none is stored. Data layer only. */
    suspend fun apiKeyFor(id: String): String?

    suspend fun recordSuccess(id: String, latencyMs: Long)

    suspend fun recordFailure(id: String, message: String)
}
