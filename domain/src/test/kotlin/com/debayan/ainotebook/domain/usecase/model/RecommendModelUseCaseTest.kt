package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.TestDispatcherProvider
import com.debayan.ainotebook.domain.model.ai.Announcement
import com.debayan.ainotebook.domain.model.ai.ChangelogEntry
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ModelCatalog
import com.debayan.ainotebook.domain.model.ai.ModelTier
import com.debayan.ainotebook.domain.model.ai.RemoteConfig
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import com.debayan.ainotebook.domain.repository.ConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendModelUseCaseTest {

    private val dispatchers = TestDispatcherProvider()

    @Test
    fun recommends_highestTierThatFitsRecommendedRam() = runTest {
        val catalog = ModelCatalog(
            config = RemoteConfig(minAppVersionCode = 0, latestAppVersionCode = 0, recommendedModelId = null, configVersion = 1),
            models = listOf(
                model("compact", ModelTier.COMPACT, minRam = 1000, recRam = 2000),
                model("balanced", ModelTier.BALANCED, minRam = 2000, recRam = 4000),
                model("high", ModelTier.HIGH_QUALITY, minRam = 4000, recRam = 8000),
            ),
        )
        // 5 GB RAM: meets recommended for compact/balanced, but not high (needs 8 GB recommended).
        val device = capabilities(totalRamMb = 5000)
        val useCase = RecommendModelUseCase(FakeConfigRepository(catalog), FakeProvider(device), dispatchers)

        val recommended = (useCase(Unit) as AppResult.Success).data

        assertEquals("balanced", recommended?.id)
    }

    private class FakeProvider(private val caps: DeviceCapabilities) : DeviceCapabilityProvider {
        override suspend fun capabilities(): DeviceCapabilities = caps
    }

    private class FakeConfigRepository(private val catalog: ModelCatalog) : ConfigRepository {
        override suspend fun getCatalog(): AppResult<ModelCatalog> = AppResult.Success(catalog)
        override suspend fun getAnnouncements(): AppResult<List<Announcement>> = AppResult.Success(emptyList())
        override suspend fun getChangelog(): AppResult<List<ChangelogEntry>> = AppResult.Success(emptyList())
    }

    private fun capabilities(totalRamMb: Long) = DeviceCapabilities(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        freeStorageBytes = 10_000_000_000,
        supportedAbis = listOf("arm64-v8a"),
        sdkInt = 34,
        cpuCores = 8,
    )

    private fun model(id: String, tier: ModelTier, minRam: Int, recRam: Int) = RemoteModel(
        id = id,
        name = id,
        version = "1.0",
        provider = "Test",
        tier = tier,
        quantization = "Q4_K_M",
        fileName = "$id.gguf",
        sizeBytes = 1_000_000_000,
        sha256 = "",
        downloadUrl = "https://example.com/$id.gguf",
        minRamMb = minRam,
        recommendedRamMb = recRam,
        minSdk = 26,
        supportedAbis = emptyList(),
        description = "",
    )
}
