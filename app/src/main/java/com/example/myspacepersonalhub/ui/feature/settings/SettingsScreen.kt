package com.example.myspacepersonalhub.ui.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myspacepersonalhub.R
import com.example.myspacepersonalhub.core.util.SnackbarController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.model.ThemeMode
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.rememberSecretCloudTrigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val secretTrigger = rememberSecretCloudTrigger(targetClicks = 15)
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. DATA BACKUP & RESTORE (LOCAL)
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "DATA BACKUP & RESTORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "AES-256-GCM + PBKDF2 authenticated encryption for your local offline database backups.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Export and Restore Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showBackupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Data", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ==========================================
            // 2. CLOUD HUB & REALTIME SYNC (ONLINE)
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "REALTIME CLOUD SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        com.example.myspacepersonalhub.ui.components.SyncStatusBadge(
                            status = state.syncStatus,
                            onSyncClick = {
                                viewModel.syncNow()
                                SnackbarController.showMessage("⚡ Syncing Realtime Cloud Hub...")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Live bi-directional synchronization via Firebase WebSockets across all your personal devices. Changes in Contacts, Notes, Tasks, Money, Vault, Habits, Goals, and Quick Access propagate automatically.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Realtime Sync Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Realtime Sync", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                if (state.preferences.realtimeSyncEnabled) "Active • Instant updates via WebSocket" else "Disabled • Manual sync only",
                                fontSize = 10.sp,
                                color = if (state.preferences.realtimeSyncEnabled) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.preferences.realtimeSyncEnabled,
                            onCheckedChange = {
                                viewModel.setRealtimeSyncEnabled(it)
                                SnackbarController.showMessage(
                                    if (it) "🟢 Realtime Sync enabled" else "⚪ Realtime Sync disabled"
                                )
                            },
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloud action buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.syncNow()
                                SnackbarController.showMessage("⚡ Synchronizing with Personal Cloud...")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { showCloudBackupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cloud Backup", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ==========================================
            // 4. APPEARANCE & THEME SECTION
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "APPEARANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(ThemeMode.DARK, "Dark"),
                            Pair(ThemeMode.LIGHT, "Light"),
                            Pair(ThemeMode.SYSTEM, "System")
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = state.preferences.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 5. CURRENCY PREFERENCE SECTION
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "CURRENCY PREFERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyUtils.SUPPORTED_CURRENCIES.forEach { curr ->
                            FilterChip(
                                selected = state.preferences.defaultCurrency == curr,
                                onClick = { viewModel.setDefaultCurrency(curr) },
                                label = { Text(curr, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. SECURITY & PRIVACY SECTION
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "SECURITY & PRIVACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric App Lock", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                "Require fingerprint to unlock Private Vault",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.preferences.biometricLockEnabled,
                            onCheckedChange = { viewModel.setBiometricLockEnabled(it) },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }

            // ==========================================
            // 6. ABOUT SECTION (Secret 15-tap Trigger)
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { secretTrigger() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "MySpace Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MySpace Personal Hub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "v1.0.0",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Version 1.0.0 (Cosmic Edition)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Optimized for security & local privacy with AES-256 cloud sync.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showBackupDialog = false },
            onConfirm = { passphrase ->
                viewModel.createEncryptedBackup(
                    passphrase = passphrase,
                    onComplete = { jsonPayload ->
                        showBackupDialog = false
                        IntentUtils.shareText(context, jsonPayload, "MySpace Encrypted Backup")
                    },
                    onError = {
                        SnackbarController.showMessage("❌ $it")
                    }
                )
            }
        )
    }

    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreDialog = false },
            onConfirm = { jsonString, passphrase ->
                viewModel.restoreEncryptedBackup(
                    backupJson = jsonString,
                    passphrase = passphrase,
                    onSuccess = { count ->
                        showRestoreDialog = false
                        SnackbarController.showMessage("✅ Successfully restored $count items!")
                    },
                    onError = {
                        SnackbarController.showMessage("❌ $it")
                    }
                )
            }
        )
    }

    if (showCloudBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showCloudBackupDialog = false },
            onConfirm = { passphrase ->
                SnackbarController.showMessage("⏳ Encrypting & uploading backup to GitHub...")
                viewModel.uploadBackupToGitHub(
                    passphrase = passphrase,
                    onComplete = { msg ->
                        showCloudBackupDialog = false
                        SnackbarController.showMessage("☁️ $msg")
                    },
                    onError = { err ->
                        SnackbarController.showMessage("❌ $err")
                    }
                )
            }
        )
    }
}

@Composable
private fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Encrypted Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose a strong passphrase. You will need this exact passphrase to restore your records.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase (min 6 chars)", fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )
                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = { confirmPassphrase = it },
                    label = { Text("Confirm Passphrase", fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )
                error?.let { Text(it, color = AccentRed, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.length < 6) {
                        error = "Passphrase must be at least 6 characters"
                    } else if (passphrase != confirmPassphrase) {
                        error = "Passphrases do not match"
                    } else {
                        onConfirm(passphrase)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Export", fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 11.sp) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var backupJson by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore From Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste the encrypted backup JSON content and enter the passphrase.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = backupJson,
                    onValueChange = { backupJson = it },
                    label = { Text("Encrypted Backup JSON", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 10.sp)
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase", fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )
                error?.let { Text(it, color = AccentRed, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (backupJson.isBlank() || passphrase.isBlank()) {
                        error = "Please fill in all fields"
                    } else {
                        onConfirm(backupJson.trim(), passphrase)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Restore", fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 11.sp) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
