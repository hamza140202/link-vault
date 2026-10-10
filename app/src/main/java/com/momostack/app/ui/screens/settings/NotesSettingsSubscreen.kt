package com.momostack.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momostack.app.ui.theme.IndigoPrimary

@Composable
fun NotesSettingsSubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mascotWatcherEnabled by remember { mutableStateOf(true) }
    var mascotMood by remember { mutableStateOf("Playful") }
    var smartPasteConversion by remember { mutableStateOf(true) }
    var showCounters by remember { mutableStateOf(true) }

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

        ToggleRow(
            title = "Format-smart pasting",
            subtitle = "Preserves bold words, headers, and bullet points automatically",
            checked = smartPasteConversion,
            onCheckedChange = { smartPasteConversion = it }
        )

        ToggleRow(
            title = "Show word & character counters",
            subtitle = "Helpful counts at the top of your note screen",
            checked = showCounters,
            onCheckedChange = { showCounters = it }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
