package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.remote.config.ConfigService
import com.debayan.ainotebook.data.remote.config.toDomain
import com.debayan.ainotebook.domain.model.ai.Announcement
import com.debayan.ainotebook.domain.model.ai.ChangelogEntry
import com.debayan.ainotebook.domain.model.ai.ModelCatalog
import com.debayan.ainotebook.domain.repository.ConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [ConfigRepository] backed by [ConfigService]; failures become [AppError.Network]. */
class ConfigRepositoryImpl @Inject constructor(
    private val service: ConfigService,
    private val dispatchers: DispatcherProvider,
) : ConfigRepository {

    override suspend fun getCatalog(): AppResult<ModelCatalog> = networkCall {
        ModelCatalog(
            config = service.fetchConfig().toDomain(),
            models = service.fetchModels().map { it.toDomain() },
        )
    }

    override suspend fun getAnnouncements(): AppResult<List<Announcement>> = networkCall {
        service.fetchAnnouncements().map { it.toDomain() }
    }

    override suspend fun getChangelog(): AppResult<List<ChangelogEntry>> = networkCall {
        service.fetchChangelog().map { it.toDomain() }
    }

    private suspend fun <T> networkCall(block: () -> T): AppResult<T> =
        withContext(dispatchers.io) {
            try {
                AppResult.Success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(AppError.Network(throwable.message, throwable))
            }
        }
}
