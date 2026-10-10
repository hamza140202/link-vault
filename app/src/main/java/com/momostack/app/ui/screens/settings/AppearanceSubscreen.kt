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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate500
import com.momostack.app.ui.viewmodel.VaultViewModel

@Composable
fun AppearanceSubscreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.appearancePrefs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SubscreenHeader(title = "Appearance", onBack = onBack)

        Text(
            text = "Look & Feel",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Match your phone or pick your preferred style",
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
                    selected = prefs.themeMode == option,
                    onClick = { viewModel.setThemeMode(option) },
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

        ToggleRow(
            title = "Match phone wallpaper colors",
            subtitle = "Tints buttons and accents to match your home screen",
            checked = prefs.dynamicColor,
            onCheckedChange = { viewModel.setDynamicColor(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Card Size",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose how roomy or snug your cards feel",
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
                    selected = prefs.cardDensity == density,
                    onClick = { viewModel.setCardDensity(density) },
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
            title = "Show photo previews",
            subtitle = "Displays photos for links and social posts",
            checked = prefs.showThumbnails,
            onCheckedChange = { viewModel.setShowThumbnails(it) }
        )

        ToggleRow(
            title = "Show website name",
            subtitle = "Shows where the link comes from (e.g. youtube.com)",
            checked = prefs.showDomain,
            onCheckedChange = { viewModel.setShowDomain(it) }
        )

        ToggleRow(
            title = "Show quick summaries",
            subtitle = "A short reading snippet under each title",
            checked = prefs.showDescription,
            onCheckedChange = { viewModel.setShowDescription(it) }
        )

        ToggleRow(
            title = "Playful animations",
            subtitle = "Smooth transitions and gentle bounces",
            checked = prefs.smoothAnimations,
            onCheckedChange = { viewModel.setSmoothAnimations(it) }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
