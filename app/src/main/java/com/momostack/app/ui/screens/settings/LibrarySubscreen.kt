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
import androidx.compose.material3.HorizontalDivider
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
import com.momostack.app.ui.theme.Slate500

@Composable
fun LibrarySubscreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var defaultLaunchView by remember { mutableStateOf("All Links") }
    var sortOrder by remember { mutableStateOf("Newest First") }
    var showMomoEncouragement by remember { mutableStateOf(true) }
    var autoClearArchived by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Your Collection", onBack = onBack)

        Text(
            text = "When you open MomoStack",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose which shelf shows up first",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All Links", "Favorites", "Notes").forEach { view ->
                FilterChip(
                    selected = defaultLaunchView == view,
                    onClick = { defaultLaunchView = view },
                    label = { Text(view) },
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
