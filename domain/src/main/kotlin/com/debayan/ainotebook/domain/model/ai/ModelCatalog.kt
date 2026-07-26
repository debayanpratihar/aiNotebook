package com.debayan.ainotebook.domain.model.ai

/** The remote configuration plus the list of available models. */
data class ModelCatalog(
    val config: RemoteConfig,
    val models: List<RemoteModel>,
)
