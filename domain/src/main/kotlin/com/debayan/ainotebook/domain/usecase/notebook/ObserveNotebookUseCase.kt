package com.debayan.ainotebook.domain.usecase.notebook

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams a single notebook (or null if it does not exist / was deleted). */
class ObserveNotebookUseCase @Inject constructor(
    private val notebookRepository: NotebookRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<String, Notebook?>(dispatchers.io) {

    override fun execute(params: String): Flow<Notebook?> =
        notebookRepository.observeNotebook(params)
}
