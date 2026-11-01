package com.momostack.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momostack.app.data.model.LinkItem
import com.momostack.app.ui.components.EmptyStateView
import com.momostack.app.ui.components.MomoMascotView
import com.momostack.app.ui.components.formatRelativeTime
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel
import com.momostack.app.util.NoteExporter
import com.momostack.app.util.RichTextFormatter
import kotlinx.coroutines.launch

/**
 * State snapshot manager for Undo and Redo in note editing.
 */
class NoteUndoManager(initialTitle: String, initialText: String, initialPos: Int) {
    data class Snapshot(val title: String, val text: String, val pos: Int)

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var lastRecordedTime = 0L

    init {
        undoStack.addLast(Snapshot(initialTitle, initialText, initialPos))
    }

    val canUndo: Boolean get() = undoStack.size > 1
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun recordChange(title: String, text: String, pos: Int, force: Boolean = false) {
        val current = undoStack.lastOrNull()
        if (current != null && current.title == title && current.text == text) {
            return
        }

        val now = System.currentTimeMillis()
        val isSignificant = force ||
                current == null ||
                current.title != title ||
                Math.abs(current.text.length - text.length) > 1 ||
                (text.isNotEmpty() && text.last().isWhitespace()) ||
                (now - lastRecordedTime > 400)

        if (isSignificant) {
            undoStack.addLast(Snapshot(title, text, pos))
            if (undoStack.size > 100) {
                undoStack.removeFirst()
            }
            redoStack.clear()
            lastRecordedTime = now
        }
    }

    fun undo(): Snapshot? {
        if (!canUndo) return null
        val current = undoStack.removeLast()
        redoStack.addLast(current)
        return undoStack.lastOrNull()
    }

    fun redo(): Snapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(next)
        return next
    }
}

/**
 * Modern Export & Share Note Dialog.
 * Allows exporting full notes as:
 * - Text Document (.txt)
 * - Microsoft Word / Office Document (.doc) with headings, styles & margins
 * - Markdown Document (.md)
 * - Web Document (.html)
 * With 1-tap options to Share to apps or Save to device Downloads folder.
 */
@Composable
fun ExportNoteDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(NoteExporter.ExportFormat.TXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = IndigoPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Export Note",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Export document with full formatting",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Note Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val wordCount = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
                        Text(
                            text = "$wordCount words · ${content.length} characters",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SELECT DOCUMENT FORMAT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(8.dp))

                NoteExporter.ExportFormat.values().forEach { format ->
                    val isSelected = (selectedFormat == format)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedFormat = format },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = format.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = format.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = format.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        NoteExporter.saveNoteToDownloads(context, title, content, selectedFormat)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }

                Button(
                    onClick = {
                        NoteExporter.shareNoteAsFile(context, title, content, selectedFormat)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share File")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotesScreen(
    viewModel: VaultViewModel,
    onNoteSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val appearancePrefs by viewModel.appearancePrefs.collectAsState()
    val noteItems = items.filter { !it.notes.isNullOrBlank() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val openNewNoteRequested by viewModel.openNewNoteEditor.collectAsState()
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<LinkItem?>(null) }
    var noteToExport by remember { mutableStateOf<LinkItem?>(null) }
    var lastDeletedItem by remember { mutableStateOf<LinkItem?>(null) }

    LaunchedEffect(openNewNoteRequested) {
        if (openNewNoteRequested) {
            noteToEdit = null
            isCreatingNewNote = true
            viewModel.consumeOpenNewNote()
        }
    }

    if (noteToExport != null) {
        ExportNoteDialog(
            title = noteToExport!!.title,
            content = noteToExport!!.notes ?: "",
            onDismiss = { noteToExport = null }
        )
    }

    if (isCreatingNewNote || noteToEdit != null) {
        FullPageNoteEditor(
            initialTitle = noteToEdit?.title ?: "",
            initialBody = noteToEdit?.notes ?: "",
            isNewNote = (noteToEdit == null),
            animate = appearancePrefs.smoothAnimations,
            onBack = {
                isCreatingNewNote = false
                noteToEdit = null
            },
            onSave = { title, body ->
                if (noteToEdit != null) {
                    viewModel.updateNotes(noteToEdit!!.id, body)
                    Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveDirectNote(body, title)
                    Toast.makeText(context, "Note saved to MomoStack", Toast.LENGTH_SHORT).show()
                    onNoteSaved()
                }
                isCreatingNewNote = false
                noteToEdit = null
            },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = IndigoPrimary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Notes",
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MomoStack Notes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "A cozy place for your thoughts & notes",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate500
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Undo Delete button if a note was recently deleted
                        if (lastDeletedItem != null) {
                            Surface(
                                color = IndigoPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    val itemToRestore = lastDeletedItem
                                    if (itemToRestore != null) {
                                        viewModel.saveDirectNote(itemToRestore.notes ?: "", itemToRestore.title)
                                        Toast.makeText(context, "Note restored! ↩️", Toast.LENGTH_SHORT).show()
                                        lastDeletedItem = null
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo Delete",
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Undo Delete",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = IndigoPrimary
                                    )
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${noteItems.size} notes",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Notes List or Empty State
                if (noteItems.isEmpty()) {
                    EmptyStateView(
                        message = "No notes yet 📝",
                        subMessage = "Tap the + button to jot down a thought, or paste anything from your clipboard.",
                        modifier = Modifier.weight(1f),
                        animate = appearancePrefs.smoothAnimations
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(noteItems, key = { it.id }) { item ->
                            val noteBody = item.notes ?: ""
                            val wordCount = if (noteBody.isBlank()) 0 else noteBody.trim().split("\\s+".toRegex()).size

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { noteToEdit = item },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.title.ifBlank { "Untitled Note" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // 1-Click Copy Note
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val textToCopy = if (item.title.isNotBlank() && !noteBody.startsWith(item.title)) {
                                                        "${item.title}\n\n$noteBody"
                                                    } else {
                                                        noteBody
                                                    }
                                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("MomoStack Note", textToCopy))
                                                    Toast.makeText(context, "📋 Note copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy Note",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Share / Export Button on Note Card (Exports full note as TXT, DOC, MD, etc.)
                                            IconButton(
                                                onClick = {
                                                    noteToExport = item
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Share,
                                                    contentDescription = "Export & Share Note",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Delete Button with 1-tap Undo support
                                            IconButton(
                                                onClick = {
                                                    val toDelete = item
                                                    lastDeletedItem = toDelete
                                                    viewModel.deleteItem(item.id)
                                                    coroutineScope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Note deleted",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.saveDirectNote(toDelete.notes ?: "", toDelete.title)
                                                            lastDeletedItem = null
                                                            Toast.makeText(context, "Note restored! ↩️", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = RichTextFormatter.parseMarkdownToAnnotatedString(noteBody),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$wordCount words · ${noteBody.length} chars",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                        Text(
                                            text = formatRelativeTime(item.updatedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar Host for Undo Delete Notifications
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
            )

            // Floating Action Button (+) for Full-Page Note Creation
            FloatingActionButton(
                onClick = { isCreatingNewNote = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create New Note", modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Dedicated Full-Page Note Editor with:
 * - Real-time Momo mascot watching the cursor as you write
 * - Undo & Redo buttons in the top bar and editing toolbar
 * - Export & Share Note action dialog (Export as .txt, .doc, .md, .html)
 * - Rich Markdown formatting toolbar (H1, H2, Bold, Italic, Bullet Lists, Quotes, Code)
 * - Format-smart Paste button (preserves headers, subtitles, bold from clipboard HTML)
 * - Automatic formatting conversion on long-press paste
 */
@Composable
fun FullPageNoteEditor(
    initialTitle: String,
    initialBody: String,
    isNewNote: Boolean,
    onBack: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var bodyValue by remember {
        mutableStateOf(TextFieldValue(text = initialBody, selection = TextRange(initialBody.length)))
    }
    var showExportDialog by remember { mutableStateOf(false) }

    // Undo / Redo Manager Instance
    val undoManager = remember {
        NoteUndoManager(initialTitle, initialBody, initialBody.length)
    }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var isUndoRedoActive by remember { mutableStateOf(false) }

    fun executeUndo() {
        val previous = undoManager.undo()
        if (previous != null) {
            isUndoRedoActive = true
            title = previous.title
            val safePos = previous.pos.coerceIn(0, previous.text.length)
            bodyValue = TextFieldValue(previous.text, TextRange(safePos))
            canUndo = undoManager.canUndo
            canRedo = undoManager.canRedo
        }
    }

    fun executeRedo() {
        val next = undoManager.redo()
        if (next != null) {
            isUndoRedoActive = true
            title = next.title
            val safePos = next.pos.coerceIn(0, next.text.length)
            bodyValue = TextFieldValue(next.text, TextRange(safePos))
            canUndo = undoManager.canUndo
            canRedo = undoManager.canRedo
        }
    }

    fun insertFormatting(prefix: String, suffix: String = "") {
        val currentText = bodyValue.text
        val selStart = bodyValue.selection.min
        val selEnd = bodyValue.selection.max
        val selectedChunk = currentText.substring(selStart, selEnd)

        val replacement = "$prefix$selectedChunk$suffix"
        val newText = currentText.replaceRange(selStart, selEnd, replacement)
        val newCursor = if (selectedChunk.isEmpty()) selStart + prefix.length else selStart + replacement.length
        bodyValue = TextFieldValue(newText, TextRange(newCursor))
        undoManager.recordChange(title, newText, newCursor, force = true)
        canUndo = undoManager.canUndo
        canRedo = undoManager.canRedo
    }

    if (showExportDialog) {
        ExportNoteDialog(
            title = title,
            content = bodyValue.text,
            onDismiss = { showExportDialog = false }
        )
    }

    BackHandler {
        onBack()
    }

    // Dynamic cursor tracking for Momo mascot pupil offset
    val text = bodyValue.text
    val cursorPos = bodyValue.selection.start
    val textBeforeCursor = text.take(cursorPos)
    val currentRow = textBeforeCursor.count { it == '\n' }
    val totalRows = maxOf(1, text.count { it == '\n' } + 1)
    val currentLineStart = textBeforeCursor.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
    val currentCol = cursorPos - currentLineStart
    val currentLineLen = maxOf(1, text.substring(currentLineStart).substringBefore('\n').length)

    val rawX = if (currentLineLen > 0) (currentCol.toFloat() / currentLineLen) else 0.5f
    val rawY = if (totalRows > 0) (currentRow.toFloat() / totalRows) else 0.5f

    val lookX = ((rawX - 0.5f) * 2f).coerceIn(-1f, 1f)
    val lookY = ((rawY - 0.5f) * 2f).coerceIn(-1f, 1f)

    val wordCount = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    val charCount = text.length

    fun pasteFormattedFromClipboard() {
        val formatted = RichTextFormatter.smartFormatClipboard(context)
        if (!formatted.isNullOrBlank()) {
            val currentText = bodyValue.text
            val selStart = bodyValue.selection.min
            val selEnd = bodyValue.selection.max
            val replaced = currentText.replaceRange(selStart, selEnd, formatted)
            val newPos = selStart + formatted.length
            bodyValue = TextFieldValue(replaced, TextRange(newPos))
            undoManager.recordChange(title, replaced, newPos, force = true)
            canUndo = undoManager.canUndo
            canRedo = undoManager.canRedo
            Toast.makeText(context, "Pasted with formatting (bold, links, headers kept) ✨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Full Page Top Bar with Mascot, Undo, Redo, Export & Save
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Momo Mascot watching cursor
                    MomoMascotView(
                        size = 38.dp,
                        animate = animate,
                        holdingCard = false,
                        isWriting = true,
                        lookOffsetX = lookX,
                        lookOffsetY = lookY,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (isNewNote) "New Note" else title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$wordCount words · $charCount chars",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Undo Button
                    IconButton(
                        onClick = { executeUndo() },
                        enabled = canUndo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) IndigoPrimary else Slate400.copy(alpha = 0.35f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { executeRedo() },
                        enabled = canRedo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) IndigoPrimary else Slate400.copy(alpha = 0.35f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Export & Share Note button (exports as TXT, DOC, MD, etc.)
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                showExportDialog = true
                            } else {
                                Toast.makeText(context, "Note is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export note as file",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSave(title, text)
                            } else {
                                Toast.makeText(context, "Please enter some note content", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Title and Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Note Title
            OutlinedTextField(
                value = title,
                onValueChange = { newTitle ->
                    title = newTitle
                    if (!isUndoRedoActive) {
                        undoManager.recordChange(newTitle, bodyValue.text, bodyValue.selection.start)
                        canUndo = undoManager.canUndo
                        canRedo = undoManager.canRedo
                    } else {
                        isUndoRedoActive = false
                    }
                },
                placeholder = {
                    Text(
                        "Note Title (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate400
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Markdown & Undo/Redo Action Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo Chip Button
                    Surface(
                        color = if (canUndo) IndigoPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(enabled = canUndo) { executeUndo() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) IndigoPrimary else Slate400.copy(alpha = 0.3f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Undo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (canUndo) IndigoPrimary else Slate400.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Redo Chip Button
                    Surface(
                        color = if (canRedo) IndigoPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(enabled = canRedo) { executeRedo() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) IndigoPrimary else Slate400.copy(alpha = 0.3f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Redo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (canRedo) IndigoPrimary else Slate400.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Text("|", color = Slate400.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.width(4.dp))

                    // Smart Paste
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { pasteFormattedFromClipboard() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Assignment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Formatted", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary)
                        }
                    }

                    // H1 Heading
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("\n# ") }
                    ) {
                        Text("H1", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }

                    // H2 Heading
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("\n## ") }
                    ) {
                        Text("H2", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Bold (**text**)
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("**", "**") }
                    ) {
                        Icon(Icons.Filled.FormatBold, contentDescription = "Bold", modifier = Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Italic (*text*)
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("*", "*") }
                    ) {
                        Icon(Icons.Filled.FormatItalic, contentDescription = "Italic", modifier = Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Bullet List
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("\n- ") }
                    ) {
                        Icon(Icons.Filled.FormatListBulleted, contentDescription = "List", modifier = Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Blockquote
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("\n> ") }
                    ) {
                        Icon(Icons.Filled.FormatQuote, contentDescription = "Quote", modifier = Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Code
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { insertFormatting("`", "`") }
                    ) {
                        Icon(Icons.Filled.Code, contentDescription = "Code", modifier = Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Format-Smart Body with Cursor-Watching and Long-Press Smart Formatting
            OutlinedTextField(
                value = bodyValue,
                onValueChange = { newValue ->
                    if (isUndoRedoActive) {
                        isUndoRedoActive = false
                        bodyValue = newValue
                        return@OutlinedTextField
                    }

                    val oldText = bodyValue.text
                    val newText = newValue.text

                    // Check if multiple characters were inserted at once (long-press paste event)
                    if (newText.length > oldText.length + 1) {
                        val selStart = bodyValue.selection.min
                        val insertedLength = newText.length - (oldText.length - (bodyValue.selection.max - selStart))
                        if (insertedLength > 0 && selStart + insertedLength <= newText.length) {
                            val insertedChunk = newText.substring(selStart, selStart + insertedLength)
                            val converted = RichTextFormatter.smartConvertPastedText(insertedChunk, context)
                            if (converted != insertedChunk) {
                                val replacedText = newText.replaceRange(selStart, selStart + insertedLength, converted)
                                val newCursor = selStart + converted.length
                                bodyValue = TextFieldValue(replacedText, TextRange(newCursor))
                                undoManager.recordChange(title, replacedText, newCursor, force = true)
                                canUndo = undoManager.canUndo
                                canRedo = undoManager.canRedo
                                return@OutlinedTextField
                            }
                        }
                    }

                    bodyValue = newValue
                    undoManager.recordChange(title, newText, newValue.selection.start)
                    canUndo = undoManager.canUndo
                    canRedo = undoManager.canRedo
                },
                placeholder = {
                    Text(
                        "Write what's on your mind, or paste notes and reading lists...",
                        color = Slate400,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                visualTransformation = RichTextFormatter.createMarkdownVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Backward-compatible NoteEditorDialog for quick popup dialog usages.
 * Now equipped with Undo, Redo, and Document Export capabilities.
 */
@Composable
fun NoteEditorDialog(
    initialTitle: String,
    initialBody: String,
    dialogTitle: String,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var bodyValue by remember {
        mutableStateOf(TextFieldValue(text = initialBody, selection = TextRange(initialBody.length)))
    }
    var showExportDialog by remember { mutableStateOf(false) }

    val undoManager = remember { NoteUndoManager(initialTitle, initialBody, initialBody.length) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var isUndoRedoActive by remember { mutableStateOf(false) }

    fun executeUndo() {
        val previous = undoManager.undo()
        if (previous != null) {
            isUndoRedoActive = true
            title = previous.title
            val safePos = previous.pos.coerceIn(0, previous.text.length)
            bodyValue = TextFieldValue(previous.text, TextRange(safePos))
            canUndo = undoManager.canUndo
            canRedo = undoManager.canRedo
        }
    }

    fun executeRedo() {
        val next = undoManager.redo()
        if (next != null) {
            isUndoRedoActive = true
            title = next.title
            val safePos = next.pos.coerceIn(0, next.text.length)
            bodyValue = TextFieldValue(next.text, TextRange(safePos))
            canUndo = undoManager.canUndo
            canRedo = undoManager.canRedo
        }
    }

    if (showExportDialog) {
        ExportNoteDialog(
            title = title,
            content = bodyValue.text,
            onDismiss = { showExportDialog = false }
        )
    }

    val wordCount = if (bodyValue.text.isBlank()) 0 else bodyValue.text.trim().split("\\s+".toRegex()).size
    val charCount = bodyValue.text.length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dialogTitle, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { executeUndo() }, enabled = canUndo, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) IndigoPrimary else Slate400.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = { executeRedo() }, enabled = canRedo, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) IndigoPrimary else Slate400.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export Note",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        if (!isUndoRedoActive) {
                            undoManager.recordChange(newTitle, bodyValue.text, bodyValue.selection.start)
                            canUndo = undoManager.canUndo
                            canRedo = undoManager.canRedo
                        } else {
                            isUndoRedoActive = false
                        }
                    },
                    placeholder = { Text("Title (Optional)", color = Slate400) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val formatted = RichTextFormatter.smartFormatClipboard(context)
                        if (!formatted.isNullOrBlank()) {
                            val currentText = bodyValue.text
                            val newText = if (currentText.isBlank()) formatted else "$currentText\n\n$formatted"
                            bodyValue = TextFieldValue(newText, TextRange(newText.length))
                            undoManager.recordChange(title, newText, newText.length, force = true)
                            canUndo = undoManager.canUndo
                            canRedo = undoManager.canRedo
                            Toast.makeText(context, "Pasted formatted text (${formatted.length} chars)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Formatted Clipboard", color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bodyValue,
                    onValueChange = { newValue ->
                        if (isUndoRedoActive) {
                            isUndoRedoActive = false
                            bodyValue = newValue
                            return@OutlinedTextField
                        }

                        val oldText = bodyValue.text
                        val newText = newValue.text
                        if (newText.length > oldText.length + 1) {
                            val selStart = bodyValue.selection.min
                            val insertedLength = newText.length - (oldText.length - (bodyValue.selection.max - selStart))
                            if (insertedLength > 0 && selStart + insertedLength <= newText.length) {
                                val insertedChunk = newText.substring(selStart, selStart + insertedLength)
                                val converted = RichTextFormatter.smartConvertPastedText(insertedChunk, context)
                                if (converted != insertedChunk) {
                                    val replacedText = newText.replaceRange(selStart, selStart + insertedLength, converted)
                                    val newCursor = selStart + converted.length
                                    bodyValue = TextFieldValue(replacedText, TextRange(newCursor))
                                    undoManager.recordChange(title, replacedText, newCursor, force = true)
                                    canUndo = undoManager.canUndo
                                    canRedo = undoManager.canRedo
                                    return@OutlinedTextField
                                }
                            }
                        }
                        bodyValue = newValue
                        undoManager.recordChange(title, newText, newValue.selection.start)
                        canUndo = undoManager.canUndo
                        canRedo = undoManager.canRedo
                    },
                    placeholder = { Text("Write your thoughts or paste rich documents...", color = Slate400) },
                    visualTransformation = RichTextFormatter.createMarkdownVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bodyValue.text.isNotBlank()) {
                        onSave(title, bodyValue.text)
                    } else {
                        Toast.makeText(context, "Please enter some note text", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
