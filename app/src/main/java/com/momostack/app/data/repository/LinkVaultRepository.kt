package com.momostack.app.data.repository

import com.momostack.app.data.dao.CategoryDao
import com.momostack.app.data.dao.LinkItemDao
import com.momostack.app.data.entity.CategoryEntity
import com.momostack.app.data.entity.LinkItemEntity
import com.momostack.app.data.model.LinkItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LinkVaultRepository(
    private val linkItemDao: LinkItemDao,
    private val categoryDao: CategoryDao
) {

    fun getActiveItems(): Flow<List<LinkItem>> {
        return linkItemDao.getAllActive().map { list -> list.map { it.toDomain() } }
    }

    fun getFavoriteItems(): Flow<List<LinkItem>> {
        return linkItemDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    fun getArchivedItems(): Flow<List<LinkItem>> {
        return linkItemDao.getArchived().map { list -> list.map { it.toDomain() } }
    }

    fun getNotesItems(): Flow<List<LinkItem>> {
        return linkItemDao.getNotes().map { list -> list.map { it.toDomain() } }
    }

    fun getItemsByCategory(category: String): Flow<List<LinkItem>> {
        return linkItemDao.getByCategory(category).map { list -> list.map { it.toDomain() } }
    }

    fun searchItems(query: String): Flow<List<LinkItem>> {
        return linkItemDao.search(query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getItemById(id: String): LinkItem? {
        return linkItemDao.getById(id)?.toDomain()
    }

    suspend fun saveItem(item: LinkItem) {
        linkItemDao.insert(LinkItemEntity.fromDomain(item))
    }

    suspend fun saveOrMergeItem(item: LinkItem): LinkItem {
        // If it's a note (empty URL and no normalized URL), insert directly without deduplicating
        if (item.url.isBlank() && item.normalizedUrl.isNullOrBlank()) {
            linkItemDao.insert(LinkItemEntity.fromDomain(item))
            return item
        }

        val existingEntity = linkItemDao.findExisting(
            url = item.url,
            normalizedUrl = item.normalizedUrl?.takeIf { it.isNotBlank() }
        )

        if (existingEntity == null) {
            linkItemDao.insert(LinkItemEntity.fromDomain(item))
            return item
        }

        val existing = existingEntity.toDomain()

        // Smart note merging: if new note is provided and different from existing, combine or keep latest
        val mergedNotes = when {
            !item.notes.isNullOrBlank() && !existing.notes.isNullOrBlank() && item.notes != existing.notes -> {
                "${item.notes}\n\n[Previous Note]: ${existing.notes}"
            }
            !item.notes.isNullOrBlank() -> item.notes
            else -> existing.notes
        }

        val mergedTags = (item.tags + existing.tags).distinct()

        val mergedTitle = if (item.title.isNotBlank() && item.title != item.url && item.title != item.domain) {
            item.title
        } else if (existing.title.isNotBlank()) {
            existing.title
        } else {
            item.title
        }

        val mergedCategory = if (item.category != "Uncategorized") item.category else existing.category

        val mergedItem = existing.copy(
            id = existing.id,
            url = if (item.url.isNotBlank()) item.url else existing.url,
            normalizedUrl = item.normalizedUrl ?: existing.normalizedUrl,
            title = mergedTitle,
            domain = item.domain ?: existing.domain,
            description = item.description ?: existing.description,
            notes = mergedNotes,
            category = mergedCategory,
            tags = mergedTags,
            isFavorite = existing.isFavorite || item.isFavorite,
            isArchived = false, // unarchive on re-save so user sees the newly saved link
            faviconUrl = item.faviconUrl ?: existing.faviconUrl,
            previewImageUrl = item.previewImageUrl ?: existing.previewImageUrl,
            status = if (existing.status == com.momostack.app.data.model.ProcessingStatus.COMPLETED &&
                item.status == com.momostack.app.data.model.ProcessingStatus.PENDING) {
                com.momostack.app.data.model.ProcessingStatus.COMPLETED
            } else {
                item.status
            },
            createdAt = existing.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        linkItemDao.update(LinkItemEntity.fromDomain(mergedItem))
        return mergedItem
    }

    suspend fun updateItem(item: LinkItem) {
        linkItemDao.update(LinkItemEntity.fromDomain(item))
    }

    suspend fun toggleFavorite(id: String) {
        val existing = linkItemDao.getById(id) ?: return
        val updated = existing.copy(
            isFavorite = !existing.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
        linkItemDao.update(updated)
    }

    suspend fun toggleArchive(id: String) {
        val existing = linkItemDao.getById(id) ?: return
        val updated = existing.copy(
            isArchived = !existing.isArchived,
            updatedAt = System.currentTimeMillis()
        )
        linkItemDao.update(updated)
    }

    suspend fun updateNotes(id: String, notes: String) {
        val existing = linkItemDao.getById(id) ?: return
        val updated = existing.copy(
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
        linkItemDao.update(updated)
    }

    suspend fun updateCategory(id: String, category: String) {
        val existing = linkItemDao.getById(id) ?: return
        val updated = existing.copy(
            category = category,
            updatedAt = System.currentTimeMillis()
        )
        linkItemDao.update(updated)
    }

    suspend fun deleteItem(id: String) {
        linkItemDao.deleteById(id)
    }

    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun getStats(): VaultStats {
        return VaultStats(
            totalLinks = linkItemDao.countTotal(),
            favorites = linkItemDao.countFavorites(),
            notes = linkItemDao.countNotes()
        )
    }
}

data class VaultStats(
    val totalLinks: Int,
    val favorites: Int,
    val notes: Int
)
