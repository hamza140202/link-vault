package com.momostack.app.backup

import android.net.Uri
import com.momostack.app.data.entity.CategoryEntity
import com.momostack.app.data.model.LinkItem
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ScreenshotBackupItem(
    val originalUri: String = "",
    val extractedText: String = "",
    val userNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Serializable
data class BackupPayload(
    val backupVersion: Int = 2,
    val appVersion: String = "1.0.15",
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<LinkItem>,
    val categories: List<CategoryEntity> = emptyList(),
    val screenshots: List<ScreenshotBackupItem> = emptyList()
)

sealed class RestoreResult {
    data class Success(
        val restoredItemsCount: Int,
        val restoredCategoriesCount: Int,
        val restoredScreenshotsCount: Int = 0
    ) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

data class BackupCreationResult(
    val file: File,
    val publicUri: Uri?,
    val itemCount: Int,
    val jsonString: String
)
