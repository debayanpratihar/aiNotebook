package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.canvas.Stroke
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for strokes. A stroke and its points are persisted atomically. Observation is
 * page-scoped (strokes across all of a page's layers) so the canvas can render a page from one flow.
 */
interface StrokeRepository {

    fun observeStrokes(pageId: String): Flow<List<Stroke>>

    /** Inserts or replaces [stroke] together with its points in a single transaction. */
    suspend fun saveStroke(stroke: Stroke): AppResult<Unit>

    suspend fun deleteStroke(strokeId: String): AppResult<Unit>

    suspend fun deleteStrokes(strokeIds: List<String>): AppResult<Unit>
}
