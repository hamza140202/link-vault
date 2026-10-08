package com.linkvault.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.theme.Slate500
import com.linkvault.app.ui.viewmodel.VaultViewModel

@Composable
fun NotesScreen(
    viewModel: VaultViewModel,
    onNoteSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    val wordCount = if (noteBody.isBlank()) 0 else noteBody.trim().split("\\s+".toRegex()).size
    val charCount = noteBody.length

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Quick Note",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "First-class local note capture",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$wordCount words · $charCount chars",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = Slate500
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Field
        OutlinedTextField(
            value = noteTitle,
            onValueChange = { noteTitle = it },
            placeholder = { Text("Note Title (optional)", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Clipboard Paste Action Button
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val pasted = clip.getItemAt(0).coerceToText(context).toString()
                    if (pasted.isNotBlank()) {
                        noteBody = if (noteBody.isBlank()) pasted else "$noteBody\n\n$pasted"
                        Toast.makeText(context, "Pasted ${pasted.length} characters from clipboard", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Nothing on clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Assignment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paste from Clipboard", color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Body Text Field
        OutlinedTextField(
            value = noteBody,
            onValueChange = { noteBody = it },
            placeholder = { Text("Type or paste large documents, code, logs, or thoughts...\nSupports full Unicode and formatting.", color = Slate400) },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (noteBody.isNotBlank()) {
                    viewModel.saveDirectNote(noteBody, noteTitle)
                    noteTitle = ""
                    noteBody = ""
                    Toast.makeText(context, "Note captured in Vault", Toast.LENGTH_SHORT).show()
                    onNoteSaved()
                } else {
                    Toast.makeText(context, "Please enter some note content", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to Vault")
        }
    }
}
