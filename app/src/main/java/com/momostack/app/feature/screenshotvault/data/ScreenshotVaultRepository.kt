package com.momostack.app.feature.screenshotvault.data

import android.content.Context
import android.net.Uri
import com.momostack.app.feature.screenshotvault.service.ScreenshotOcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ScreenshotVaultRepository(
    private val context: Context,
    private val screenshotDao: ScreenshotDao
) {
    val allScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getAllScreenshots()
    val favoriteScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getFavoriteScreenshots()

    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>> {
        return screenshotDao.searchScreenshots(query)
    }

    suspend fun getScreenshotById(id: Long): ScreenshotEntity? = withContext(Dispatchers.IO) {
        screenshotDao.getScreenshotById(id)
    }

    /**
     * Imports a screenshot from a given Uri, copies it into private app storage,
     * extracts text via on-device ML Kit OCR, and inserts it into ScreenshotVaultDatabase.
     */
    suspend fun importScreenshotFromUri(uri: Uri): ScreenshotEntity = withContext(Dispatchers.IO) {
        val screenshotsDir = File(context.filesDir, "screenshots").apply { mkdirs() }
        val filename = "screenshot_${System.currentTimeMillis()}_${(1000..9999).random()}.png"
        val destFile = File(screenshotsDir, filename)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val extractedText = ScreenshotOcrProcessor.extractTextFromFile(destFile)

        val entity = ScreenshotEntity(
            localImagePath = destFile.absolutePath,
            originalUri = uri.toString(),
            extractedText = extractedText
        )

        val insertedId = screenshotDao.insertScreenshot(entity)
        entity.copy(id = insertedId)
    }

    suspend fun updateNotes(id: Long, newNotes: String) = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotById(id) ?: return@withContext
        screenshotDao.updateScreenshot(existing.copy(userNotes = newNotes))
    }

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotById(id) ?: return@withContext
        screenshotDao.updateScreenshot(existing.copy(isFavorite = !existing.isFavorite))
    }

    suspend fun deleteScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(screenshot.localImagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        screenshotDao.deleteScreenshot(screenshot)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotById(id)
        if (existing != null) {
            deleteScreenshot(existing)
        }
    }
}
