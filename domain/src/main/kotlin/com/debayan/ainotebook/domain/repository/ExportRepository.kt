package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.export.ExportedFile

/** Exports notebooks to shareable files. */
interface ExportRepository {

    /**
     * Writes a lossless native `.ainb` package for the notebook and returns the resulting file.
     * The package preserves the notebook's pages, layers, vector strokes, and AI annotations.
     */
    suspend fun exportNativePackage(notebookId: String): AppResult<ExportedFile>
}
