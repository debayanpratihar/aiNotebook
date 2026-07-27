package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.repository.DownloadRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams progress for all in-flight model downloads. */
class ObserveActiveDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<Unit, List<ModelDownloadProgress>>(dispatchers.io) {

    override fun execute(params: Unit): Flow<List<ModelDownloadProgress>> =
        downloadRepository.observeActiveDownloads()
}
