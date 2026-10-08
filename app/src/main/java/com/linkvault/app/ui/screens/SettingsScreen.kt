package com.linkvault.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.linkvault.app.backup.RestoreResult
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.theme.Slate500
import com.linkvault.app.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

enum class SettingsSubscreen {
    MAIN,
    APPEARANCE,
    BACKUP,
    PRIVACY
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
        SettingsSubscreen.BACKUP -> BackupSubscreen(
            viewModel = viewModel,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier
        )
        SettingsSubscreen.PRIVACY -> PrivacySubscreen(
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
    val context = LocalContext.current

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
            IconButton(onClick = { Toast.makeText(context, "Search settings", Toast.LENGTH_SHORT).show() }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // App Card (matching left screenshot)
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                // Momo Mascot squircle avatar
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
                    com.linkvault.app.ui.components.MomoMascotView(
                        size = 50.dp,
                        animate = true,
                        holdingCard = true
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
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
                        text = "Version 1.0.7",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Rows
        SettingsRowItem(
            icon = Icons.Outlined.Palette,
            title = "Appearance",
            subtitle = "Theme, colors, display",
            onClick = { onNavigate(SettingsSubscreen.APPEARANCE) }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Folder,
            title = "Library",
            subtitle = "Sorting, default views, behavior",
            onClick = { Toast.makeText(context, "Library settings", Toast.LENGTH_SHORT).show() }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Link,
            title = "Capture & Save",
            subtitle = "What happens when you save",
            onClick = { Toast.makeText(context, "Capture & Save settings", Toast.LENGTH_SHORT).show() }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Label,
            title = "Categories & Tags",
            subtitle = "Automatic organization and custom tags",
            onClick = { Toast.makeText(context, "Categories settings", Toast.LENGTH_SHORT).show() }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Description,
            title = "Notes",
            subtitle = "Default note settings, clipboard",
            onClick = { Toast.makeText(context, "Notes settings", Toast.LENGTH_SHORT).show() }
        )
        SettingsRowItem(
            icon = Icons.Outlined.CloudUpload,
            title = "Backup & Restore",
            subtitle = "Export and import your data",
            onClick = { onNavigate(SettingsSubscreen.BACKUP) }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Shield,
            title = "Privacy & Data",
            subtitle = "Local storage, network, permissions",
            onClick = { onNavigate(SettingsSubscreen.PRIVACY) }
        )
        SettingsRowItem(
            icon = Icons.Outlined.Science,
            title = "Advanced",
            subtitle = "Developer options, experimental features",
            onClick = { Toast.makeText(context, "Advanced options", Toast.LENGTH_SHORT).show() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Slate500,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        SettingsRowItem(
            icon = Icons.Outlined.Info,
            title = "About MomoStack",
            subtitle = "Version, licenses, credits",
            onClick = { Toast.makeText(context, "MomoStack v1.0.5", Toast.LENGTH_SHORT).show() }
        )
        SettingsRowItem(
            icon = Icons.Outlined.HelpOutline,
            title = "Help & Feedback",
            subtitle = "Get help or suggest features",
            onClick = { Toast.makeText(context, "github.com/hamza140202/link-vault", Toast.LENGTH_SHORT).show() }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

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
    var reduceMotion by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
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
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Theme Segmented Control
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose how MomoStack looks",
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

        // Dynamic Color Toggle
        ToggleRow(
            title = "Dynamic color",
            subtitle = "Use your device's color scheme",
            checked = dynamicColorEnabled,
            onCheckedChange = { dynamicColorEnabled = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Display
        Text(
            text = "Display",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Slate500,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Text(
            text = "Content density",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Adjust spacing and item size",
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
            title = "Show thumbnails",
            subtitle = "Display preview images for links",
            checked = showThumbnails,
            onCheckedChange = { showThumbnails = it }
        )
        ToggleRow(
            title = "Show domain",
            subtitle = "Always show source domain",
            checked = showDomain,
            onCheckedChange = { showDomain = it }
        )
        ToggleRow(
            title = "Show description",
            subtitle = "Show link description",
            checked = showDescription,
            onCheckedChange = { showDescription = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Animations
        Text(
            text = "Animations",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Slate500,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        ToggleRow(
            title = "Use smooth animations",
            subtitle = "Smoother transitions and effects",
            checked = smoothAnimations,
            onCheckedChange = { smoothAnimations = it }
        )
        ToggleRow(
            title = "Reduce motion",
            subtitle = "Minimize animations for accessibility",
            checked = reduceMotion,
            onCheckedChange = { reduceMotion = it }
        )

        Spacer(modifier = Modifier.height(80.dp))
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
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "Export and import your entire library using portable, versioned JSON archives.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val json = viewModel.exportBackupJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MomoStack Backup", json))
                    Toast.makeText(context, "Backup copied to clipboard (${json.length} bytes)", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Backup JSON")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showRestoreDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Restore Backup JSON")
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Backup JSON") },
                text = {
                    Column {
                        Text(
                            text = "Paste your MomoStack JSON archive below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            placeholder = { Text("Paste JSON here...", color = Slate400) },
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
                                        restoreResultMsg = "Restored ${res.restoredItemsCount} items!"
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
                text = "Privacy & Data",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "• 100% on-device private SQLite database.\n" +
                            "• Zero user telemetry, zero analytics trackers, zero cloud required.\n" +
                            "• Direct enrichment without proxy servers.\n" +
                            "• Clipboard never transmitted over the network.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
