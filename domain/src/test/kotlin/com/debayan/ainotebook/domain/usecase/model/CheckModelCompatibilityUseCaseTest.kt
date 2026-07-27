package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.TestDispatcherProvider
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ModelTier
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckModelCompatibilityUseCaseTest {

    private val dispatchers = TestDispatcherProvider()

    @Test
    fun compatible_whenDeviceMeetsEveryRequirement() = runTest {
        val device = capabilities(totalRamMb = 6000, freeStorage = 5_000_000_000, abis = listOf("arm64-v8a"), sdk = 33)
        val useCase = CheckModelCompatibilityUseCase(FakeProvider(device), dispatchers)

        val result = useCase(model(minRam = 2000, recRam = 4000, sizeBytes = 1_000_000_000))

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isCompatible)
    }

    @Test
    fun incompatible_whenRamBelowMinimum() = runTest {
        val device = capabilities(totalRamMb = 1500, freeStorage = 5_000_000_000, abis = listOf("arm64-v8a"), sdk = 33)
        val useCase = CheckModelCompatibilityUseCase(FakeProvider(device), dispatchers)

        val data = (useCase(model(minRam = 4000)) as AppResult.Success).data

        assertFalse(data.isCompatible)
        assertFalse(data.meetsMinRam)
    }

    @Test
    fun incompatible_whenAbiUnsupported() = runTest {
        val device = capabilities(totalRamMb = 8000, freeStorage = 5_000_000_000, abis = listOf("x86"), sdk = 33)
        val useCase = CheckModelCompatibilityUseCase(FakeProvider(device), dispatchers)

        val data = (useCase(model(abis = listOf("arm64-v8a"))) as AppResult.Success).data

        assertFalse(data.isCompatible)
        assertFalse(data.abiSupported)
    }

    private class FakeProvider(private val caps: DeviceCapabilities) : DeviceCapabilityProvider {
        override suspend fun capabilities(): DeviceCapabilities = caps
    }

    private fun capabilities(totalRamMb: Long, freeStorage: Long, abis: List<String>, sdk: Int) =
        DeviceCapabilities(
            totalRamMb = totalRamMb,
            availableRamMb = totalRamMb / 2,
            freeStorageBytes = freeStorage,
            supportedAbis = abis,
            sdkInt = sdk,
            cpuCores = 8,
        )

    private fun model(
        minRam: Int = 2000,
        recRam: Int = 4000,
        sizeBytes: Long = 1_000_000_000,
        abis: List<String> = emptyList(),
        minSdk: Int = 26,
    ) = RemoteModel(
        id = "m1",
        name = "Test model",
        version = "1.0",
        provider = "Test",
        tier = ModelTier.BALANCED,
        quantization = "Q4_K_M",
        fileName = "m1.gguf",
        sizeBytes = sizeBytes,
        sha256 = "",
        downloadUrl = "https://example.com/m1.gguf",
        minRamMb = minRam,
        recommendedRamMb = recRam,
        minSdk = minSdk,
        supportedAbis = abis,
        description = "",
    )
}
