package com.linkvault.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.linkvault.app.LinkVaultApp
import com.linkvault.app.classifier.AutoClassifier
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.data.model.ProcessingStatus
import com.linkvault.app.ui.components.MomoMascotView
import com.linkvault.app.ui.screens.AddLinkDialog
import com.linkvault.app.ui.screens.CategoriesScreen
import com.linkvault.app.ui.screens.DetailScreen
import com.linkvault.app.ui.screens.HomeScreen
import com.linkvault.app.ui.screens.NoteEditorDialog
import com.linkvault.app.ui.screens.NotesScreen
import com.linkvault.app.ui.screens.SettingsScreen
import com.linkvault.app.ui.theme.IndigoPrimary
import com.linkvault.app.ui.theme.LinkVaultTheme
import com.linkvault.app.ui.theme.Slate400
import com.linkvault.app.ui.theme.Slate500
import com.linkvault.app.ui.viewmodel.VaultViewModel
import com.linkvault.app.util.UrlExtractor
import com.linkvault.app.worker.EnrichmentScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class MainTab {
    HOME,
    CATEGORIES,
    NOTES,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        setContent {
            LinkVaultTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: ""

        if (sharedText.isBlank()) return

        val extracted = UrlExtractor.extract(sharedText)
        val itemId = UUID.randomUUID().toString()

        val itemToSave = if (extracted.url != null) {
            val initialCategory = AutoClassifier.classify(extracted.domain, null, null)
            LinkItem(
                id = itemId,
                url = extracted.url,
                normalizedUrl = extracted.normalizedUrl,
                title = extracted.domain ?: extracted.url,
                domain = extracted.domain,
                notes = extracted.accompanyingText,
                category = initialCategory,
                status = ProcessingStatus.PENDING
            )
        } else {
            LinkItem(
                id = itemId,
                url = "",
                title = sharedText.lines().firstOrNull()?.take(50) ?: "Shared Note",
                notes = sharedText,
                category = "Work",
                status = ProcessingStatus.COMPLETED
            )
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LinkVaultApp.instance.repository.saveItem(itemToSave)
            }

            if (extracted.url != null) {
                EnrichmentScheduler.scheduleEnrichment(applicationContext, itemId)
            }

            viewModel.refreshStats()

            Toast.makeText(
                applicationContext,
                if (extracted.url != null) "Saved to MomoStack" else "Note captured in MomoStack",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: VaultViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var selectedItemForDetail by remember { mutableStateOf<LinkItem?>(null) }
    var showQuickAddChoice by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedItemForDetail == null) {
                MomoBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddClick = { showQuickAddChoice = true }
                )
            }
        }
    ) { innerPadding ->
        if (selectedItemForDetail != null) {
            BackHandler {
                selectedItemForDetail = null
            }
            DetailScreen(
                item = selectedItemForDetail!!,
                viewModel = viewModel,
                onBack = { selectedItemForDetail = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onItemClick = { item -> selectedItemForDetail = item },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.CATEGORIES -> CategoriesScreen(
                    viewModel = viewModel,
                    onCategorySelected = { categoryName ->
                        viewModel.setCategory(categoryName)
                        selectedTab = MainTab.HOME
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.NOTES -> NotesScreen(
                    viewModel = viewModel,
                    onNoteSaved = { selectedTab = MainTab.NOTES },
                    modifier = Modifier.padding(innerPadding)
                )
                MainTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Quick Add Choice Dialog
        if (showQuickAddChoice) {
            QuickAddDialog(
                onDismiss = { showQuickAddChoice = false },
                onAddLink = { showAddLinkDialog = true },
                onAddNote = { showCreateNoteDialog = true }
            )
        }

        // Add Link Dialog
        if (showAddLinkDialog) {
            AddLinkDialog(
                onDismiss = { showAddLinkDialog = false },
                onSave = { url, title, notes ->
                    viewModel.saveDirectLink(url, title, notes)
                    Toast.makeText(context, "Saved link to MomoStack", Toast.LENGTH_SHORT).show()
                    showAddLinkDialog = false
                    selectedTab = MainTab.HOME
                }
            )
        }

        // Create Note Dialog
        if (showCreateNoteDialog) {
            NoteEditorDialog(
                initialTitle = "",
                initialBody = "",
                dialogTitle = "New Note",
                onDismiss = { showCreateNoteDialog = false },
                onSave = { title, body ->
                    viewModel.saveDirectNote(body, title)
                    Toast.makeText(context, "Saved note to MomoStack", Toast.LENGTH_SHORT).show()
                    showCreateNoteDialog = false
                    selectedTab = MainTab.NOTES
                }
            )
        }
    }
}

@Composable
fun MomoBottomNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 1. Home
            BottomNavItem(
                icon = if (selectedTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                isSelected = selectedTab == MainTab.HOME,
                onClick = { onTabSelected(MainTab.HOME) },
                modifier = Modifier.weight(1f)
            )

            // 2. Categories
            BottomNavItem(
                icon = if (selectedTab == MainTab.CATEGORIES) Icons.Filled.GridView else Icons.Outlined.GridView,
                label = "Categories",
                isSelected = selectedTab == MainTab.CATEGORIES,
                onClick = { onTabSelected(MainTab.CATEGORIES) },
                modifier = Modifier.weight(1f)
            )

            // 3. Center Elevated + Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onAddClick,
                    shape = CircleShape,
                    color = IndigoPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Quick Add",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // 4. Notes
            BottomNavItem(
                icon = if (selectedTab == MainTab.NOTES) Icons.Filled.Description else Icons.Outlined.Description,
                label = "Notes",
                isSelected = selectedTab == MainTab.NOTES,
                onClick = { onTabSelected(MainTab.NOTES) },
                modifier = Modifier.weight(1f)
            )

            // 5. Settings
            BottomNavItem(
                icon = if (selectedTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                label = "Settings",
                isSelected = selectedTab == MainTab.SETTINGS,
                onClick = { onTabSelected(MainTab.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IndigoPrimary.copy(alpha = 0.14f),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = IndigoPrimary,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Slate400,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) IndigoPrimary else Slate500
        )
    }
}

@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onAddLink: () -> Unit,
    onAddNote: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MomoMascotView(size = 32.dp, animate = true, holdingCard = false)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Quick Add", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onAddLink()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Link, contentDescription = null, tint = IndigoPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Save Link", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text("Paste URL, auto-categorize & enrich", color = Slate500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onAddNote()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF43F5E).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFFF43F5E))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("New Note", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text("Capture thoughts, snippets & memos", color = Slate500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
