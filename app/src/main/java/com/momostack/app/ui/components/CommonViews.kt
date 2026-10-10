package com.momostack.app.ui.components

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
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import com.momostack.app.data.model.LinkItem
import com.momostack.app.ui.theme.IndigoPrimary
import com.momostack.app.ui.theme.Slate400
import com.momostack.app.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkCard(
    item: LinkItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onOpenBrowser: () -> Unit,
    onShare: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    cardDensity: String = "Comfortable",
    showThumbnails: Boolean = true,
    showDomain: Boolean = true,
    showDescription: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardPadding = when (cardDensity) {
        "Compact" -> 8.dp
        "Spacious" -> 16.dp
        else -> 12.dp
    }
    val (thumbWidth, thumbHeight) = when (cardDensity) {
        "Compact" -> 64.dp to 48.dp
        "Spacious" -> 96.dp to 72.dp
        else -> 80.dp to 60.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showThumbnails) {
                CardThumbnailBox(item = item, width = thumbWidth, height = thumbHeight)
                Spacer(modifier = Modifier.width(if (cardDensity == "Compact") 8.dp else 12.dp))
            }

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
                    if (showDomain) {
                        Text(
                            text = (item.domain ?: "note").lowercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

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
                    maxLines = if (cardDensity == "Compact") 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Description / Note Snippet
                val snippetText = item.description?.takeIf { it.isNotBlank() } ?: item.notes?.takeIf { it.isNotBlank() }
                if (showDescription && !snippetText.isNullOrBlank()) {
                    Text(
                        text = snippetText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Slate500,
                        maxLines = if (cardDensity == "Spacious") 2 else 1,
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
                        // 1. Refresh icon: visible if metadata/thumbnail is not yet enriched or failed
                        val needsEnrichment = item.url.isNotBlank() && (item.previewImageUrl.isNullOrBlank() || item.status != com.momostack.app.data.model.ProcessingStatus.COMPLETED)
                        if (needsEnrichment && onRefresh != null) {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh metadata & thumbnail",
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // 2. Share icon: shares to anyone or copies link
                        IconButton(
                            onClick = {
                                if (onShare != null) {
                                    onShare()
                                } else {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        val shareText = if (item.url.isNotBlank()) {
                                            if (item.title.isNotBlank()) "${item.title}\n${item.url}" else item.url
                                        } else {
                                            item.notes ?: item.title
                                        }
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_TITLE, item.title)
                                        type = "text/plain"
                                    }
                                    val shareChooser = Intent.createChooser(sendIntent, "Share link")
                                    context.startActivity(shareChooser)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // 3. Open in browser (if URL is present)
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

                        // 4. Archive
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

                        // 5. Delete
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
    width: androidx.compose.ui.unit.Dp = 80.dp,
    height: androidx.compose.ui.unit.Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val isNote = item.url.isBlank() || !item.notes.isNullOrBlank() && item.url.isBlank()
    val isYouTube = item.domain?.contains("youtube") == true || item.domain?.contains("youtu.be") == true || item.category.equals("YouTube", ignoreCase = true)
    val isInstagram = item.domain?.contains("instagram") == true || item.category.equals("Instagram", ignoreCase = true)
    val isTwitter = item.domain?.contains("twitter") == true || item.domain?.contains("x.com") == true || item.category.contains("Twitter", ignoreCase = true)
    val isThreads = item.domain?.contains("threads") == true || item.category.equals("Threads", ignoreCase = true)
    val isGitHub = item.domain?.contains("github") == true || item.category.equals("GitHub", ignoreCase = true)
    val isHuggingFace = item.domain?.contains("huggingface.co") == true || item.category.equals("Hugging Face", ignoreCase = true)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isNote -> Color(0xFFFEF3C7)
                    isGitHub -> Color(0xFF181717)
                    isHuggingFace -> Color(0xFFFFFBEB)
                    isInstagram -> Color(0xFFFDF2F8)
                    isTwitter -> Color(0xFFF0F9FF)
                    isThreads -> Color(0xFFF8FAFC)
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
            // Platform corner badge
            val badgeColor = when {
                isYouTube -> Color(0xFFFF0000)
                isInstagram -> Color(0xFFE1306C)
                isTwitter -> Color(0xFF1DA1F2)
                isThreads -> Color(0xFF000000)
                isGitHub -> Color(0xFF24292E)
                isHuggingFace -> Color(0xFFFF9D00)
                else -> null
            }
            val badgeText = when {
                isYouTube -> "▶"
                isInstagram -> "IG"
                isTwitter -> "𝕏"
                isThreads -> "@"
                isGitHub -> "GH"
                isHuggingFace -> "🤗"
                else -> null
            }
            if (badgeColor != null && badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(width = 20.dp, height = 14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
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
                isInstagram -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE1306C),
                        modifier = Modifier.size(width = 38.dp, height = 28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "IG",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                isTwitter -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1419),
                        modifier = Modifier.size(width = 38.dp, height = 28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "𝕏",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                isThreads -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF000000),
                        modifier = Modifier.size(width = 38.dp, height = 28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "@",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
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
