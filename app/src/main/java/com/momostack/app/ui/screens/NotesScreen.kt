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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.momostack.app.util.RichTextFormatter

fun shareNoteAsText(context: Context, title: String, content: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            val subject = title.ifBlank { "MomoStack Note" }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            val fullText = if (title.isNotBlank() && !content.startsWith(title)) {
                "$title\n\n$content"
            } else {
                content
            }
            putExtra(Intent.EXTRA_TEXT, fullText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share note via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NotesScreen(
    viewModel: VaultViewModel,
    onNoteSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val noteItems = items.filter { !it.notes.isNullOrBlank() }
    val context = LocalContext.current

    val openNewNoteRequested by viewModel.openNewNoteEditor.collectAsState()
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<LinkItem?>(null) }

    LaunchedEffect(openNewNoteRequested) {
        if (openNewNoteRequested) {
            noteToEdit = null
            isCreatingNewNote = true
            viewModel.consumeOpenNewNote()
        }
    }

    if (isCreatingNewNote || noteToEdit != null) {
        FullPageNoteEditor(
            initialTitle = noteToEdit?.title ?: "",
            initialBody = noteToEdit?.notes ?: "",
            isNewNote = (noteToEdit == null),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Notes List or Empty State
                if (noteItems.isEmpty()) {
                    EmptyStateView(
                        message = "No notes yet 📝",
                        subMessage = "Tap the + button to jot down a thought, or paste anything from your clipboard.",
                        modifier = Modifier.weight(1f)
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
                                            // Share Button on Note Card
                                            IconButton(
                                                onClick = {
                                                    shareNoteAsText(context, item.title, noteBody)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Share,
                                                    contentDescription = "Share Note",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Delete Button
                                            IconButton(
                                                onClick = { viewModel.deleteItem(item.id) },
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
 * - Rich Markdown formatting toolbar (H1, H2, Bold, Italic, Lists, Tasks)
 * - Format-smart Paste button (preserves headers, subtitles, bold from clipboard HTML)
 * - Automatic formatting conversion on long-press paste
 * - Share note action to send note text to any app
 */
@Composable
fun FullPageNoteEditor(
    initialTitle: String,
    initialBody: String,
    isNewNote: Boolean,
    onBack: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var bodyValue by remember {
        mutableStateOf(TextFieldValue(text = initialBody, selection = TextRange(initialBody.length)))
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
        // Full Page Top Bar with Mascot
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Momo Mascot watching cursor
                    MomoMascotView(
                        size = 44.dp,
                        animate = true,
                        holdingCard = false,
                        isWriting = true,
                        lookOffsetX = lookX,
                        lookOffsetY = lookY,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Column {
                        Text(
                            text = if (isNewNote) "New Note" else "Edit Note",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$wordCount words · $charCount chars",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Smart Paste Button
                    IconButton(
                        onClick = { pasteFormattedFromClipboard() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = "Paste with formatting",
                            tint = IndigoPrimary
                        )
                    }

                    // Share Note button
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                shareNoteAsText(context, title, text)
                            } else {
                                Toast.makeText(context, "Note is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share note",
                            tint = Slate500
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
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Title and Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Note Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Format-Smart Body with Cursor-Watching and Long-Press Smart Formatting
            OutlinedTextField(
                value = bodyValue,
                onValueChange = { newValue ->
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
                                return@OutlinedTextField
                            }
                        }
                    }

                    bodyValue = newValue
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$wordCount words · $charCount chars",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
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
                                    return@OutlinedTextField
                                }
                            }
                        }
                        bodyValue = newValue
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
