package com.example.myspacepersonalhub.ui.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDetailScreen(
    itemId: String,
    viewModel: VaultViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val itemFlow = remember(itemId) { viewModel.getVaultItemById(itemId) }
    val item by itemFlow.collectAsState(initial = null)
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var decryptedContent by remember { mutableStateOf<String?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        item?.let {
            try {
                decryptedContent = viewModel.decryptNote(it)
            } catch (_: Exception) {
                decryptedContent = "Error decrypting: Invalid key or corrupted data."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.title ?: "Vault Item", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(itemId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val currentItem = item
        if (currentItem == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ENCRYPTED CONTENT", style = MaterialTheme.typography.labelSmall, color = AccentPurple, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { isRevealed = !isRevealed }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val displaySecret = if (isRevealed) {
                            decryptedContent ?: "Decrypting..."
                        } else {
                            "••••••••••••••••••••••••••••••••••••"
                        }

                        Text(
                            text = displaySecret,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                decryptedContent?.let {
                                    IntentUtils.copyToClipboard(context, "Secure Item", it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Secret to Clipboard")
                        }
                    }
                }

                // Security Metadata Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SECURITY METADATA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cipher: AES-256-GCM (Hardware Keystore backed)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("IV Length: 96 bits (12 bytes)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tag Length: 128 bits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Storage: App-Private Sandbox (Plaintext never logged)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Vault Item",
            message = "This encrypted item will be permanently removed from device storage.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                item?.let {
                    viewModel.deleteItem(it)
                    onBackClick()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
