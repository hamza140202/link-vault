package com.momostack.app.feature.screenshotvault.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.momostack.app.MomoStackApp
import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import com.momostack.app.feature.screenshotvault.data.ScreenshotVaultDatabase
import com.momostack.app.feature.screenshotvault.data.ScreenshotVaultRepository
import com.momostack.app.feature.screenshotvault.service.ScreenshotWatcherService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScreenshotVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScreenshotVaultRepository
    private val userPreferences = (application as MomoStackApp).userPreferences

    val searchQuery = MutableStateFlow("")
    val isAutoCaptureEnabled = MutableStateFlow(userPreferences.isAutoCaptureScreenshotsEnabled())
    val isImporting = MutableStateFlow(false)
    val selectedScreenshot = MutableStateFlow<ScreenshotEntity?>(null)

    init {
        val database = ScreenshotVaultDatabase.getDatabase(application)
        repository = ScreenshotVaultRepository(application, database.screenshotDao())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val screenshots: StateFlow<List<ScreenshotEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allScreenshots
            } else {
                repository.searchScreenshots(query.trim())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun selectScreenshot(screenshot: ScreenshotEntity?) {
        selectedScreenshot.value = screenshot
    }

    fun toggleAutoCapture(context: Context, enabled: Boolean) {
        userPreferences.setAutoCaptureScreenshotsEnabled(enabled)
        isAutoCaptureEnabled.value = enabled
        if (enabled) {
            ScreenshotWatcherService.start(context)
        } else {
            ScreenshotWatcherService.stop(context)
        }
    }

    fun importScreenshot(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            try {
                val saved = repository.importScreenshotFromUri(uri)
                selectedScreenshot.value = saved
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isImporting.value = false
            }
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            if (selectedScreenshot.value?.id == screenshot.id) {
                selectedScreenshot.value = null
            }
            repository.deleteScreenshot(screenshot)
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            if (selectedScreenshot.value?.id == id) {
                selectedScreenshot.value = repository.getScreenshotById(id)
            }
        }
    }

    fun updateNotes(id: Long, newNotes: String) {
        viewModelScope.launch {
            repository.updateNotes(id, newNotes)
            if (selectedScreenshot.value?.id == id) {
                selectedScreenshot.value = repository.getScreenshotById(id)
            }
        }
    }
}
