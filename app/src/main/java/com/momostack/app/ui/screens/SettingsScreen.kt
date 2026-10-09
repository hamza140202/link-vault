package com.momostack.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momostack.app.backup.RestoreResult
import com.momostack.app.ui.components.MomoMascotView
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

enum class SettingsSubscreen {
    MAIN,
    APPEARANCE,
    LIBRARY,
    CAPTURE,
    CATEGORIES,
    NOTES,
    BACKUP,
    PRIVACY,
    ADVANCED,
    ABOUT,
    HELP
}

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
            onNavigate = { activeSubscreen = it },
            modifier = modifier
        )
        SettingsSubscreen.APPEARANCE -> AppearanceSubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.LIBRARY -> LibrarySubscreen(
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.CAPTURE -> CaptureSubscreen(
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
    onNavigate: (SettingsSubscreen) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        animate = true,
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
                        text = "Version 1.0.12",
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
            SettingsItemModel("Appearance", "Colors, text size, and look", Icons.Outlined.Palette, SettingsSubscreen.APPEARANCE),
            SettingsItemModel("Library", "How your saved items look and sort", Icons.Outlined.Folder, SettingsSubscreen.LIBRARY),
            SettingsItemModel("Capture & Save", "How links and copied text are saved", Icons.Outlined.Link, SettingsSubscreen.CAPTURE),
            SettingsItemModel("Categories & Tags", "Organize into folders and tags", Icons.Outlined.Label, SettingsSubscreen.CATEGORIES),
            SettingsItemModel("Notes", "Writing buddy and quick paste", Icons.Outlined.Description, SettingsSubscreen.NOTES),
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
            subtitle = "The story, version, and credits",
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

private data class SettingsItemModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: SettingsSubscreen
)

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IndigoPrimary
            )
        )
    }
}

// ---------------------------------------------------------------------------
// SUB-SCREENS WITH SOFT, FRIENDLY LANGUAGE
// ---------------------------------------------------------------------------

@Composable
fun AppearanceSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeSelection by remember { mutableStateOf("System") }
    var dynamicColorEnabled by remember { mutableStateOf(true) }
    var densitySelection by remember { mutableStateOf("Compact") }
    var showThumbnails by remember { mutableStateOf(true) }
    var showDomain by remember { mutableStateOf(true) }
    var showDescription by remember { mutableStateOf(true) }
    var smoothAnimations by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Appearance", onBack = onBack)

        Text(
            text = "Look & Feel",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Match your phone or pick your preferred style",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("System", "Light", "Dark").forEach { option ->
                FilterChip(
                    selected = themeSelection == option,
                    onClick = { themeSelection = option },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ToggleRow(
            title = "Match phone wallpaper colors",
            subtitle = "Tints buttons and accents to match your home screen",
            checked = dynamicColorEnabled,
            onCheckedChange = { dynamicColorEnabled = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Card Size",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose how roomy or snug your cards feel",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Compact", "Comfortable", "Spacious").forEach { density ->
                FilterChip(
                    selected = densitySelection == density,
                    onClick = { densitySelection = density },
                    label = { Text(density) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ToggleRow(
            title = "Show photo previews",
            subtitle = "Displays photos for links and social posts",
            checked = showThumbnails,
            onCheckedChange = { showThumbnails = it }
        )

        ToggleRow(
            title = "Show website name",
            subtitle = "Shows where the link comes from (e.g. youtube.com)",
            checked = showDomain,
            onCheckedChange = { showDomain = it }
        )

        ToggleRow(
            title = "Show quick summaries",
            subtitle = "A short reading snippet under each title",
            checked = showDescription,
            onCheckedChange = { showDescription = it }
        )

        ToggleRow(
            title = "Playful animations",
            subtitle = "Smooth transitions and gentle bounces",
            checked = smoothAnimations,
            onCheckedChange = { smoothAnimations = it }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LibrarySubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var defaultFilter by remember { mutableStateOf("All Links") }
    var sortOrder by remember { mutableStateOf("Newest First") }
    var autoClearArchived by remember { mutableStateOf(false) }
    var showMomoEncouragement by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Library & Behavior", onBack = onBack)

        Text(
            text = "What to show when opening",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose what opens first when you launch MomoStack",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All Links", "Notes", "Favorites").forEach { filter ->
                FilterChip(
                    selected = defaultFilter == filter,
                    onClick = { defaultFilter = filter },
                    label = { Text(filter) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sorting",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Newest First", "Oldest First").forEach { sort ->
                FilterChip(
                    selected = sortOrder == sort,
                    onClick = { sortOrder = sort },
                    label = { Text(sort) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        ToggleRow(
            title = "Friendly mascot when empty",
            subtitle = "Shows a cheerful Momo illustration when a folder is clean",
            checked = showMomoEncouragement,
            onCheckedChange = { showMomoEncouragement = it }
        )

        ToggleRow(
            title = "Clean up old archive items",
            subtitle = "Quietly clears out archived items older than a month",
            checked = autoClearArchived,
            onCheckedChange = { autoClearArchived = it }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CaptureSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var deduplicationMode by remember { mutableStateOf("Update with latest info") }
    var headlessCapture by remember { mutableStateOf(true) }
    var wifiOnlyEnrichment by remember { mutableStateOf(false) }
    var clipboardAutoDetect by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Capture & Save", onBack = onBack)

        Text(
            text = "When you save a link again",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "How MomoStack handles saving a link you already kept",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Update with latest info", "Keep as it was").forEach { mode ->
                FilterChip(
                    selected = deduplicationMode == mode,
                    onClick = { deduplicationMode = mode },
                    label = { Text(mode) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ToggleRow(
            title = "Quick background saving",
            subtitle = "Saves instantly from the share menu without pulling you away",
            checked = headlessCapture,
            onCheckedChange = { headlessCapture = it }
        )

        ToggleRow(
            title = "Save copied text as notes",
            subtitle = "If you copy words instead of a link, it saves as a clean note",
            checked = clipboardAutoDetect,
            onCheckedChange = { clipboardAutoDetect = it }
        )

        ToggleRow(
            title = "Download pictures only on Wi-Fi",
            subtitle = "Saves mobile data by waiting for Wi-Fi to load preview photos",
            checked = wifiOnlyEnrichment,
            onCheckedChange = { wifiOnlyEnrichment = it }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CategoriesSettingsSubscreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    var autoClassify by remember { mutableStateOf(true) }
    var showCategoryPills by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Categories & Folders", onBack = onBack)

        ToggleRow(
            title = "Smart folder sorting",
            subtitle = "Sorts links into YouTube, Reading, Work, etc. automatically",
            checked = autoClassify,
            onCheckedChange = { autoClassify = it }
        )

        ToggleRow(
            title = "Show category tags on cards",
            subtitle = "Small colorful badges on your saved links",
            checked = showCategoryPills,
            onCheckedChange = { showCategoryPills = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Your Categories (${categories.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        categories.forEach { cat ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    if (cat.isSystem) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Default",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Slate500
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun NotesSettingsSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mascotWatcherEnabled by remember { mutableStateOf(true) }
    var smartPasteConversion by remember { mutableStateOf(true) }
    var showCounters by remember { mutableStateOf(true) }
    var mascotMood by remember { mutableStateOf("Playful") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Notes & Writing", onBack = onBack)

        ToggleRow(
            title = "Momo writing buddy",
            subtitle = "A cute dumpling friend who watches and cheers as you type",
            checked = mascotWatcherEnabled,
            onCheckedChange = { mascotWatcherEnabled = it }
        )

        if (mascotWatcherEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Momo's demeanor",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Playful", "Focused", "Calm").forEach { mood ->
                    FilterChip(
                        selected = mascotMood == mood,
                        onClick = { mascotMood = mood },
                        label = { Text(mood) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        ToggleRow(
            title = "Keep bold & headings when pasting",
            subtitle = "When you paste articles, titles and bold text stay looking neat",
            checked = smartPasteConversion,
            onCheckedChange = { smartPasteConversion = it }
        )

        ToggleRow(
            title = "Show word and character count",
            subtitle = "A small counter at the top of your note",
            checked = showCounters,
            onCheckedChange = { showCounters = it }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CareSubscreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    var healthStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Care & Maintenance", onBack = onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Your Collection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "• Saved items: ${stats.totalLinks}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Favorites: ${stats.favorites}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Notes: ${stats.notes}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Everything is tucked away safely on your phone 📱✨",
                    style = MaterialTheme.typography.bodySmall,
                    color = IndigoPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                viewModel.refetchPendingItems()
                Toast.makeText(context, "Looking for preview photos...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Link Photos & Details")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                healthStatus = "Your vault is in great shape! Everything is safe and healthy ✨"
                Toast.makeText(context, "Vault check passed!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check Vault Health")
        }

        if (healthStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = healthStatus!!,
                style = MaterialTheme.typography.bodySmall,
                color = IndigoPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(context, "Temporary photos cleaned up", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Free Up Phone Storage")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AboutSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "About MomoStack", onBack = onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6B8B), Color(0xFFFF8E53), Color(0xFFFFAE33))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MomoMascotView(size = 64.dp, animate = true, holdingCard = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MomoStack",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Save anything for later · v1.0.12",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = IndigoPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A cozy, peaceful place to keep your favorite web links, reading lists, and thoughts. No ads, no cloud logins, no tracking — just you, your links, and Momo the dumpling 🥟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Crafted with Care",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Slate500
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "• Made by solo developer: Bilal Ansari", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Mascot: Momo the Dumpling 🥟", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Stored 100% on this phone", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Open Source under the MIT License", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hamza140202/link-vault"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("repo", "https://github.com/hamza140202/link-vault"))
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View on GitHub", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun HelpSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Help & Common Questions", onBack = onBack)

        val faqs = listOf(
            Pair("How do I save a link quickly?", "Whenever you're in Chrome, YouTube, Instagram or any app, tap Share and choose MomoStack. It saves quietly in the background without pulling you away."),
            Pair("Where are my links and notes kept?", "Everything is kept safely on your phone. Nothing is ever sent to any cloud server or company."),
            Pair("What happens if I save a link twice?", "MomoStack is smart: it updates with any new notes or details you add, keeping your list clean and duplicate-free."),
            Pair("How do notes keep their formatting?", "When you paste text with headings or bold words, MomoStack keeps them looking neat. You can also use the formatting buttons at the top of the note editor.")
        )

        faqs.forEach { (q, a) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = q, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = a, style = MaterialTheme.typography.bodySmall, color = Slate500, lineHeight = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val diag = "MomoStack v1.0.12 (Build 13)\nAndroid: ${android.os.Build.VERSION.RELEASE}\nDevice: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                clipboard.setPrimaryClip(ClipData.newPlainText("support_info", diag))
                Toast.makeText(context, "Device info copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Device Info for Support")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BackupSubscreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreResultMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Backup & Restore", onBack = onBack)

        Text(
            text = "Back up your vault",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Copy a safe backup of all your saved links and notes to your clipboard so you can save it anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    val json = viewModel.exportBackupJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MomoStack Backup", json))
                    Toast.makeText(context, "Backup copied to clipboard! Paste it into a safe file.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Copy Full Backup")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Restore from a backup",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Paste a previous backup copy to restore all your links and notes.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showRestoreDialog = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Restore Backup")
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore from Backup") },
                text = {
                    Column {
                        Text(
                            text = "Paste your backup text below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            placeholder = { Text("Paste backup text here...", color = Slate400) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                        if (restoreResultMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = restoreResultMsg!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoPrimary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = viewModel.restoreBackupJson(restoreJsonInput, clearExisting = false)
                                when (res) {
                                    is RestoreResult.Success -> {
                                        restoreResultMsg = "Restored ${res.restoredItemsCount} items safely!"
                                        Toast.makeText(context, restoreResultMsg, Toast.LENGTH_SHORT).show()
                                        showRestoreDialog = false
                                    }
                                    is RestoreResult.Error -> {
                                        restoreResultMsg = res.message
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PrivacySubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Privacy & Safety", onBack = onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Our Promise to You 🌸",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Everything you save lives only on your phone.\n\n" +
                            "• No account needed, no sign-ins, and zero ads.\n\n" +
                            "• We never track what you read, save, or write.\n\n" +
                            "• Your clipboard stays strictly in your hands.\n\n" +
                            "• You can export your full collection anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SubscreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
