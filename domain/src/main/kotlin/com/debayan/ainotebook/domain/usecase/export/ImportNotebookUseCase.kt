package com.debayan.ainotebook.domain.usecase.export

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.ImportRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Imports a native package as a new notebook, returning its id. */
class ImportNotebookUseCase @Inject constructor(
    private val importRepository: ImportRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, String>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<String> =
        importRepository.importNativePackage(params)
}
