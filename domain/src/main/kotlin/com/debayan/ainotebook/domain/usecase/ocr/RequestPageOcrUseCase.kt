package com.debayan.ainotebook.domain.usecase.ocr

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.OcrRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Requests background OCR indexing for a page (e.g. after edits settle). */
class RequestPageOcrUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    dispatchers: DispatcherProvider,
) : UseCase<RequestPageOcrUseCase.Params, Unit>(dispatchers.io) {

    data class Params(val notebookId: String, val pageId: String)

    override suspend fun execute(params: Params): AppResult<Unit> =
        ocrRepository.requestPageIndexing(params.notebookId, params.pageId)
}
