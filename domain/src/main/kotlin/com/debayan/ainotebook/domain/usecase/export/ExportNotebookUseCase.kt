package com.debayan.ainotebook.domain.usecase.export

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.repository.ExportRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Exports a notebook to a lossless native package. */
class ExportNotebookUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, ExportedFile>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<ExportedFile> =
        exportRepository.exportNativePackage(params)
}
