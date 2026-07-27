package com.debayan.ainotebook.data.repository

import android.content.Context
import com.debayan.ainotebook.core.AppConstants
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.export.NativeNotebookPackager
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** [ExportRepository] that writes native packages into the app's exports directory. */
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packager: NativeNotebookPackager,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : ExportRepository {

    override suspend fun exportNativePackage(notebookId: String): AppResult<ExportedFile> =
        withContext(dispatchers.io) {
            try {
                val exportsDir = File(context.filesDir, AppConstants.Directories.EXPORTS).apply { mkdirs() }
                val file = File(
                    exportsDir,
                    "notebook_${notebookId}_${timeProvider.now()}.${NativeNotebookPackager.FILE_EXTENSION}",
                )
                packager.export(notebookId, file)
                AppResult.Success(ExportedFile(file.absolutePath, NativeNotebookPackager.MIME_TYPE))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AppResult.Failure(AppError.Storage(throwable.message, throwable))
            }
        }
}
