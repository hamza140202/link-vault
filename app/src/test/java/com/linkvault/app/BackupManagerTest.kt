package com.linkvault.app

import com.linkvault.app.backup.BackupPayload
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.data.model.ProcessingStatus
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
}
