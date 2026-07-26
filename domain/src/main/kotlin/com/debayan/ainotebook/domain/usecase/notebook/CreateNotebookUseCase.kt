package com.debayan.ainotebook.domain.usecase.notebook

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.model.canvas.Layer
import com.debayan.ainotebook.domain.model.canvas.Page
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.repository.PageRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a new notebook that is immediately ready to draw in: the notebook plus its first page and
 * a default layer. Returns the new notebook id. The page/layer creation is transactional inside the
 * repository (per the storage spec); notebook and page are created in sequence.
 */
class CreateNotebookUseCase @Inject constructor(
    private val notebookRepository: NotebookRepository,
    private val pageRepository: PageRepository,
    private val timeProvider: TimeProvider,
    dispatchers: DispatcherProvider,
) : UseCase<CreateNotebookUseCase.Params, String>(dispatchers.default) {

    data class Params(
        val title: String,
        val templateId: String? = null,
        val color: Long = 0L,
    )

    override suspend fun execute(params: Params): AppResult<String> {
        val now = timeProvider.now()
        val notebookId = UUID.randomUUID().toString()

        val notebook = Notebook(
            id = notebookId,
            title = params.title.ifBlank { DEFAULT_TITLE },
            templateId = params.templateId,
            color = params.color,
            createdAt = now,
            updatedAt = now,
        )
        when (val result = notebookRepository.createNotebook(notebook)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> Unit
        }

        val page = Page(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            pageNumber = 1,
            templateId = params.templateId,
            canvasWidth = Page.DEFAULT_WIDTH,
            canvasHeight = Page.DEFAULT_HEIGHT,
            createdAt = now,
            updatedAt = now,
        )
        val layer = Layer(
            id = UUID.randomUUID().toString(),
            pageId = page.id,
            name = Layer.DEFAULT_NAME,
            orderIndex = 0,
        )
        return when (val result = pageRepository.createPage(page, layer)) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(notebookId)
        }
    }

    private companion object {
        const val DEFAULT_TITLE = "Untitled Notebook"
    }
}
