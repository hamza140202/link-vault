package com.linkvault.app.backup

import com.linkvault.app.data.entity.CategoryEntity
import com.linkvault.app.data.model.LinkItem
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val backupVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<LinkItem>,
    val categories: List<CategoryEntity> = emptyList()
)

sealed class RestoreResult {
    data class Success(val restoredItemsCount: Int, val restoredCategoriesCount: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
