package com.momostack.app.feature.screenshotvault.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import com.momostack.app.feature.screenshotvault.service.ScreenshotPermissionHelper
import com.momostack.app.ui.components.MomoMascotView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotVaultScreen(
    onNavigateBack: () -> Unit,
    onSaveAsNote: (String) -> Unit,
    viewModel: ScreenshotVaultViewModel = viewModel()
) {
    val context = LocalContext.current
    val screenshots by viewModel.screenshots.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAutoCaptureEnabled by viewModel.isAutoCaptureEnabled.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val selectedScreenshot by viewModel.selectedScreenshot.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.importScreenshot(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val mediaGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (mediaGranted) {
            viewModel.toggleAutoCapture(context, true)
            Toast.makeText(context, "Auto-Capture activated!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.toggleAutoCapture(context, false)
            Toast.makeText(context, "Storage permission required for auto-capture", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Screenshot Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Text Extractor (Labs)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Import Screenshot",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Auto-Capture Toggle Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Capture Screenshots",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically saves screenshots & extracts text via on-device AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoCaptureEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (ScreenshotPermissionHelper.hasAllRequiredPermissions(context)) {
                                    viewModel.toggleAutoCapture(context, true)
                                } else {
                                    showPermissionDialog = true
                                }
                            } else {
                                viewModel.toggleAutoCapture(context, false)
                            }
                        }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search text inside screenshots...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            // Gallery / List Content
            if (screenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MomoMascotView(
                            size = 130.dp,
                            animate = true,
                            holdingCard = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching screenshots found" else "No screenshots saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for different words" else "Turn on Auto-Capture or tap the import button to extract text from your photos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(screenshots, key = { it.id }) { screenshot ->
                        ScreenshotCard(
                            screenshot = screenshot,
                            onClick = { viewModel.selectScreenshot(screenshot) },
                            onCopyText = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot Text", screenshot.extractedText))
                                Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onSaveToNotes = {
                                onSaveAsNote(screenshot.extractedText)
                                Toast.makeText(context, "Saved to Momo Notes!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { viewModel.deleteScreenshot(screenshot) }
                        )
                    }
                }
            }
        }
    }

    // Split / Detail Modal Sheet
    selectedScreenshot?.let { screenshot ->
        ScreenshotDetailDialog(
            screenshot = screenshot,
            onDismiss = { viewModel.selectScreenshot(null) },
            onUpdateText = { newText ->
                viewModel.updateNotes(screenshot.id, newText)
            },
            onCopyText = { textToCopy ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Screenshot Text", textToCopy))
                Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            onSaveToNotes = { textToSave ->
                onSaveAsNote(textToSave)
                Toast.makeText(context, "Saved to Momo Notes!", Toast.LENGTH_SHORT).show()
            },
            onShare = { textToShare ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textToShare)
                }
                context.startActivity(Intent.createChooser(intent, "Share Extracted Text"))
            },
            onDelete = {
                viewModel.deleteScreenshot(screenshot)
                viewModel.selectScreenshot(null)
            }
        )
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "Permission Needed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "To automatically detect new screenshots and notify you when text extraction is complete, MomoStack needs access to your Photos and Notifications."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(ScreenshotPermissionHelper.getRequiredPermissions())
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScreenshotCard(
    screenshot: ScreenshotEntity,
    onClick: () -> Unit,
    onCopyText: () -> Unit,
    onSaveToNotes: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screenshot Thumbnail
            AsyncImage(
                model = File(screenshot.localImagePath),
                contentDescription = "Screenshot Thumbnail",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Text Preview & Date
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = remember(screenshot.createdAt) {
                        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(screenshot.createdAt))
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "${screenshot.wordCount} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = screenshot.extractedText.ifBlank { "No text detected in screenshot" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions row
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (screenshot.extractedText.isNotBlank()) {
                        IconButton(onClick = onCopyText, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onSaveToNotes, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = "Save to Notes",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotDetailDialog(
    screenshot: ScreenshotEntity,
    onDismiss: () -> Unit,
    onUpdateText: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onSaveToNotes: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editableText by remember(screenshot.id) { mutableStateOf(screenshot.extractedText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screenshot Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Screenshot Preview
            AsyncImage(
                model = File(screenshot.localImagePath),
                contentDescription = "Full Screenshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onCopyText(editableText) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy All")
                }

                Button(
                    onClick = { onSaveToNotes(editableText) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Note")
                }

                IconButton(
                    onClick = { onShare(editableText) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transcription Body Box (Editable & Selectable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted Text (${editableText.length} chars):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to edit or select part",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = editableText,
                onValueChange = { newText ->
                    editableText = newText
                    onUpdateText(newText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 340.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("No readable text detected. You can type or edit here...") },
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
