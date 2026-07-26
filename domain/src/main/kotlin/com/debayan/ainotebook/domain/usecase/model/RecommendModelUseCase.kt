package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ModelCatalog
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import com.debayan.ainotebook.domain.repository.ConfigRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Picks the best model for this device: the highest-tier model whose recommended RAM the device
 * meets; failing that, the highest-tier model that at least meets minimum RAM. Honors the config's
 * explicit recommendation when that model is itself compatible. Returns null if nothing fits.
 */
class RecommendModelUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    dispatchers: DispatcherProvider,
) : UseCase<Unit, RemoteModel?>(dispatchers.io) {

    override suspend fun execute(params: Unit): AppResult<RemoteModel?> {
        val catalog: ModelCatalog = when (val result = configRepository.getCatalog()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val device = deviceCapabilityProvider.capabilities()
        return AppResult.Success(pickBest(catalog, device))
    }

    private fun pickBest(catalog: ModelCatalog, device: DeviceCapabilities): RemoteModel? {
        val compatible = catalog.models.filter { model ->
            device.totalRamMb >= model.minRamMb &&
                device.sdkInt >= model.minSdk &&
                (model.supportedAbis.isEmpty() || model.supportedAbis.any { it in device.supportedAbis })
        }
        if (compatible.isEmpty()) return null

        catalog.config.recommendedModelId
            ?.let { id -> compatible.firstOrNull { it.id == id } }
            ?.let { return it }

        return compatible.filter { device.totalRamMb >= it.recommendedRamMb }
            .maxByOrNull { it.tier.rank }
            ?: compatible.maxByOrNull { it.tier.rank }
    }
}
