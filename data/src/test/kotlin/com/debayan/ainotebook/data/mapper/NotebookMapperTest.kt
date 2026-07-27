package com.debayan.ainotebook.data.mapper

import com.debayan.ainotebook.data.local.room.entity.NotebookEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class NotebookMapperTest {

    @Test
    fun entityToDomainToEntity_roundTripsWithoutLoss() {
        val entity = NotebookEntity(
            notebookId = "n1",
            title = "Notes",
            description = "desc",
            coverThumbnail = "cover.png",
            folderId = "f1",
            templateId = "t1",
            color = 0xFF112233,
            createdAt = 100L,
            updatedAt = 200L,
            isFavorite = true,
            isArchived = false,
            pageCount = 3,
        )

        val domain = entity.toDomain()

        assertEquals("n1", domain.id)
        assertEquals("Notes", domain.title)
        assertEquals("cover.png", domain.coverThumbnailPath)
        assertEquals(3, domain.pageCount)
        // Round-trip should reproduce the original row exactly.
        assertEquals(entity, domain.toEntity())
    }
}
