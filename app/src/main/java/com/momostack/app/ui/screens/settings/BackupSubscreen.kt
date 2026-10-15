package com.momostack.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momostack.app.backup.BackupCreationResult
import com.momostack.app.backup.BackupManager
import com.momostack.app.backup.RestoreResult
import com.momostack.app.ui.theme.SuccessGreen
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BackupSubscreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBackingUp by remember { mutableStateOf(false) }
    var lastBackupResult by remember { mutableStateOf<BackupCreationResult?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }

    var showManualSection by remember { mutableStateOf(false) }
    var showManualRestoreDialog by remember { mutableStateOf(false) }
    var manualRestoreInput by remember { mutableStateOf("") }

    // System File Picker for 1-tap restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isRestoring = true
                val result = viewModel.restoreBackupFromUri(context, uri)
                isRestoring = false
                when (result) {
                    is RestoreResult.Success -> {
                        val details = buildString {
                            append("Successfully restored ${result.restoredItemsCount} links and notes")
                            if (result.restoredCategoriesCount > 0) {
                                append(", ${result.restoredCategoriesCount} categories")
                            }
                            if (result.restoredScreenshotsCount > 0) {
                                append(", and ${result.restoredScreenshotsCount} screenshot transcriptions")
                            }
                            append("!\n\nAll your data is immediately available.")
                        }
                        successDialogMessage = details
                        showSuccessDialog = true
                    }
                    is RestoreResult.Error -> {
                        errorDialogMessage = result.message
                        showErrorDialog = true
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Backup & Restore", onBack = onBack)

        // Hero Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "100% Private & On-Device",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your backups stay on your phone. No accounts or internet required. Moving to a new phone? Just share or copy the file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: 1-Click Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "One-Tap Backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Packages all your links, notes, auto-titles, and transcriptions into a single momostackbackup.json file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isBackingUp = true
                            val result = viewModel.createAndSaveBackupFile(context)
                            isBackingUp = false
                            result.onSuccess { res ->
                                lastBackupResult = res
                                Toast.makeText(
                                    context,
                                    "Backup complete! Saved to Downloads (${res.itemCount} items)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { err ->
                                errorDialogMessage = err.localizedMessage ?: "Failed to create backup"
                                showErrorDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBackingUp,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Safe Backup...")
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚡ Backup Now")
                    }
                }

                // If backup was just created, show confirmation & Share Button
                AnimatedVisibility(visible = lastBackupResult != null) {
                    lastBackupResult?.let { res ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ready in Downloads/MomoStack/${BackupManager.BACKUP_FILE_NAME}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SuccessGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val shareIntent = BackupManager.getShareBackupIntent(context, res.file)
                                    context.startActivity(Intent.createChooser(shareIntent, "Share MomoStack Backup"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send / Share Backup File")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: 1-Click Retrieve on Any Phone
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retrieve on Any Device",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Open MomoStack on any phone, tap below, and choose your momostackbackup.json file. All links, notes, and categories load automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        openDocumentLauncher.launch(
                            arrayOf(
                                "application/json",
                                "application/octet-stream",
                                "text/plain",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restoring Vault...")
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📂 Select Backup File")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Advanced / Manual Section Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showManualSection = !showManualSection }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Advanced / Clipboard Options",
                style = MaterialTheme.typography.labelLarge,
                color = Slate500
            )
            Text(
                text = if (showManualSection) "▲" else "▼",
                color = Slate500,
                fontSize = 12.sp
            )
        }

        AnimatedVisibility(visible = showManualSection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val json = viewModel.exportBackupJson(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MomoStack Backup", json))
                            Toast.makeText(context, "Raw backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Copy Raw JSON to Clipboard")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showManualRestoreDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Paste Raw JSON to Restore")
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎉", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Completed")
                    }
                },
                text = {
                    Text(
                        text = successDialogMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Awesome!")
                    }
                }
            )
        }

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Backup / Restore Error") },
                text = {
                    Text(
                        text = errorDialogMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Manual Paste Dialog
        if (showManualRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showManualRestoreDialog = false },
                title = { Text("Paste Raw Backup") },
                text = {
                    Column {
                        Text(
                            text = "Paste your raw JSON text below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualRestoreInput,
                            onValueChange = { manualRestoreInput = it },
                            placeholder = { Text("Paste JSON here...", color = Slate400) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = viewModel.restoreBackupJson(context, manualRestoreInput, clearExisting = false)
                                when (res) {
                                    is RestoreResult.Success -> {
                                        showManualRestoreDialog = false
                                        successDialogMessage = "Restored ${res.restoredItemsCount} links/notes and ${res.restoredCategoriesCount} categories!"
                                        showSuccessDialog = true
                                    }
                                    is RestoreResult.Error -> {
                                        showManualRestoreDialog = false
                                        errorDialogMessage = res.message
                                        showErrorDialog = true
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
                    TextButton(onClick = { showManualRestoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
