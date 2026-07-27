package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult

/**
 * Schedules background OCR of a page. Recognition is non-destructive: it reads the page's strokes,
 * recognizes their text, and updates the search index without ever modifying the strokes.
 */
interface OcrRepository {

    /** Enqueues (or replaces) a background OCR/index job for the page. */
    suspend fun requestPageIndexing(notebookId: String, pageId: String): AppResult<Unit>
}
