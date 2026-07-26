package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.ModelCatalog
import com.debayan.ainotebook.domain.repository.ConfigRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Fetches the available-model catalog from the remote configuration. */
class GetModelCatalogUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    dispatchers: DispatcherProvider,
) : UseCase<Unit, ModelCatalog>(dispatchers.io) {

    override suspend fun execute(params: Unit): AppResult<ModelCatalog> =
        configRepository.getCatalog()
}
