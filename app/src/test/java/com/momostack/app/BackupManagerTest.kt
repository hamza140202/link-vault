package com.momostack.app

import com.momostack.app.backup.BackupPayload
import com.momostack.app.data.model.LinkItem
import com.momostack.app.data.model.ProcessingStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testBackupPayloadSerialization() {
        val testItem = LinkItem(
            id = "test-123",
            url = "https://kotlinlang.org",
            normalizedUrl = "https://kotlinlang.org",
            title = "Kotlin Programming Language",
            domain = "kotlinlang.org",
            description = "Concise, modern, safe programming language.",
            notes = "Favorite language for Android apps.",
            category = "Development",
            tags = listOf("android", "kotlin"),
            isFavorite = true,
            isArchived = false,
            status = ProcessingStatus.COMPLETED
        )

        val payload = BackupPayload(
            backupVersion = 1,
            appVersion = "1.0.0",
            exportedAt = 1728345600000L,
            items = listOf(testItem),
            categories = emptyList()
        )

        val serialized = json.encodeToString(payload)
        assertTrue(serialized.contains("\"backupVersion\": 1"))
        assertTrue(serialized.contains("https://kotlinlang.org"))

        val deserialized = json.decodeFromString<BackupPayload>(serialized)
        assertEquals(1, deserialized.backupVersion)
        assertEquals(1, deserialized.items.size)
        assertEquals("Kotlin Programming Language", deserialized.items[0].title)
        assertTrue(deserialized.items[0].isFavorite)
    }

    @Test
    fun testBackupPayloadV2WithScreenshots() {
        val testItem = LinkItem(
            id = "item-1",
            url = "https://github.com",
            title = "GitHub",
            domain = "github.com",
            notes = "Code hosting"
        )
        val screenshot = com.momostack.app.backup.ScreenshotBackupItem(
            originalUri = "content://media/external/images/media/42",
            extractedText = "Meeting at 3 PM on Monday",
            userNotes = "Important meeting note",
            isFavorite = true
        )

        val payload = BackupPayload(
            backupVersion = 2,
            appVersion = "1.0.15",
            items = listOf(testItem),
            screenshots = listOf(screenshot)
        )

        val serialized = json.encodeToString(payload)
        val deserialized = json.decodeFromString<BackupPayload>(serialized)

        assertEquals(2, deserialized.backupVersion)
        assertEquals(1, deserialized.items.size)
        assertEquals(1, deserialized.screenshots.size)
        assertEquals("Meeting at 3 PM on Monday", deserialized.screenshots[0].extractedText)
        assertTrue(deserialized.screenshots[0].isFavorite)
    }
}
