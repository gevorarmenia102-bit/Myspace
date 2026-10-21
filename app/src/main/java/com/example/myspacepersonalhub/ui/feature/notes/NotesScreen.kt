package com.example.myspacepersonalhub.ui.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String) -> Unit,
    onAddNoteClick: (isJournal: Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes & Journal", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.selectedTab == 3 && state.trashNotes.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashConfirm = true }) {
                            Text("Empty Trash", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (state.selectedTab != 3) {
                FloatingActionButton(
                    onClick = { onAddNoteClick(state.selectedTab == 1) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        if (state.selectedTab == 1) Icons.Default.AutoStories else Icons.Default.EditNote,
                        contentDescription = "Add"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Search
            SearchBarField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search notes or journal..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs: All, Journal, Pinned, Trash
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("All (${state.notes.size})", fontSize = 11.sp) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Journal", fontSize = 11.sp) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Pinned", fontSize = 11.sp) }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.onTabSelected(3) },
                    text = { Text("Trash (${state.trashNotes.size})", fontSize = 11.sp) }
                )
            }

            // Category filter chips
            if (state.availableCategories.isNotEmpty() && state.selectedTab != 3) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("All Categories", fontSize = 12.sp) }
                    )
                    state.availableCategories.forEach { category ->
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List or Empty
            if (state.isLoading) {
                LoadingStateView(message = "Loading notes...")
            } else if (state.filteredNotes.isEmpty()) {
                val emptyMessage = when (state.selectedTab) {
                    1 -> "No journal entries yet. Capture your reflections, thoughts, and moods."
                    2 -> "No pinned or favorite notes. Tap the pin or star on any note to keep it here."
                    3 -> "Trash is empty."
                    else -> "Create your first note or journal entry to start organizing your thoughts."
                }
                EmptyStateView(
                    title = if (state.searchQuery.isNotBlank()) "No Matching Notes" else "No Notes",
                    message = emptyMessage,
                    icon = if (state.selectedTab == 1) Icons.Default.AutoStories else Icons.Default.Description,
                    actionLabel = if (state.selectedTab != 3 && state.searchQuery.isBlank()) "Create Note" else null,
                    onAction = { onAddNoteClick(state.selectedTab == 1) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            isTrash = state.selectedTab == 3,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                            onRestore = { viewModel.restoreFromTrash(note.id) },
                            onDeletePermanently = { viewModel.deletePermanently(note) }
                        )
                    }
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        ConfirmationDialog(
            title = "Empty Trash",
            message = "All notes in the trash will be permanently deleted.",
            confirmLabel = "Empty Trash",
            isDestructive = true,
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashConfirm = false
            },
            onDismiss = { showEmptyTrashConfirm = false }
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    isTrash: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(note.updatedAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (note.isJournal) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentPurple.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Journal ${note.journalMood?.let { "• $it" } ?: ""}",
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                note.category,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isTrash) {
                    Row {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) AccentEmerald else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) AccentAmber else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Restore,
                            contentDescription = "Restore",
                            onClick = onRestore,
                            size = 30.dp,
                            iconSize = 15.dp,
                            tint = AccentEmerald,
                            containerColor = AccentEmerald.copy(alpha = 0.12f),
                            borderColor = AccentEmerald.copy(alpha = 0.25f)
                        )
                        CircleDeleteButton(
                            onClick = onDeletePermanently,
                            size = 30.dp,
                            iconSize = 15.dp,
                            contentDescription = "Delete Forever"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (note.tags.isNotEmpty()) {
                    Text(
                        text = note.tags.take(3).joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
