package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.ModelRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Makes a model the active one and records that it was used. */
class ActivateModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, Unit>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<Unit> =
        when (val result = modelRepository.setActiveModel(params)) {
            is AppResult.Failure -> result
            is AppResult.Success -> modelRepository.markUsed(params)
        }
}
