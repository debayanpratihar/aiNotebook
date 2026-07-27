package com.debayan.ainotebook.feature.canvas.engine

import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeBuilderTest {

    @Test
    fun add_rejectsPointsWithinDedupRadius() {
        val builder = StrokeBuilder(minWorldDistance = 5f)
        builder.start(StrokePoint(0f, 0f))
        assertFalse(builder.add(StrokePoint(2f, 2f))) // ~2.8 units away → rejected
        assertTrue(builder.add(StrokePoint(10f, 10f))) // ~14 units away → accepted
    }

    @Test
    fun build_producesStrokeWithPointsAndBoundingBox() {
        val builder = StrokeBuilder()
        builder.start(StrokePoint(0f, 0f))
        builder.add(StrokePoint(10f, 20f))

        val stroke = requireNotNull(builder.build("s1", "l1", BrushSettings(), createdAt = 123L))

        assertEquals(2, stroke.points.size)
        assertEquals("l1", stroke.layerId)
        assertEquals(0f, stroke.boundingBox.left, 0.001f)
        assertEquals(20f, stroke.boundingBox.bottom, 0.001f)
    }

    @Test
    fun build_returnsNullWhenEmpty() {
        assertNull(StrokeBuilder().build("s", "l", BrushSettings(), 0L))
    }
}
