package com.momostack.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momostack.app.data.model.LinkItem
import com.momostack.app.ui.components.EmptyStateView
import com.momostack.app.ui.components.LinkCard
import com.momostack.app.ui.components.SearchBarView
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.FilterMode
import com.momostack.app.ui.viewmodel.VaultViewModel

@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    onItemClick: (LinkItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current

    var showAddLinkDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<LinkItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        com.momostack.app.ui.components.MomoMascotView(
                            size = 36.dp,
                            animate = true,
                            holdingCard = true
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MomoStack",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Local-First Capture",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${stats.totalLinks} saved",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            SearchBarView(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )

            // Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedCategory != null) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("📁 $selectedCategory  ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                val filters = listOf(
                    Pair(FilterMode.ALL, "All"),
                    Pair(FilterMode.INSTAGRAM, "📸 Instagram"),
                    Pair(FilterMode.YOUTUBE, "▶️ YouTube"),
                    Pair(FilterMode.TWITTER, "𝕏 Twitter"),
                    Pair(FilterMode.THREADS, "🧵 Threads"),
                    Pair(FilterMode.GITHUB, "🐙 GitHub"),
                    Pair(FilterMode.HUGGINGFACE, "🤗 Hugging Face"),
                    Pair(FilterMode.FAVORITES, "Favorites"),
                    Pair(FilterMode.NOTES, "Notes"),
                    Pair(FilterMode.ARCHIVED, "Archived")
                )
                items(filters) { (mode, title) ->
                    val isSelected = selectedFilter == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(mode) },
                        label = { Text(title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Refresh banner for un-enriched or pending items
            val pendingCount = items.count { it.url.isNotBlank() && (it.previewImageUrl.isNullOrBlank() || it.status != com.momostack.app.data.model.ProcessingStatus.COMPLETED) }
            if (pendingCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IndigoPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            viewModel.refetchPendingItems()
                            Toast.makeText(context, "Re-fetching $pendingCount pending items in background...", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sync",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$pendingCount item(s) missing metadata / preview",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Tap to fetch ➔",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List or Empty State
            if (items.isEmpty()) {
                val emptyMsg = when (selectedFilter) {
                    FilterMode.INSTAGRAM -> "No Instagram links saved yet"
                    FilterMode.YOUTUBE -> "No YouTube videos saved yet"
                    FilterMode.TWITTER -> "No Twitter / X links saved yet"
                    FilterMode.THREADS -> "No Threads links saved yet"
                    FilterMode.GITHUB -> "No GitHub repositories saved yet"
                    FilterMode.HUGGINGFACE -> "No Hugging Face models saved yet"
                    FilterMode.FAVORITES -> "No favorite links yet"
                    FilterMode.NOTES -> "No notes captured yet"
                    FilterMode.ARCHIVED -> "No archived links"
                    FilterMode.ALL -> if (searchQuery.isNotBlank()) "No matching results" else "No saved links yet"
                }
                val emptySubMsg = when (selectedFilter) {
                    FilterMode.INSTAGRAM -> "Share any post or reel from Instagram to MomoStack to view thumbnails and captions."
                    FilterMode.YOUTUBE -> "Share any video or short from YouTube to MomoStack to capture thumbnails instantly."
                    FilterMode.TWITTER -> "Share any post from X/Twitter to MomoStack to capture tweets and media."
                    FilterMode.THREADS -> "Share any post from Threads to MomoStack to save conversations."
                    FilterMode.GITHUB -> "Share any GitHub repo to MomoStack to capture code previews, stars, and descriptions."
                    FilterMode.HUGGINGFACE -> "Share any model, space, or dataset from Hugging Face to MomoStack to save rich previews."
                    else -> if (searchQuery.isNotBlank()) "Try another keyword or domain search." else "Share any link from Chrome, YouTube, Instagram or tap + to save manually."
                }
                EmptyStateView(
                    message = emptyMsg,
                    subMessage = emptySubMsg,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val hasUrl = item.url.isNotBlank() && (item.url.startsWith("http://") || item.url.startsWith("https://"))
                        LinkCard(
                            item = item,
                            onClick = {
                                if (hasUrl) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        onItemClick(item)
                                    }
                                } else {
                                    noteToEdit = item
                                }
                            },
                            onLongClick = {
                                onItemClick(item)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onArchive = { viewModel.toggleArchive(item) },
                            onDelete = { viewModel.deleteItem(item.id) },
                            onRefresh = {
                                viewModel.refetchItem(item.id)
                                Toast.makeText(context, "Re-fetching metadata for ${item.title.take(20)}...", Toast.LENGTH_SHORT).show()
                            },
                            onOpenBrowser = {
                                if (item.url.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button (+) for manual URL capture
        FloatingActionButton(
            onClick = { showAddLinkDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = IndigoPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Link Manually", modifier = Modifier.size(24.dp))
        }

        // Add Link Dialog
        if (showAddLinkDialog) {
            AddLinkDialog(
                onDismiss = { showAddLinkDialog = false },
                onSave = { url, title, notes ->
                    viewModel.saveDirectLink(url, title, notes)
                    Toast.makeText(context, "Link saved to MomoStack", Toast.LENGTH_SHORT).show()
                    showAddLinkDialog = false
                }
            )
        }

        // Note Viewer / Editor Full-Page Setup (when tapped directly)
        if (noteToEdit != null) {
            val currentNote = noteToEdit!!
            FullPageNoteEditor(
                initialTitle = currentNote.title,
                initialBody = currentNote.notes ?: "",
                isNewNote = false,
                onBack = { noteToEdit = null },
                onSave = { _, updatedBody ->
                    viewModel.updateNotes(currentNote.id, updatedBody)
                    Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
                    noteToEdit = null
                }
            )
        }
    }
}

@Composable
fun AddLinkDialog(
    onDismiss: () -> Unit,
    onSave: (url: String, title: String?, notes: String?) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Link to MomoStack", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://example.com/...", color = Slate400) },
                    label = { Text("URL") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).coerceToText(context).toString().trim()
                                if (text.isNotBlank()) {
                                    urlInput = text
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Assignment, contentDescription = "Paste", tint = IndigoPrimary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("Leave blank to auto-detect", color = Slate400) },
                    label = { Text("Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("Add notes, tags, or thoughts...", color = Slate400) },
                    label = { Text("Personal Notes (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        onSave(urlInput, titleInput.takeIf { it.isNotBlank() }, notesInput.takeIf { it.isNotBlank() })
                    } else {
                        Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
