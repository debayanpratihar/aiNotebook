package com.debayan.ainotebook.domain.usecase.model

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.repository.ModelRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the currently active model (or null when none is active). */
class ObserveActiveModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<Unit, InstalledModel?>(dispatchers.io) {

    override fun execute(params: Unit): Flow<InstalledModel?> =
        modelRepository.observeActiveModel()
}
