package com.example.myspacepersonalhub.ui.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.fragment.app.FragmentActivity
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity
import com.example.myspacepersonalhub.core.model.VaultItemType
import com.example.myspacepersonalhub.core.security.BiometricAuthManager
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onSyncClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (state.biometricEnabled && !state.isUnlocked) {
        BiometricLockOverlay(
            onUnlockClick = {
                if (activity != null && BiometricAuthManager.isBiometricAvailable(activity)) {
                    BiometricAuthManager.authenticate(
                        activity = activity,
                        title = "Unlock Private Vault",
                        subtitle = "Confirm biometric credentials",
                        onSuccess = { viewModel.unlockVault() },
                        onError = { /* Keep locked */ }
                    )
                } else {
                    // Fallback unlock if device lacks biometric sensor
                    viewModel.unlockVault()
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Private Vault", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "AES-GCM",
                                color = AccentPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        CircleIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Sync Vault",
                            tint = AccentPurple,
                            containerColor = AccentPurple.copy(alpha = 0.15f),
                            borderColor = AccentPurple.copy(alpha = 0.3f),
                            onClick = {
                                onSyncClick()
                                com.example.myspacepersonalhub.core.util.SnackbarController.showMessage("⚡ Realtime Sync started...")
                            }
                        )
                        if (state.biometricEnabled) {
                            CircleIconButton(
                                icon = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = AccentPurple,
                                containerColor = AccentPurple.copy(alpha = 0.15f),
                                borderColor = AccentPurple.copy(alpha = 0.3f),
                                onClick = viewModel::lockVault
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItemClick,
                containerColor = AccentPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
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
                placeholder = "Search encrypted vault..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            val filteredItems = if (state.searchQuery.isBlank()) {
                state.items
            } else {
                state.items.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            }

            if (state.isLoading) {
                LoadingStateView(message = "Loading vault...")
            } else if (filteredItems.isEmpty()) {
                EmptyStateView(
                    title = "Vault is Empty",
                    message = "Store private confidential documents, passport copies, passwords, and secure notes encrypted with Android Keystore.",
                    icon = Icons.Default.EnhancedEncryption,
                    actionLabel = "Create Secure Note",
                    onAction = onAddItemClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultItemCard(
    item: VaultItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val (icon, typeName) = when (item.itemType) {
        VaultItemType.SECURE_NOTE -> Pair(Icons.Default.Notes, "Secure Note")
        VaultItemType.DOCUMENT -> Pair(Icons.Default.Description, "Document")
        VaultItemType.IMAGE -> Pair(Icons.Default.Image, "Private Image")
        VaultItemType.FILE -> Pair(Icons.Default.Folder, "Encrypted File")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$typeName • ${dateFormat.format(Date(item.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
