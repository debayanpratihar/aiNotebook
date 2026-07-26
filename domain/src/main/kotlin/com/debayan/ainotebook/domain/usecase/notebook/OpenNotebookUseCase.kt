package com.debayan.ainotebook.domain.usecase.notebook

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.domain.model.canvas.Layer
import com.debayan.ainotebook.domain.model.canvas.Page
import com.debayan.ainotebook.domain.repository.LayerRepository
import com.debayan.ainotebook.domain.repository.PageRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/** The page to display plus the layer new strokes should attach to. */
data class OpenedNotebook(
    val page: Page,
    val activeLayerId: String,
)

/**
 * Resolves what to show when opening a notebook: its first page and that page's active layer,
 * creating a page (with a default layer) if the notebook has none. Guarantees the canvas always has
 * both a page and a layer to draw on.
 */
class OpenNotebookUseCase @Inject constructor(
    private val pageRepository: PageRepository,
    private val layerRepository: LayerRepository,
    private val timeProvider: TimeProvider,
    dispatchers: DispatcherProvider,
) : UseCase<String, OpenedNotebook>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<OpenedNotebook> {
        val notebookId = params

        val existingPage = pageRepository.getFirstPage(notebookId)
        if (existingPage != null) {
            val layerId = firstLayerId(existingPage.id) ?: run {
                val layer = defaultLayer(existingPage.id)
                when (val result = layerRepository.addLayer(layer)) {
                    is AppResult.Failure -> return result
                    is AppResult.Success -> Unit
                }
                layer.id
            }
            return AppResult.Success(OpenedNotebook(existingPage, layerId))
        }

        val now = timeProvider.now()
        val page = Page(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            pageNumber = 1,
            canvasWidth = Page.DEFAULT_WIDTH,
            canvasHeight = Page.DEFAULT_HEIGHT,
            createdAt = now,
            updatedAt = now,
        )
        val layer = defaultLayer(page.id)
        return when (val result = pageRepository.createPage(page, layer)) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(OpenedNotebook(page, layer.id))
        }
    }

    private suspend fun firstLayerId(pageId: String): String? =
        layerRepository.observeLayers(pageId).first().firstOrNull()?.id

    private fun defaultLayer(pageId: String): Layer = Layer(
        id = UUID.randomUUID().toString(),
        pageId = pageId,
        name = Layer.DEFAULT_NAME,
        orderIndex = 0,
    )
}
