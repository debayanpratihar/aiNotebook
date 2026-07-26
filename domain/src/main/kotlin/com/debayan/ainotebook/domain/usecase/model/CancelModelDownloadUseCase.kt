package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.DownloadRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Cancels an in-progress download (the partial file is retained for later resume). */
class CancelModelDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, Unit>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<Unit> =
        downloadRepository.cancelDownload(params)
}
