package com.momostack.app

import com.momostack.app.data.entity.LinkItemEntity
import com.momostack.app.data.model.LinkItem
import com.momostack.app.data.model.OfflineArchiveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineWebArchiveTest {

    @Test
    fun testOfflineArchiveStatusDefaults() {
        val item = LinkItem(
            id = "test-1",
            url = "https://example.com/article",
            title = "Test Article"
        )

        assertEquals(OfflineArchiveStatus.NONE, item.offlinePdfStatus)
        assertNull(item.offlinePdfPath)
        assertEquals(0L, item.offlinePdfSize)
        assertNull(item.offlinePdfCreatedAt)
    }

    @Test
    fun testLinkItemEntityToDomainMapping() {
        val entity = LinkItemEntity(
            id = "test-2",
            url = "https://example.com/doc",
            normalizedUrl = "https://example.com/doc",
            title = "Documentation",
            domain = "example.com",
            description = "A great doc",
            notes = "My notes",
            category = "Development",
            tagsJson = "[\"dev\",\"docs\"]",
            isFavorite = true,
            isArchived = false,
            status = "COMPLETED",
            faviconUrl = null,
            previewImageUrl = null,
            createdAt = 1000L,
            updatedAt = 2000L,
            offlinePdfPath = "/data/user/0/com.momostack.app/files/web_archives/webdoc_test-2.pdf",
            offlinePdfSize = 1048576L,
            offlinePdfCreatedAt = 3000L,
            offlinePdfStatus = "READY"
        )

        val domain = entity.toDomain()
        assertEquals(OfflineArchiveStatus.READY, domain.offlinePdfStatus)
        assertEquals("/data/user/0/com.momostack.app/files/web_archives/webdoc_test-2.pdf", domain.offlinePdfPath)
        assertEquals(1048576L, domain.offlinePdfSize)
        assertEquals(3000L, domain.offlinePdfCreatedAt)

        val roundTrip = LinkItemEntity.fromDomain(domain)
        assertEquals("READY", roundTrip.offlinePdfStatus)
        assertEquals("/data/user/0/com.momostack.app/files/web_archives/webdoc_test-2.pdf", roundTrip.offlinePdfPath)
        assertEquals(1048576L, roundTrip.offlinePdfSize)
    }

    @Test
    fun testCorruptOfflineArchiveStatusFallback() {
        val entity = LinkItemEntity(
            id = "test-3",
            url = "https://example.com/fallback",
            normalizedUrl = null,
            title = "Fallback Test",
            domain = "example.com",
            description = null,
            notes = null,
            category = "General",
            tagsJson = "[]",
            isFavorite = false,
            isArchived = false,
            status = "COMPLETED",
            faviconUrl = null,
            previewImageUrl = null,
            createdAt = 1000L,
            updatedAt = 2000L,
            offlinePdfStatus = "NON_EXISTENT_STATUS"
        )

        val domain = entity.toDomain()
        assertEquals(OfflineArchiveStatus.NONE, domain.offlinePdfStatus)
    }
}
