package com.debayan.ainotebook.data.repository

import android.content.Context
import com.debayan.ainotebook.core.AppConstants
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.data.export.ImageExporter
import com.debayan.ainotebook.data.export.NativeNotebookPackager
import com.debayan.ainotebook.data.export.PdfExporter
import com.debayan.ainotebook.data.local.room.dao.PageDao
import com.debayan.ainotebook.data.local.room.dao.StrokeDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.model.export.ImageFormat
import com.debayan.ainotebook.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** [ExportRepository] that writes native/PDF/image exports into the app's exports directory. */
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packager: NativeNotebookPackager,
    private val pdfExporter: PdfExporter,
    private val imageExporter: ImageExporter,
    private val pageDao: PageDao,
    private val strokeDao: StrokeDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
) : ExportRepository {

    override suspend fun exportNativePackage(notebookId: String): AppResult<ExportedFile> =
        runExport(notebookId, NativeNotebookPackager.FILE_EXTENSION, NativeNotebookPackager.MIME_TYPE) { file ->
            packager.export(notebookId, file)
        }

    override suspend fun exportPdf(notebookId: String): AppResult<ExportedFile> =
        runExport(notebookId, "pdf", "application/pdf") { file ->
            val pages = pageDao.observeByNotebook(notebookId).first()
            val pageStrokes = pages.map { page ->
                strokeDao.observeStrokesWithPointsByPage(page.pageId).first().map { it.toDomain() }
            }
            pdfExporter.export(pageStrokes, file)
        }

    override suspend fun exportImage(notebookId: String, format: ImageFormat): AppResult<ExportedFile> {
        val extension = if (format == ImageFormat.PNG) "png" else "jpg"
        val mimeType = if (format == ImageFormat.PNG) "image/png" else "image/jpeg"
        return runExport(notebookId, extension, mimeType) { file ->
            val firstPage = pageDao.getFirstPage(notebookId)
                ?: throw IllegalStateException("Notebook has no pages to export")
            val strokes = strokeDao.observeStrokesWithPointsByPage(firstPage.pageId).first().map { it.toDomain() }
            imageExporter.export(strokes, format, file)
        }
    }

    private suspend fun runExport(
        notebookId: String,
        extension: String,
        mimeType: String,
        render: suspend (File) -> Unit,
    ): AppResult<ExportedFile> = withContext(dispatchers.io) {
        try {
            val exportsDir = File(context.filesDir, AppConstants.Directories.EXPORTS).apply { mkdirs() }
            val file = File(exportsDir, "notebook_${notebookId}_${timeProvider.now()}.$extension")
            render(file)
            AppResult.Success(ExportedFile(file.absolutePath, mimeType))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(AppError.Storage(throwable.message, throwable))
        }
    }
}
