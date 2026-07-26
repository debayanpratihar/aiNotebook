package com.debayan.ainotebook.data.local.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.debayan.ainotebook.data.local.room.entity.StrokeEntity
import com.debayan.ainotebook.data.local.room.entity.StrokePointEntity

/**
 * A stroke together with all of its points, loaded in one query. Points are not ordered by the
 * relation; the mapper sorts them by `sequenceNumber` when building the domain model.
 */
data class StrokeWithPoints(
    @Embedded val stroke: StrokeEntity,
    @Relation(parentColumn = "strokeId", entityColumn = "strokeId")
    val points: List<StrokePointEntity>,
)
