package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.repository.ModelRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the list of installed models. */
class ObserveInstalledModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<Unit, List<InstalledModel>>(dispatchers.io) {

    override fun execute(params: Unit): Flow<List<InstalledModel>> =
        modelRepository.observeInstalledModels()
}
