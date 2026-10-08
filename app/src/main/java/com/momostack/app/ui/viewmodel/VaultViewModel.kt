package com.momostack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momostack.app.LinkVaultApp
import com.momostack.app.backup.BackupManager
import com.momostack.app.backup.RestoreResult
import com.momostack.app.data.model.LinkItem
import com.momostack.app.data.repository.LinkVaultRepository
import com.momostack.app.data.repository.VaultStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterMode {
    ALL,
    INSTAGRAM,
    YOUTUBE,
    TWITTER,
    THREADS,
    FAVORITES,
    NOTES,
    ARCHIVED
}

class VaultViewModel(
    private val repository: LinkVaultRepository = LinkVaultApp.instance.repository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterMode.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _stats = MutableStateFlow(VaultStats(0, 0, 0))
    val stats = _stats.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<LinkItem>> = combine(
        _searchQuery,
        _selectedFilter,
        _selectedCategory
    ) { query, filter, category ->
        Triple(query, filter, category)
    }.flatMapLatest { (query, filter, category) ->
        if (query.isNotBlank()) {
            repository.searchItems(query)
        } else if (category != null) {
            repository.getItemsByCategory(category)
        } else {
            when (filter) {
                FilterMode.ALL -> repository.getActiveItems()
                FilterMode.INSTAGRAM -> repository.getActiveItems().map { list ->
                    list.filter { item ->
                        item.domain?.contains("instagram.com") == true ||
                        item.category.equals("Instagram", ignoreCase = true)
                    }
                }
                FilterMode.YOUTUBE -> repository.getActiveItems().map { list ->
                    list.filter { item ->
                        item.domain?.contains("youtube.com") == true ||
                        item.domain?.contains("youtu.be") == true ||
                        item.category.equals("YouTube", ignoreCase = true)
                    }
                }
                FilterMode.TWITTER -> repository.getActiveItems().map { list ->
                    list.filter { item ->
                        item.domain?.contains("twitter.com") == true ||
                        item.domain?.contains("x.com") == true ||
                        item.category.contains("Twitter", ignoreCase = true)
                    }
                }
                FilterMode.THREADS -> repository.getActiveItems().map { list ->
                    list.filter { item ->
                        item.domain?.contains("threads.net") == true ||
                        item.category.equals("Threads", ignoreCase = true)
                    }
                }
                FilterMode.FAVORITES -> repository.getFavoriteItems()
                FilterMode.NOTES -> repository.getNotesItems()
                FilterMode.ARCHIVED -> repository.getArchivedItems()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStats()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setFilter(filter: FilterMode) {
        _selectedFilter.value = filter
        _selectedCategory.value = null
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(item: LinkItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id)
            refreshStats()
        }
    }

    fun toggleArchive(item: LinkItem) {
        viewModelScope.launch {
            repository.toggleArchive(item.id)
            refreshStats()
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            refreshStats()
        }
    }

    fun updateNotes(itemId: String, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(itemId, notes)
            refreshStats()
        }
    }

    fun updateCategory(itemId: String, category: String) {
        viewModelScope.launch {
            repository.updateCategory(itemId, category)
        }
    }

    fun createCategory(name: String, colorHex: String = "#4F46E5", iconName: String = "folder") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val entity = com.momostack.app.data.entity.CategoryEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = trimmed,
            colorHex = colorHex,
            iconName = iconName,
            isSystem = false
        )
        viewModelScope.launch {
            repository.addCategory(entity)
        }
    }

    fun saveDirectLink(rawUrl: String, title: String? = null, notes: String? = null) {
        if (rawUrl.isBlank()) return
        val extracted = com.momostack.app.util.UrlExtractor.extract(rawUrl)
        val urlToUse = extracted.url ?: rawUrl.trim()
        val domain = extracted.domain ?: com.momostack.app.util.UrlExtractor.extractDomain(urlToUse)
        val autoTitle = title?.takeIf { it.isNotBlank() } ?: domain ?: urlToUse
        val itemId = java.util.UUID.randomUUID().toString()

        val item = LinkItem(
            id = itemId,
            url = urlToUse,
            normalizedUrl = extracted.normalizedUrl,
            title = autoTitle,
            domain = domain,
            notes = notes?.takeIf { it.isNotBlank() },
            category = com.momostack.app.classifier.AutoClassifier.classify(domain, autoTitle, notes),
            status = com.momostack.app.data.model.ProcessingStatus.PENDING
        )

        viewModelScope.launch {
            repository.saveItem(item)
            com.momostack.app.worker.EnrichmentScheduler.scheduleEnrichment(LinkVaultApp.instance, itemId)
            refreshStats()
        }
    }

    fun saveDirectNote(noteContent: String, title: String? = null) {
        if (noteContent.isBlank()) return
        viewModelScope.launch {
            val autoTitle = title?.takeIf { it.isNotBlank() }
                ?: noteContent.lines().firstOrNull()?.take(40)
                ?: "Quick Note"

            val item = LinkItem(
                id = java.util.UUID.randomUUID().toString(),
                url = "",
                title = autoTitle,
                notes = noteContent,
                category = "Work"
            )
            repository.saveItem(item)
            refreshStats()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = repository.getStats()
        }
    }

    suspend fun exportBackupJson(): String {
        val db = LinkVaultApp.instance.database
        return BackupManager.createBackupJson(db.linkItemDao(), db.categoryDao())
    }

    suspend fun restoreBackupJson(jsonString: String, clearExisting: Boolean): RestoreResult {
        val db = LinkVaultApp.instance.database
        val result = BackupManager.restoreFromJson(jsonString, db.linkItemDao(), db.categoryDao(), clearExisting)
        refreshStats()
        return result
    }
}
