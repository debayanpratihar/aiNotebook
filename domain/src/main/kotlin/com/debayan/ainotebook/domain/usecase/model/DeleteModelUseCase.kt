package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.ModelRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Removes an installed model (registry entry and its file). */
class DeleteModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, Unit>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<Unit> =
        modelRepository.deleteModel(params)
}
