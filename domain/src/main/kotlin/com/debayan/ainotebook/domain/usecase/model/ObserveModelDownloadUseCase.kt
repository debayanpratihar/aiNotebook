package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.repository.DownloadRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams download progress for a specific model (null when there is no download for it). */
class ObserveModelDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<String, ModelDownloadProgress?>(dispatchers.io) {

    override fun execute(params: String): Flow<ModelDownloadProgress?> =
        downloadRepository.observeDownload(params)
}
