package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.RemoteModel
import com.debayan.ainotebook.domain.repository.DownloadRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Gates a model download on device compatibility, then enqueues it. An incompatible model is
 * rejected with the reason from the compatibility check rather than starting a download that can't run.
 */
class StartModelDownloadUseCase @Inject constructor(
    private val checkCompatibility: CheckModelCompatibilityUseCase,
    private val downloadRepository: DownloadRepository,
    dispatchers: DispatcherProvider,
) : UseCase<StartModelDownloadUseCase.Params, Unit>(dispatchers.io) {

    data class Params(
        val model: RemoteModel,
        val allowMeteredNetwork: Boolean = false,
    )

    override suspend fun execute(params: Params): AppResult<Unit> {
        val compatibility = when (val result = checkCompatibility(params.model)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        if (!compatibility.isCompatible) {
            return AppResult.Failure(
                AppError.Validation(compatibility.reason ?: "This model is not compatible with your device"),
            )
        }
        return downloadRepository.startDownload(params.model, params.allowMeteredNetwork)
    }
}
