package com.linkvault.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LinkCard(
    item: LinkItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed Thumbnail Box (80x60dp, 4:3, radius 12dp)
            CardThumbnailBox(item = item)

            Spacer(modifier = Modifier.width(12.dp))

            // Card Content Wrap
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Top row: Domain + Relative Time + Favorite Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (item.domain ?: "note").lowercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatRelativeTime(item.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Slate400,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) Color(0xFFEAB308) else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Title
                Text(
                    text = item.title.ifBlank { item.url.ifBlank { "Untitled Note" } },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Description / Note Snippet
                val snippetText = item.description?.takeIf { it.isNotBlank() } ?: item.notes?.takeIf { it.isNotBlank() }
                if (!snippetText.isNullOrBlank()) {
                    Text(
                        text = snippetText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: Category Pill + Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tagBg = when (item.category.lowercase()) {
                        "reading" -> Color(0xFFECFDF5)
                        "media" -> Color(0xFFFFF1F2)
                        "design" -> Color(0xFFFFFBEB)
                        "work" -> Color(0xFFEFF6FF)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val tagText = when (item.category.lowercase()) {
                        "reading" -> Color(0xFF065F46)
                        "media" -> Color(0xFF9F1239)
                        "design" -> Color(0xFF92400E)
                        "work" -> Color(0xFF1E40AF)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagBg
                    ) {
                        Text(
                            text = item.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = tagText
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.url.isNotBlank()) {
                            IconButton(
                                onClick = onOpenBrowser,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = "Open in browser",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onArchive,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Archive,
                                contentDescription = "Archive",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardThumbnailBox(
    item: LinkItem,
    modifier: Modifier = Modifier
) {
    val isNote = item.url.isBlank() || !item.notes.isNullOrBlank() && item.url.isBlank()
    val isYouTube = item.domain?.contains("youtube") == true || item.domain?.contains("youtu.be") == true
    val isGitHub = item.domain?.contains("github") == true

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isNote -> Color(0xFFFEF3C7)
                    isGitHub -> Color(0xFF181717)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!item.previewImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.previewImageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isYouTube) {
                // Red YouTube badge in corner (matching index.html)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF0000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(width = 20.dp, height = 14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        } else {
            // Semantic fallback icons matching index.html
            when {
                isNote -> {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = "Note",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                }
                isGitHub -> {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "GitHub",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                isYouTube -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF0000),
                        modifier = Modifier.size(width = 36.dp, height = 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "YouTube",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                else -> {
                    val domainInitial = item.domain?.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString()
                        ?: item.title.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString()
                        ?: "L"
                    Text(
                        text = domainInitial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarView(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search links, notes, domains...", color = Slate400) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Slate400)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = IndigoPrimary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun EmptyStateView(
    message: String,
    subMessage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MomoMascotView(
            size = 96.dp,
            animate = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Slate500,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
