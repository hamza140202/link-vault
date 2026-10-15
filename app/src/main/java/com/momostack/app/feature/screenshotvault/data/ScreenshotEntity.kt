package com.momostack.app.feature.screenshotvault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Isolated Room Entity representing a saved screenshot and its extracted text.
 * Completely decoupled from LinkItem and NoteItem to preserve database isolation.
 */
@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val localImagePath: String, // Absolute path in filesDir/screenshots/
    val originalUri: String,    // Content URI or original source
    val extractedText: String,  // On-device OCR extracted text
    val userNotes: String? = null, // Optional user edited notes
    val createdAt: Long = System.currentTimeMillis(),
    val characterCount: Int = extractedText.length,
    val wordCount: Int = if (extractedText.isBlank()) 0 else extractedText.trim().split("\\s+".toRegex()).size,
    val isFavorite: Boolean = false
)
