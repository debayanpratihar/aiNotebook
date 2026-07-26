package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.canvas.Layer
import kotlinx.coroutines.flow.Flow

/** Repository contract for layers on a page. */
interface LayerRepository {

    fun observeLayers(pageId: String): Flow<List<Layer>>

    suspend fun addLayer(layer: Layer): AppResult<Unit>

    suspend fun updateLayer(layer: Layer): AppResult<Unit>

    suspend fun deleteLayer(layerId: String): AppResult<Unit>
}
