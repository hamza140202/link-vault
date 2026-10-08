package com.linkvault.app.backup

import com.linkvault.app.data.dao.CategoryDao
import com.linkvault.app.data.dao.LinkItemDao
import com.linkvault.app.data.entity.LinkItemEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val CURRENT_BACKUP_VERSION = 1

    suspend fun createBackupJson(linkItemDao: LinkItemDao, categoryDao: CategoryDao): String {
        val items = linkItemDao.getAllList().map { it.toDomain() }
        val categories = categoryDao.getAllCategoriesList()

        val payload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            appVersion = "1.0.0",
            exportedAt = System.currentTimeMillis(),
            items = items,
            categories = categories
        )

        return json.encodeToString(payload)
    }

    suspend fun restoreFromJson(
        jsonString: String,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao,
        clearExisting: Boolean = false
    ): RestoreResult {
        return try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)

            if (payload.backupVersion > CURRENT_BACKUP_VERSION) {
                return RestoreResult.Error("Unsupported backup version (${payload.backupVersion}). Please update LinkVault to restore this file.")
            }

            if (clearExisting) {
                linkItemDao.clearAll()
            }

            val entities = payload.items.map { LinkItemEntity.fromDomain(it) }
            linkItemDao.insertAll(entities)

            for (cat in payload.categories) {
                categoryDao.insertCategory(cat)
            }

            RestoreResult.Success(
                restoredItemsCount = entities.size,
                restoredCategoriesCount = payload.categories.size
            )
        } catch (e: Exception) {
            RestoreResult.Error("Failed to parse backup archive: ${e.localizedMessage ?: "Invalid JSON format"}")
        }
    }
}
