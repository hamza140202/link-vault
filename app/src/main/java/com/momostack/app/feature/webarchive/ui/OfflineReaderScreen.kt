package com.momostack.app.feature.webarchive.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.momostack.app.data.model.LinkItem
import com.momostack.app.feature.webarchive.OfflineWebArchiveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ReaderTheme(val label: String, val bgColor: Color, val isDark: Boolean) {
    LIGHT("Light", Color(0xFFF8FAFC), false),
    SEPIA("Sepia", Color(0xFFF7EFE1), false),
    DARK("Dark", Color(0xFF0F172A), true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineReaderScreen(
    item: LinkItem,
    onBack: () -> Unit,
    onDeleteArchive: () -> Unit
) {
    val context = LocalContext.current
    var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    DisposableEffect(item.offlinePdfPath) {
        val path = item.offlinePdfPath
        if (path == null) {
            errorMessage = "No offline document found for this link."
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val file = File(path)
        if (!file.exists()) {
            errorMessage = "Offline PDF file is missing."
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            pfd = descriptor
            pdfRenderer = renderer
            pageCount = renderer.pageCount
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load document: ${e.message}"
            isLoading = false
        }

        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    val colorMatrix = remember(readerTheme) {
        when (readerTheme) {
            ReaderTheme.LIGHT -> null
            ReaderTheme.SEPIA -> {
                ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.05f, 0.00f, 0f, 0f,
                        0.05f, 0.90f, 0.05f, 0f, 0f,
                        0.00f, 0.05f, 0.80f, 0f, 0f,
                        0f,    0f,    0f,    1f, 0f
                    )
                )
            }
            ReaderTheme.DARK -> {
                // Invert colors matrix
                ColorMatrix(
                    floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f, 0f
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = item.title.ifBlank { "Offline Document" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.domain ?: item.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Theme Switcher
                    IconButton(
                        onClick = {
                            readerTheme = when (readerTheme) {
                                ReaderTheme.LIGHT -> ReaderTheme.SEPIA
                                ReaderTheme.SEPIA -> ReaderTheme.DARK
                                ReaderTheme.DARK -> ReaderTheme.LIGHT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (readerTheme) {
                                ReaderTheme.LIGHT -> Icons.Outlined.LightMode
                                ReaderTheme.SEPIA -> Icons.Outlined.Brightness4
                                ReaderTheme.DARK -> Icons.Outlined.DarkMode
                            },
                            contentDescription = "Theme: ${readerTheme.label}"
                        )
                    }

                    // Share PDF
                    IconButton(
                        onClick = {
                            val shareIntent = OfflineWebArchiveManager.getSharePdfIntent(context, item)
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF"
                        )
                    }

                    // Delete PDF Archive
                    IconButton(onClick = onDeleteArchive) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Offline File",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = readerTheme.bgColor
                )
            )
        },
        containerColor = readerTheme.bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading offline document...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
                pdfRenderer != null -> {
                    val renderer = pdfRenderer!!

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pageCount) { pageIndex ->
                                PdfPageCard(
                                    renderer = renderer,
                                    pageIndex = pageIndex,
                                    colorMatrix = colorMatrix
                                )
                            }
                        }
                    }

                    // Floating Page Counter Pill
                    val firstVisible = remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
                    AnimatedVisibility(
                        visible = pageCount > 1,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = "Page ${firstVisible.value} of $pageCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    renderer: PdfRenderer,
    pageIndex: Int,
    colorMatrix: ColorMatrix?
) {
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var aspectRatio by remember(pageIndex) { mutableFloatStateOf(1.414f) } // Default A4 ratio

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val page = synchronized(renderer) { renderer.openPage(pageIndex) }
                val w = page.width
                val h = page.height
                aspectRatio = if (w > 0) h.toFloat() / w.toFloat() else 1.414f

                // Render at 2x resolution for crisp high-DPI clarity
                val renderWidth = (w * 2).coerceAtMost(2400)
                val renderHeight = (h * 2).coerceAtMost(3400)
                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)

                synchronized(renderer) {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                }
                pageBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / aspectRatio),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
    }
}
