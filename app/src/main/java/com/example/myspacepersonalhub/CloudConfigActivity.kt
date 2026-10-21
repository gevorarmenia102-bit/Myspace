package com.example.myspacepersonalhub

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myspacepersonalhub.core.util.SnackbarController
import com.example.myspacepersonalhub.ui.components.ModernSnackbar
import com.example.myspacepersonalhub.ui.components.CircleIconButton
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.feature.settings.SettingsViewModel

/**
 * Secret Safety Activity for Firebase & GitHub Cloud Configuration.
 * Hidden from the public settings interface.
 * Only opened when clicking 15 times rapidly on the "MySpace Personal Hub" layout.
 */
class CloudConfigActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MySpaceApplication

        setContent {
            val userPrefs by app.preferencesRepository.userPreferencesFlow.collectAsState(
                initial = com.example.myspacepersonalhub.core.datastore.UserPreferences()
            )

            // Dynamic Window Flags for Privacy / App-switcher obscuring when biometric is enabled
            if (userPrefs.biometricLockEnabled) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            val viewModel = remember {
                SettingsViewModel(
                    preferencesRepository = app.preferencesRepository,
                    backupRepository = app.backupRepository,
                    syncManager = app.syncManager
                )
            }

            MySpaceTheme(themeMode = userPrefs.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CloudConfigScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudConfigScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedCloudTab by remember { mutableIntStateOf(0) }
    var firebaseDbUrl by remember { mutableStateOf("") }
    var githubToken by remember { mutableStateOf("") }
    var githubOwner by remember { mutableStateOf("") }
    var githubRepo by remember { mutableStateOf("") }
    var githubBranch by remember { mutableStateOf("main") }
    var showTokenText by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.preferences) {
        firebaseDbUrl = state.preferences.firebaseDatabaseUrl ?: ""
        githubToken = state.preferences.githubToken ?: ""
        githubOwner = state.preferences.githubOwner ?: ""
        githubRepo = state.preferences.githubRepo ?: ""
        githubBranch = state.preferences.githubBranch.ifBlank { "main" }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = event.duration
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            ) { snackbarData ->
                ModernSnackbar(snackbarData = snackbarData)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Secret Cloud Console",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            "Firebase RTDB + GitHub Cloud Engine",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentEmerald.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Owner Only",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                        }
                    }
                },
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
            // Safety Notice Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp).padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Hidden Safety Vault",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "This configuration console is completely hidden from public settings. Only someone who taps 15 times rapidly on 'MySpace Personal Hub' can enter here. Your GitHub PAT and Firebase credentials stay 100% private.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // ==========================================
            // 1. REALTIME CLOUD SYNC HERO CARD
            // ==========================================
            val isEngineActive = state.preferences.realtimeSyncEnabled
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = if (isEngineActive) listOf(
                                AccentEmerald.copy(alpha = 0.6f),
                                AccentBlue.copy(alpha = 0.6f)
                            ) else listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isEngineActive) AccentEmerald.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (isEngineActive) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Hybrid Realtime Engine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Firebase RTDB + GitHub Cloud",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status Badge Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (state.syncStatus) {
                                is CloudSyncStatus.Synced -> AccentEmerald.copy(alpha = 0.15f)
                                is CloudSyncStatus.Syncing -> AccentAmber.copy(alpha = 0.15f)
                                is CloudSyncStatus.Error -> AccentRed.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (state.syncStatus) {
                                    is CloudSyncStatus.Synced -> "Connected"
                                    is CloudSyncStatus.Syncing -> "Syncing..."
                                    is CloudSyncStatus.Error -> "Sync Error"
                                    is CloudSyncStatus.NotSignedIn -> "Local Mode"
                                    else -> "Unconfigured"
                                },
                                color = when (state.syncStatus) {
                                    is CloudSyncStatus.Synced -> AccentEmerald
                                    is CloudSyncStatus.Syncing -> AccentAmber
                                    is CloudSyncStatus.Error -> AccentRed
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-sync Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Realtime WebSocket Sync",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Instant background sync on create/edit/delete",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.preferences.realtimeSyncEnabled,
                            onCheckedChange = { viewModel.setRealtimeSyncEnabled(it) },
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Sync Trigger Button
                    Button(
                        onClick = {
                            viewModel.syncNow()
                            SnackbarController.showMessage("Full sync triggered")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Sync Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ==========================================
            // 2. HYBRID CLOUD STORAGE CONFIG (Tabs)
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "CREDENTIALS & CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Engine Selector Tabs
                    SecondaryTabRow(
                        selectedTabIndex = selectedCloudTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedCloudTab == 0,
                            onClick = { selectedCloudTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Firebase (Text)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedCloudTab == 1,
                            onClick = { selectedCloudTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GitHub (Files)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedCloudTab == 0) {
                        // ------------------------------------
                        // TAB 0: FIREBASE REALTIME DB
                        // ------------------------------------
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "⚡ Sub-second live WebSocket sync (<0.2s) for Contacts, Notes, Tasks & Money Transactions.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = firebaseDbUrl,
                            onValueChange = {
                                firebaseDbUrl = it
                                viewModel.setFirebaseDatabaseUrl(it)
                            },
                            label = { Text("Database URL (override)", fontSize = 11.sp) },
                            placeholder = { Text("https://your-rtdb.firebaseio.com", fontSize = 10.sp) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        firebaseDbUrl = clip
                                        viewModel.setFirebaseDatabaseUrl(clip)
                                        SnackbarController.showMessage("Pasted URL from clipboard")
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tip: If google-services.json is present, it will connect automatically without entering a URL.",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        // ------------------------------------
                        // TAB 1: GITHUB BIG FILE ENGINE
                        // ------------------------------------
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "📦 100GB+ free private storage for Contact Avatars, Media attachments, and Encrypted Hub Backups.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // GitHub Token Field
                        OutlinedTextField(
                            value = githubToken,
                            onValueChange = {
                                githubToken = it
                                viewModel.setGitHubConfig(it, githubOwner, githubRepo, githubBranch)
                            },
                            label = { Text("Personal Access Token (PAT)", fontSize = 11.sp) },
                            placeholder = { Text("ghp_xxxxxxxxxxxx", fontSize = 10.sp) },
                            visualTransformation = if (showTokenText) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showTokenText = !showTokenText }) {
                                    Icon(
                                        if (showTokenText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Owner and Repo inputs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = githubOwner,
                                onValueChange = {
                                    githubOwner = it
                                    viewModel.setGitHubConfig(githubToken, it, githubRepo, githubBranch)
                                },
                                label = { Text("Username", fontSize = 11.sp) },
                                placeholder = { Text("e.g. xagor", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                            )

                            OutlinedTextField(
                                value = githubRepo,
                                onValueChange = {
                                    githubRepo = it
                                    viewModel.setGitHubConfig(githubToken, githubOwner, it, githubBranch)
                                },
                                label = { Text("Repo Name", fontSize = 11.sp) },
                                placeholder = { Text("myspace-storage", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = githubBranch,
                            onValueChange = {
                                githubBranch = it
                                viewModel.setGitHubConfig(githubToken, githubOwner, githubRepo, it)
                            },
                            label = { Text("Branch (default main)", fontSize = 11.sp) },
                            placeholder = { Text("main", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test GitHub Connection Button
                        Button(
                            onClick = { viewModel.testGitHubConnection() },
                            enabled = !state.isTestingGitHub && githubToken.isNotBlank() && githubOwner.isNotBlank() && githubRepo.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            if (state.isTestingGitHub) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting to GitHub...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test GitHub Connection", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Connection Result Banner
                        state.gitHubConnectionMessage?.let { msg ->
                            val isSuccess = state.isGitHubConnected == true
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSuccess) AccentEmerald.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) AccentEmerald else AccentRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        msg,
                                        color = if (isSuccess) AccentEmerald else AccentRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. ENCRYPTED BACKUP & RESTORE TO GITHUB
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "CLOUD BACKUP REPOSITORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Directly commits zero-knowledge encrypted AES-256 JSON backups to your private GitHub repo.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showCloudBackupDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        enabled = !state.isUploadingBackupToCloud && githubToken.isNotBlank() && githubOwner.isNotBlank() && githubRepo.isNotBlank()
                    ) {
                        if (state.isUploadingBackupToCloud) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Encrypting & Uploading...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Push Encrypted Backup to GitHub", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    state.cloudBackupMessage?.let { msg ->
                        Text(msg, color = AccentEmerald, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // ==========================================
            // 4. TRUECALLER CALLER ID CONFIGURATION
            // ==========================================
            var truecallerTokenInput by remember {
                mutableStateOf(com.example.myspacepersonalhub.core.util.CallerIdService.getTruecallerToken(context) ?: "")
            }
            var showTruecallerTokenSaved by remember { mutableStateOf(false) }

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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhoneCallback,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Truecaller Caller ID API",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Global live name auto-fill for unknown numbers",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (truecallerTokenInput.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AccentEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Configured",
                                    color = AccentEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = truecallerTokenInput,
                        onValueChange = {
                            truecallerTokenInput = it
                            showTruecallerTokenSaved = false
                        },
                        label = { Text("Installation Token / Bearer", fontSize = 11.sp) },
                        placeholder = { Text("e.g. a1k07--...", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        trailingIcon = {
                            if (truecallerTokenInput.isNotBlank()) {
                                IconButton(onClick = {
                                    truecallerTokenInput = ""
                                    com.example.myspacepersonalhub.core.util.CallerIdService.setTruecallerToken(context, "")
                                    SnackbarController.showMessage("Truecaller token cleared")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.myspacepersonalhub.core.util.CallerIdService.setTruecallerToken(context, truecallerTokenInput)
                                showTruecallerTokenSaved = true
                                SnackbarController.showMessage("✅ Truecaller Token Saved!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Token", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (showTruecallerTokenSaved) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "✅ Token saved. Live Truecaller search will now run automatically in Contact Edit.",
                            fontSize = 10.sp,
                            color = AccentEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showCloudBackupDialog) {
        CloudBackupPassphraseDialog(
            onDismiss = { showCloudBackupDialog = false },
            onConfirm = { passphrase ->
                showCloudBackupDialog = false
                viewModel.uploadBackupToGitHub(
                    passphrase = passphrase,
                    onComplete = { SnackbarController.showMessage("✅ Cloud Backup Pushed to GitHub!") },
                    onError = { SnackbarController.showMessage("❌ Error: $it") }
                )
            }
        )
    }
}

@Composable
private fun CloudBackupPassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Push Encrypted Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Encrypts all your records with AES-256-GCM and pushes directly to your private GitHub repo.",
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Push to GitHub", fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 11.sp) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
