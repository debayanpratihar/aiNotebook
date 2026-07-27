package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.model.export.ImageFormat

/** Exports notebooks to shareable files. */
interface ExportRepository {

    /**
     * Writes a lossless native `.ainb` package for the notebook. Preserves pages, layers, vector
     * strokes, and AI annotations.
     */
    suspend fun exportNativePackage(notebookId: String): AppResult<ExportedFile>

    /** Renders the notebook to a multi-page PDF (one PDF page per notebook page). */
    suspend fun exportPdf(notebookId: String): AppResult<ExportedFile>

    /** Renders the notebook's first page to a PNG/JPEG image. */
    suspend fun exportImage(notebookId: String, format: ImageFormat): AppResult<ExportedFile>
}
