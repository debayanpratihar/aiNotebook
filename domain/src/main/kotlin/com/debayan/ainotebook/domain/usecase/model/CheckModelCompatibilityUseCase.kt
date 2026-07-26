package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ModelCompatibility
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Checks a [RemoteModel] against the current device. Requires ABI, SDK, RAM (minimum) and free
 * storage (with headroom); recommended RAM is reported separately so the UI can warn about
 * degraded performance without blocking the install.
 */
class CheckModelCompatibilityUseCase @Inject constructor(
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    dispatchers: DispatcherProvider,
) : UseCase<RemoteModel, ModelCompatibility>(dispatchers.default) {

    override suspend fun execute(params: RemoteModel): AppResult<ModelCompatibility> {
        val device = deviceCapabilityProvider.capabilities()
        return AppResult.Success(evaluate(params, device))
    }

    private fun evaluate(model: RemoteModel, device: DeviceCapabilities): ModelCompatibility {
        val meetsMinRam = device.totalRamMb >= model.minRamMb
        val meetsRecommendedRam = device.totalRamMb >= model.recommendedRamMb
        val hasEnoughStorage = device.freeStorageBytes >= model.sizeBytes + STORAGE_HEADROOM_BYTES
        val abiSupported = model.supportedAbis.isEmpty() ||
            model.supportedAbis.any { it in device.supportedAbis }
        val sdkSupported = device.sdkInt >= model.minSdk

        val reason = when {
            !sdkSupported -> "Requires Android API ${model.minSdk} or newer"
            !abiSupported -> "Your device's CPU architecture is not supported"
            !meetsMinRam -> "Needs at least ${model.minRamMb} MB of RAM"
            !hasEnoughStorage -> "Not enough free storage to install this model"
            else -> null
        }

        return ModelCompatibility(
            isCompatible = meetsMinRam && hasEnoughStorage && abiSupported && sdkSupported,
            meetsMinRam = meetsMinRam,
            meetsRecommendedRam = meetsRecommendedRam,
            hasEnoughStorage = hasEnoughStorage,
            abiSupported = abiSupported,
            sdkSupported = sdkSupported,
            reason = reason,
        )
    }

    private companion object {
        /** Extra free space required beyond the model size, so installation never fills the disk. */
        const val STORAGE_HEADROOM_BYTES = 200L * 1024 * 1024
    }
}
