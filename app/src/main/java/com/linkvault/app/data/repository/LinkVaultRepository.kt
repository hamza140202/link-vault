package com.linkvault.app.data.repository

import com.linkvault.app.data.dao.CategoryDao
import com.linkvault.app.data.dao.LinkItemDao
import com.linkvault.app.data.entity.CategoryEntity
import com.linkvault.app.data.entity.LinkItemEntity
import com.linkvault.app.data.model.LinkItem
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
