package com.debayan.ainotebook.domain.model.ai

/**
 * Application-level remote configuration (config.json): version gating and the id of the model the
 * config service currently recommends. Configuration updates ship independently of app updates.
 */
data class RemoteConfig(
    val minAppVersionCode: Int,
    val latestAppVersionCode: Int,
    val recommendedModelId: String?,
    val configVersion: Int,
)
