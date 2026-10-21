package com.example.myspacepersonalhub.ui.feature.quickaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import com.example.myspacepersonalhub.core.model.QuickAccessType
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAccessScreen(
    viewModel: QuickAccessViewModel,
    onSyncClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Access", fontWeight = FontWeight.Bold) },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Sync Quick Access",
                            tint = AccentAmber,
                            containerColor = AccentAmber.copy(alpha = 0.15f),
                            borderColor = AccentAmber.copy(alpha = 0.3f),
                            onClick = {
                                onSyncClick()
                                com.example.myspacepersonalhub.core.util.SnackbarController.showMessage("⚡ Realtime Sync started...")
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentAmber,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shortcut")
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
            SearchBarField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search shortcuts..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.selectType(null) },
                    label = { Text("All", fontSize = 10.sp) }
                )
                QuickAccessType.values().forEach { t ->
                    FilterChip(
                        selected = state.selectedType == t,
                        onClick = { viewModel.selectType(t) },
                        label = { Text(t.name, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                LoadingStateView(message = "Loading shortcuts...")
            } else if (state.filteredItems.isEmpty()) {
                EmptyStateView(
                    title = "No Shortcuts Saved",
                    message = "Save frequently accessed website links, direct numbers, email addresses, coordinates, or text snippets.",
                    icon = Icons.Default.FlashOn,
                    actionLabel = "Add Shortcut",
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredItems, key = { it.id }) { item ->
                        val (icon, color) = when (item.itemType) {
                            QuickAccessType.URL -> Pair(Icons.Default.Link, AccentBlue)
                            QuickAccessType.PHONE -> Pair(Icons.Default.Phone, AccentEmerald)
                            QuickAccessType.EMAIL -> Pair(Icons.Default.Email, AccentPurple)
                            QuickAccessType.ADDRESS -> Pair(Icons.Default.Place, AccentRed)
                            QuickAccessType.TEXT -> Pair(Icons.Default.TextFields, AccentAmber)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    when (item.itemType) {
                                        QuickAccessType.URL -> IntentUtils.openWebUrl(context, item.value)
                                        QuickAccessType.PHONE -> IntentUtils.openDialer(context, item.value)
                                        QuickAccessType.EMAIL -> IntentUtils.sendEmail(context, item.value)
                                        QuickAccessType.ADDRESS -> IntentUtils.openMapAddress(context, item.value)
                                        QuickAccessType.TEXT -> IntentUtils.copyToClipboard(context, item.title, item.value)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                CircleDeleteButton(
                                    onClick = { viewModel.deleteItem(item.id) },
                                    size = 34.dp,
                                    iconSize = 17.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddQuickAccessDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, type, value, desc ->
                val entity = QuickAccessEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    itemType = type,
                    value = value,
                    description = desc.ifBlank { null }
                )
                viewModel.saveItem(entity, onSuccess = { showAddDialog = false }, onError = {})
            }
        )
    }
}

@Composable
private fun AddQuickAccessDialog(
    onDismiss: () -> Unit,
    onSave: (String, QuickAccessType, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(QuickAccessType.URL) }
    var value by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Shortcut", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Work Portal, Mom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickAccessType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = {
                        val lbl = when (type) {
                            QuickAccessType.URL -> "URL (https://...)"
                            QuickAccessType.PHONE -> "Phone Number"
                            QuickAccessType.EMAIL -> "Email Address"
                            QuickAccessType.ADDRESS -> "Address / Location (e.g. Amerikyan St 102, Yerevan, Armenia)"
                            QuickAccessType.TEXT -> "Text Snippet"
                        }
                        Text(lbl)
                    },
                    singleLine = type != QuickAccessType.TEXT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && value.isNotBlank()) {
                        onSave(title.trim(), type, value.trim(), desc.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
