package com.debayan.ainotebook.domain.usecase.notebook

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Renames a notebook, falling back to a default title when the input is blank. */
class RenameNotebookUseCase @Inject constructor(
    private val notebookRepository: NotebookRepository,
    dispatchers: DispatcherProvider,
) : UseCase<RenameNotebookUseCase.Params, Unit>(dispatchers.io) {

    data class Params(val notebookId: String, val title: String)

    override suspend fun execute(params: Params): AppResult<Unit> {
        val current = notebookRepository.observeNotebook(params.notebookId).first()
            ?: return AppResult.Failure(AppError.NotFound("Notebook not found"))
        val title = params.title.trim().ifBlank { DEFAULT_TITLE }
        return notebookRepository.updateNotebook(current.copy(title = title))
    }

    private companion object {
        const val DEFAULT_TITLE = "Untitled Notebook"
    }
}
