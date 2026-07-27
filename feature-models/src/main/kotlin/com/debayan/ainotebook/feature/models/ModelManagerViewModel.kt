package com.debayan.ainotebook.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.ModelCompatibility
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.repository.SettingsRepository
import com.debayan.ainotebook.domain.usecase.model.ActivateModelUseCase
import com.debayan.ainotebook.domain.usecase.model.CancelModelDownloadUseCase
import com.debayan.ainotebook.domain.usecase.model.CheckModelCompatibilityUseCase
import com.debayan.ainotebook.domain.usecase.model.DeleteModelUseCase
import com.debayan.ainotebook.domain.usecase.model.GetModelCatalogUseCase
import com.debayan.ainotebook.domain.usecase.model.ObserveActiveDownloadsUseCase
import com.debayan.ainotebook.domain.usecase.model.ObserveActiveModelUseCase
import com.debayan.ainotebook.domain.usecase.model.ObserveInstalledModelsUseCase
import com.debayan.ainotebook.domain.usecase.model.StartModelDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val getModelCatalog: GetModelCatalogUseCase,
    private val checkCompatibility: CheckModelCompatibilityUseCase,
    private val startModelDownload: StartModelDownloadUseCase,
    private val cancelModelDownload: CancelModelDownloadUseCase,
    private val activateModel: ActivateModelUseCase,
    private val deleteModel: DeleteModelUseCase,
    private val settingsRepository: SettingsRepository,
    observeInstalledModels: ObserveInstalledModelsUseCase,
    observeActiveModel: ObserveActiveModelUseCase,
    observeActiveDownloads: ObserveActiveDownloadsUseCase,
) : ViewModel() {

    private data class CatalogData(
        val models: List<Pair<RemoteModel, ModelCompatibility>>,
        val recommendedId: String?,
    )

    private val catalog = MutableStateFlow<CatalogData?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModelManagerUiState> = combine(
        catalog,
        observeInstalledModels(Unit),
        observeActiveModel(Unit),
        observeActiveDownloads(Unit),
        error,
    ) { catalogData, installed, active, downloads, errorMessage ->
        val downloadsById = downloads.associateBy { it.modelId }
        val installedIds = installed.mapTo(HashSet()) { it.id }
        val available = catalogData?.models?.map { (model, compatibility) ->
            AvailableModelUi(
                model = model,
                compatibility = compatibility,
                isInstalled = model.id in installedIds,
                download = downloadsById[model.id],
                isRecommended = model.id == catalogData.recommendedId,
            )
        }.orEmpty()

        ModelManagerUiState(
            isLoading = catalogData == null && errorMessage == null,
            available = available,
            installed = installed,
            activeModelId = active?.id,
            errorMessage = errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ModelManagerUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            error.value = null
            catalog.value = null
            when (val result = getModelCatalog(Unit)) {
                is AppResult.Success -> {
                    val models = result.data.models.map { model ->
                        model to compatibilityOf(model)
                    }
                    catalog.value = CatalogData(models, result.data.config.recommendedModelId)
                }

                is AppResult.Failure ->
                    error.value = "Couldn't load models. Check your connection and try again."
            }
        }
    }

    fun onDownload(model: RemoteModel) {
        viewModelScope.launch {
            val allowMetered = !settingsRepository.userPreferences.first().wifiOnlyDownloads
            val result = startModelDownload(StartModelDownloadUseCase.Params(model, allowMetered))
            if (result is AppResult.Failure) error.value = result.error.message
        }
    }

    fun onCancelDownload(modelId: String) {
        viewModelScope.launch { cancelModelDownload(modelId) }
    }

    fun onActivate(modelId: String) {
        viewModelScope.launch {
            if (activateModel(modelId) is AppResult.Failure) error.value = "Couldn't activate model."
        }
    }

    fun onDelete(modelId: String) {
        viewModelScope.launch {
            if (deleteModel(modelId) is AppResult.Failure) error.value = "Couldn't delete model."
        }
    }

    fun consumeError() {
        error.value = null
    }

    private suspend fun compatibilityOf(model: RemoteModel): ModelCompatibility =
        when (val result = checkCompatibility(model)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> ModelCompatibility(
                isCompatible = false,
                meetsMinRam = false,
                meetsRecommendedRam = false,
                hasEnoughStorage = false,
                abiSupported = false,
                sdkSupported = false,
                reason = "Could not check compatibility",
            )
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
