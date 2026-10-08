package com.linkvault.app.ui

import android.os.Bundle
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
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.ui.screens.DetailScreen
import com.linkvault.app.ui.screens.HomeScreen
import com.linkvault.app.ui.screens.NotesScreen
import com.linkvault.app.ui.screens.SettingsScreen
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.LinkVaultTheme
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.viewmodel.VaultViewModel

enum class MainTab {
    LIBRARY,
    NOTES,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinkVaultTheme {
                MainAppScaffold(viewModel = viewModel)
            }
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
