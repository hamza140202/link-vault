package com.momostack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momostack.app.ui.components.MomoMascotView
import com.momostack.app.ui.screens.settings.AboutSubscreen
import com.momostack.app.ui.screens.settings.AppearanceSubscreen
import com.momostack.app.ui.screens.settings.BackupSubscreen
import com.momostack.app.ui.screens.settings.CareSubscreen
import com.momostack.app.ui.screens.settings.CategoriesSettingsSubscreen
import com.momostack.app.ui.screens.settings.HelpSubscreen
import com.momostack.app.ui.screens.settings.LibrarySubscreen
import com.momostack.app.ui.screens.settings.NotesSettingsSubscreen
import com.momostack.app.ui.screens.settings.PrivacySubscreen
import com.momostack.app.ui.screens.settings.SettingsItemModel
import com.momostack.app.ui.screens.settings.SettingsRowItem
import com.momostack.app.ui.screens.settings.SettingsSubscreen
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubscreen by remember { mutableStateOf(SettingsSubscreen.MAIN) }

    BackHandler(enabled = activeSubscreen != SettingsSubscreen.MAIN) {
        activeSubscreen = SettingsSubscreen.MAIN
    }

    when (activeSubscreen) {
        SettingsSubscreen.MAIN -> MainSettingsView(
            viewModel = viewModel,
            onNavigate = { activeSubscreen = it },
            modifier = modifier
        )
        SettingsSubscreen.APPEARANCE -> AppearanceSubscreen(
            viewModel = viewModel,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.LIBRARY -> LibrarySubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.CATEGORIES -> CategoriesSettingsSubscreen(
            viewModel = viewModel,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.NOTES -> NotesSettingsSubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.BACKUP -> BackupSubscreen(
            viewModel = viewModel,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.PRIVACY -> PrivacySubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.ADVANCED -> CareSubscreen(
            viewModel = viewModel,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.ABOUT -> AboutSubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.HELP -> HelpSubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
    }
}

@Composable
fun MainSettingsView(
    viewModel: VaultViewModel,
    onNavigate: (SettingsSubscreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val appearancePrefs by viewModel.appearancePrefs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { isSearching = !isSearching }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = if (isSearching) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isSearching) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings...", color = Slate400) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // App Card with Momo Mascot
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(SettingsSubscreen.ABOUT) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6B8B), Color(0xFFFF8E53), Color(0xFFFFAE33))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MomoMascotView(
                        size = 50.dp,
                        animate = appearancePrefs.smoothAnimations,
                        holdingCard = true
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MomoStack",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Save anything for later",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                    Text(
                        text = "Version 1.0.13",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate400
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val menuItems = listOf(
            SettingsItemModel("Appearance", "Theme, card size, and look", Icons.Outlined.Palette, SettingsSubscreen.APPEARANCE),
            SettingsItemModel("Library", "How your saved items look and sort", Icons.Outlined.Folder, SettingsSubscreen.LIBRARY),
            SettingsItemModel("Categories & Folders", "Organize into folders and tags", Icons.Outlined.Label, SettingsSubscreen.CATEGORIES),
            SettingsItemModel("Notes", "Writing companion and smart paste", Icons.Outlined.Description, SettingsSubscreen.NOTES),
            SettingsItemModel("Backup & Restore", "Save a copy of your stuff safely", Icons.Outlined.CloudUpload, SettingsSubscreen.BACKUP),
            SettingsItemModel("Privacy & Safety", "Your data stays on your phone", Icons.Outlined.Shield, SettingsSubscreen.PRIVACY),
            SettingsItemModel("Care & Maintenance", "Refresh previews and tidy up", Icons.Outlined.Science, SettingsSubscreen.ADVANCED)
        )

        val filteredItems = if (searchQuery.isBlank()) {
            menuItems
        } else {
            menuItems.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredItems.forEach { item ->
            SettingsRowItem(
                icon = item.icon,
                title = item.title,
                subtitle = item.subtitle,
                onClick = { onNavigate(item.target) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "About & Help",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Slate500,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        SettingsRowItem(
            icon = Icons.Outlined.Info,
            title = "About MomoStack",
            subtitle = "The story and version",
            onClick = { onNavigate(SettingsSubscreen.ABOUT) }
        )
        SettingsRowItem(
            icon = Icons.Outlined.HelpOutline,
            title = "Help & FAQs",
            subtitle = "Helpful guides and answers",
            onClick = { onNavigate(SettingsSubscreen.HELP) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
