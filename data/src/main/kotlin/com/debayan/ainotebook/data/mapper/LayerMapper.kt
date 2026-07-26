package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.LayerEntity
import com.debayan.ainotebook.domain.model.canvas.Layer

fun LayerEntity.toDomain(): Layer = Layer(
    id = layerId,
    pageId = pageId,
    name = name,
    orderIndex = orderIndex,
    visible = visible,
    locked = locked,
    opacity = opacity,
)

fun Layer.toEntity(): LayerEntity = LayerEntity(
    layerId = id,
    pageId = pageId,
    name = name,
    orderIndex = orderIndex,
    visible = visible,
    locked = locked,
    opacity = opacity,
)
