package com.momostack.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momostack.app.backup.RestoreResult
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

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
