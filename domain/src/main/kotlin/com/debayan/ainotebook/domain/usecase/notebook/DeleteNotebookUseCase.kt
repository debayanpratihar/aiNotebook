package com.debayan.ainotebook.domain.usecase.notebook

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Deletes a notebook (cascades to its pages/layers/strokes via the database). */
class DeleteNotebookUseCase @Inject constructor(
    private val notebookRepository: NotebookRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, Unit>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<Unit> =
        notebookRepository.deleteNotebook(params)
}
