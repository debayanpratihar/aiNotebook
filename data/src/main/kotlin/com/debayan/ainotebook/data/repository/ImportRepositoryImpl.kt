package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.export.NativeNotebookPackager
import com.debayan.ainotebook.domain.repository.ImportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** [ImportRepository] that reads and validates native packages via [NativeNotebookPackager]. */
class ImportRepositoryImpl @Inject constructor(
    private val packager: NativeNotebookPackager,
    private val dispatchers: DispatcherProvider,
) : ImportRepository {

    override suspend fun importNativePackage(sourcePath: String): AppResult<String> =
        withContext(dispatchers.io) {
            try {
                AppResult.Success(packager.import(File(sourcePath)))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(AppError.Storage(throwable.message, throwable))
            }
        }
}
