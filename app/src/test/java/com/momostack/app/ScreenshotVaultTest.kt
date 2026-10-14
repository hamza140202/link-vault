package com.momostack.app

import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScreenshotVaultTest {

    @Test
    fun testScreenshotEntityWordAndCharCount() {
        val sampleText = "MomoStack is a private, local-first app for saving links and notes."
        val entity = ScreenshotEntity(
            id = 1,
            localImagePath = "/data/user/0/com.momostack.app/files/screenshots/screenshot_1.png",
            originalUri = "content://media/external/images/media/123",
            extractedText = sampleText
        )

        assertEquals(sampleText.length, entity.characterCount)
        assertEquals(10, entity.wordCount)
        assertNotNull(entity.localImagePath)
    }

    @Test
    fun testEmptyScreenshotEntityWordCount() {
        val entity = ScreenshotEntity(
            id = 2,
            localImagePath = "/path/test.png",
            originalUri = "content://test",
            extractedText = "   "
        )

        assertEquals(0, entity.wordCount)
    }
}
