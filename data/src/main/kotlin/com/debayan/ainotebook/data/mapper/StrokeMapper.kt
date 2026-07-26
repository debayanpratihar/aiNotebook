package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.BoundingBoxEmbedded
import com.debayan.ainotebook.data.local.room.entity.StrokeEntity
import com.debayan.ainotebook.data.local.room.entity.StrokePointEntity
import com.debayan.ainotebook.data.local.room.relation.StrokeWithPoints
import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.canvas.ToolType

/** Maps between the persisted stroke (+ its points) and the [Stroke] domain model. */

fun StrokeWithPoints.toDomain(): Stroke = Stroke(
    id = stroke.strokeId,
    layerId = stroke.layerId,
    tool = stroke.toolType.toToolType(),
    color = stroke.color,
    width = stroke.width,
    opacity = stroke.opacity,
    points = points
        .sortedBy { it.sequenceNumber }
        .map { StrokePoint(x = it.x, y = it.y, pressure = it.pressure, timestamp = it.timestamp) },
    boundingBox = with(stroke.boundingBox) { BoundingBox(left, top, right, bottom) },
    createdAt = stroke.createdAt,
)

fun Stroke.toStrokeEntity(): StrokeEntity = StrokeEntity(
    strokeId = id,
    layerId = layerId,
    toolType = tool.name,
    color = color,
    width = width,
    opacity = opacity,
    boundingBox = BoundingBoxEmbedded(
        left = boundingBox.left,
        top = boundingBox.top,
        right = boundingBox.right,
        bottom = boundingBox.bottom,
    ),
    createdAt = createdAt,
)

/** Explodes a stroke's ordered points into rows; the DB assigns each row an auto-generated id. */
fun Stroke.toPointEntities(): List<StrokePointEntity> = points.mapIndexed { index, point ->
    StrokePointEntity(
        strokeId = id,
        sequenceNumber = index,
        x = point.x,
        y = point.y,
        pressure = point.pressure,
        timestamp = point.timestamp,
    )
}

private fun String.toToolType(): ToolType =
    runCatching { ToolType.valueOf(this) }.getOrDefault(ToolType.BALL_PEN)
