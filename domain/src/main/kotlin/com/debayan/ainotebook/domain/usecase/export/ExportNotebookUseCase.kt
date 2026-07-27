package com.debayan.ainotebook.domain.usecase.export

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.export.ExportFormat
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.model.export.ImageFormat
import com.debayan.ainotebook.domain.repository.ExportRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Exports a notebook in the requested [ExportFormat]. */
class ExportNotebookUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
    dispatchers: DispatcherProvider,
) : UseCase<ExportNotebookUseCase.Params, ExportedFile>(dispatchers.io) {

    data class Params(val notebookId: String, val format: ExportFormat)

    override suspend fun execute(params: Params): AppResult<ExportedFile> = when (params.format) {
        ExportFormat.NATIVE_PACKAGE -> exportRepository.exportNativePackage(params.notebookId)
        ExportFormat.PDF -> exportRepository.exportPdf(params.notebookId)
        ExportFormat.PNG -> exportRepository.exportImage(params.notebookId, ImageFormat.PNG)
        ExportFormat.JPEG -> exportRepository.exportImage(params.notebookId, ImageFormat.JPEG)
    }
}
