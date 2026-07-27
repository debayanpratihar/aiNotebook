package com.debayan.ainotebook.feature.models

import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.model.ai.ModelCompatibility
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.model.ai.RemoteModel

/** A catalog model plus its device compatibility, install state, and any active download. */
data class AvailableModelUi(
    val model: RemoteModel,
    val compatibility: ModelCompatibility,
    val isInstalled: Boolean,
    val download: ModelDownloadProgress?,
    val isRecommended: Boolean,
)

data class ModelManagerUiState(
    val isLoading: Boolean = true,
    val available: List<AvailableModelUi> = emptyList(),
    val installed: List<InstalledModel> = emptyList(),
    val activeModelId: String? = null,
    val errorMessage: String? = null,
)
