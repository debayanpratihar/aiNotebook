package com.debayan.ainotebook.data.repository

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.data.local.room.dao.LayerDao
import com.debayan.ainotebook.data.mapper.toDomain
import com.debayan.ainotebook.data.mapper.toEntity
import com.debayan.ainotebook.data.util.runDbCatching
import com.debayan.ainotebook.domain.model.canvas.Layer
import com.debayan.ainotebook.domain.repository.LayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [LayerRepository] over Room. */
class LayerRepositoryImpl @Inject constructor(
    private val layerDao: LayerDao,
    private val dispatchers: DispatcherProvider,
) : LayerRepository {

    override fun observeLayers(pageId: String): Flow<List<Layer>> =
        layerDao.observeByPage(pageId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addLayer(layer: Layer): AppResult<Unit> =
        dispatchers.runDbCatching { layerDao.upsert(layer.toEntity()) }

    override suspend fun updateLayer(layer: Layer): AppResult<Unit> =
        dispatchers.runDbCatching { layerDao.upsert(layer.toEntity()) }

    override suspend fun deleteLayer(layerId: String): AppResult<Unit> =
        dispatchers.runDbCatching { layerDao.deleteById(layerId) }
}
