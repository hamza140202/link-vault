package com.linkvault.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.linkvault.app.LinkVaultApp
import com.linkvault.app.classifier.AutoClassifier
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.data.model.ProcessingStatus
import com.linkvault.app.ui.screens.DetailScreen
import com.linkvault.app.ui.screens.HomeScreen
import com.linkvault.app.ui.screens.NotesScreen
import com.linkvault.app.ui.screens.SettingsScreen
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.LinkVaultTheme
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.viewmodel.VaultViewModel
import com.linkvault.app.util.UrlExtractor
import com.linkvault.app.worker.EnrichmentScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class MainTab {
    LIBRARY,
    NOTES,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        setContent {
            LinkVaultTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: ""

        if (sharedText.isBlank()) return

        val extracted = UrlExtractor.extract(sharedText)
        val itemId = UUID.randomUUID().toString()

        val itemToSave = if (extracted.url != null) {
            val initialCategory = AutoClassifier.classify(extracted.domain, null, null)
            LinkItem(
                id = itemId,
                url = extracted.url,
                normalizedUrl = extracted.normalizedUrl,
                title = extracted.domain ?: extracted.url,
                domain = extracted.domain,
                notes = extracted.accompanyingText,
                category = initialCategory,
                status = ProcessingStatus.PENDING
            )
        } else {
            LinkItem(
                id = itemId,
                url = "",
                title = sharedText.lines().firstOrNull()?.take(50) ?: "Shared Note",
                notes = sharedText,
                category = "Work",
                status = ProcessingStatus.COMPLETED
            )
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LinkVaultApp.instance.repository.saveItem(itemToSave)
            }

            if (extracted.url != null) {
                EnrichmentScheduler.scheduleEnrichment(applicationContext, itemId)
            }

            viewModel.refreshStats()

            Toast.makeText(
                applicationContext,
                if (extracted.url != null) "Saved to MomoStack" else "Note captured in MomoStack",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: VaultViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.LIBRARY) }
    var selectedItemForDetail by remember { mutableStateOf<LinkItem?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedItemForDetail == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.LIBRARY,
                        onClick = { selectedTab = MainTab.LIBRARY },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.NOTES,
                        onClick = { selectedTab = MainTab.NOTES },
                        icon = { Icon(Icons.Filled.Description, contentDescription = "Notes") },
                        label = { Text("Notes") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { selectedTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedItemForDetail != null) {
            DetailScreen(
                item = selectedItemForDetail!!,
                viewModel = viewModel,
                onBack = { selectedItemForDetail = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedTab) {
                MainTab.LIBRARY -> HomeScreen(
                    viewModel = viewModel,
                    onItemClick = { item -> selectedItemForDetail = item },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.NOTES -> NotesScreen(
                    viewModel = viewModel,
                    onNoteSaved = { selectedTab = MainTab.LIBRARY },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
