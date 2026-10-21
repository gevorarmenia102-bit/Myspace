package com.example.myspacepersonalhub.ui.feature.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myspacepersonalhub.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.navigation.Screen
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (Screen) -> Unit,
    onAddContact: () -> Unit,
    onAddNote: () -> Unit,
    onAddTask: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddVaultItem: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val secretTrigger = rememberSecretCloudTrigger(targetClicks = 15)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isSyncing = state.isLoading || state.syncStatus is CloudSyncStatus.Syncing
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val rotationAngle by if (isSyncing) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Sleek Top App Bar with Branding and Sync Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secret trigger on the branding badge (15 taps opens CloudConfigActivity)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { secretTrigger() }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "MySpace Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MYSPACE HUB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dedicated Realtime Refresh Button
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                viewModel.syncNow()
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = "Syncing Realtime Hub...",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Realtime Data",
                                tint = if (isSyncing) AccentBlue else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rotationAngle }
                            )
                        }
                    }

                    SyncStatusBadge(
                        status = state.syncStatus,
                        onSyncClick = {
                            viewModel.syncNow()
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = "Syncing Realtime Hub...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Greeting Banner Card (Clickable also triggers secret)
            HeroGreetingCard(
                greeting = state.greeting,
                dateDisplay = state.dateDisplay,
                tasksCount = state.todayTasks.size,
                contactCount = state.contactCount,
                isDark = isDark,
                onSecretTap = { secretTrigger() }
            )

            // 2. Quick Action Fast Dock
            QuickActionsBar(
                onAddContact = onAddContact,
                onAddNote = onAddNote,
                onAddTask = onAddTask,
                onAddTransaction = onAddTransaction,
                onAddVaultItem = onAddVaultItem
            )

            // 3. Bento Financial & Activity Metrics
            FinancialMetricsCard(
                currencyBalance = state.currencyBalance,
                preferredCurrency = state.preferredCurrency,
                onClick = { onNavigate(Screen.Money) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoMetricCard(
                    title = "Contacts",
                    value = "${state.contactCount}",
                    subtitle = "Directory →",
                    icon = Icons.Default.Contacts,
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Contacts) }
                )
                BentoMetricCard(
                    title = "Due Today",
                    value = "${state.todayTasks.size}",
                    subtitle = if (state.todayTasks.isEmpty()) "All clear ✦" else "Pending →",
                    icon = Icons.Default.CheckCircle,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Tasks) }
                )
            }

            // 4. Recent Contacts Carousel (with Add New shortcut)
            SectionHeader(
                title = "Recent Contacts",
                actionLabel = "View All",
                onAction = { onNavigate(Screen.Contacts) }
            )

            RecentContactsCarousel(
                contacts = state.recentContacts,
                onAddContact = onAddContact,
                onContactClick = { id -> onNavigate(Screen.ContactDetail(id)) }
            )

            // 5. Today's Agenda Section
            SectionHeader(
                title = "Today's Agenda",
                actionLabel = "View Tasks",
                onAction = { onNavigate(Screen.Tasks) }
            )

            TodayAgendaList(
                tasks = state.todayTasks,
                onTaskClick = { id -> onNavigate(Screen.TaskEdit(id)) },
                onPlanTasks = { onNavigate(Screen.Tasks) }
            )

            // 6. Pinned Notes Highlights
            if (state.pinnedNotes.isNotEmpty()) {
                SectionHeader(
                    title = "Pinned Notes",
                    actionLabel = "View Notes",
                    onAction = { onNavigate(Screen.Notes) }
                )

                PinnedNotesList(
                    notes = state.pinnedNotes,
                    onNoteClick = { id -> onNavigate(Screen.NoteEdit(id)) }
                )
            }

            // 7. Security & Encryption Badge
            SecurityHubFooter()

            // Safe bottom spacing for curved floating navigation bar
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun HeroGreetingCard(
    greeting: String,
    dateDisplay: String,
    tasksCount: Int,
    contactCount: Int,
    isDark: Boolean,
    onSecretTap: () -> Unit
) {
    val gradientBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F5F9)
                )
            )
        }
    }

    val isEvening = greeting.contains("Evening", ignoreCase = true) || greeting.contains("Night", ignoreCase = true)
    val greetingIcon = if (isEvening) Icons.Default.NightsStay else Icons.Default.WbSunny
    val iconTint = if (isEvening) AccentPurple else AccentAmber

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onSecretTap),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.25f else 0.45f)),
        shadowElevation = if (isDark) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Welcome back",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) AccentEmerald else Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✦", fontSize = 10.sp, color = if (isDark) AccentEmerald else Color(0xFF047857))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greeting,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = dateDisplay,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF0B111E) else Color(0xFFE2E8F0).copy(alpha = 0.5f))
                            .border(1.5.dp, iconTint.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "MySpace Emblem",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                // Live status chips strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(
                        icon = Icons.Default.TaskAlt,
                        text = "$tasksCount tasks due",
                        color = if (tasksCount > 0) AccentBlue else if (isDark) AccentEmerald else Color(0xFF047857)
                    )
                    Text("•", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 12.sp)
                    StatusChip(
                        icon = Icons.Default.People,
                        text = "$contactCount contacts",
                        color = if (isDark) AccentEmerald else Color(0xFF047857)
                    )
                    Text("•", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 12.sp)
                    StatusChip(
                        icon = Icons.Default.Lock,
                        text = "Encrypted",
                        color = if (isDark) AccentPurple else Color(0xFF6D28D9)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun QuickActionsBar(
    onAddContact: () -> Unit,
    onAddNote: () -> Unit,
    onAddTask: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddVaultItem: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModernQuickActionButton("Contact", Icons.Default.PersonAdd, AccentEmerald, onAddContact)
        ModernQuickActionButton("Note", Icons.Default.EditNote, AccentBlue, onAddNote)
        ModernQuickActionButton("Task", Icons.Default.AddTask, AccentPurple, onAddTask)
        ModernQuickActionButton("Expense", Icons.Default.AddCard, AccentAmber, onAddTransaction)
        ModernQuickActionButton("Vault", Icons.Default.EnhancedEncryption, AccentRed, onAddVaultItem)
    }
}

@Composable
private fun ModernQuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FinancialMetricsCard(
    currencyBalance: com.example.myspacepersonalhub.core.util.CurrencyBalance?,
    preferredCurrency: String,
    onClick: () -> Unit
) {
    val net = currencyBalance?.netMinorUnits ?: 0L
    val income = currencyBalance?.incomeMinorUnits ?: 0L
    val expense = currencyBalance?.expenseMinorUnits ?: 0L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Cash Flow Overview",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preferredCurrency,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Big Bold Amount Display
            Text(
                text = CurrencyUtils.formatAmount(net, preferredCurrency),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = if (net >= 0) AccentEmerald else AccentRed
            )

            // Income / Expense mini breakdown bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Income: +${CurrencyUtils.formatAmount(income, preferredCurrency)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentEmerald
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Expense: -${CurrencyUtils.formatAmount(expense, preferredCurrency)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentRed
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Icon(
                    Icons.Default.ArrowOutward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun RecentContactsCarousel(
    contacts: List<ContactEntity>,
    onAddContact: () -> Unit,
    onContactClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // First item: Add New contact action avatar
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddContact)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald.copy(alpha = 0.15f))
                        .border(1.5.dp, AccentEmerald.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Contact",
                        tint = AccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add New",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentEmerald
                )
            }
        }

        if (contacts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.height(76.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No contacts saved yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(contacts, key = { it.id }) { contact ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onContactClick(contact.id) }
                        .padding(4.dp)
                ) {
                    InitialAvatar(
                        name = contact.displayName,
                        imagePath = contact.profileImageLocalPath,
                        size = 52.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = contact.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayAgendaList(
    tasks: List<TaskEntity>,
    onTaskClick: (String) -> Unit,
    onPlanTasks: () -> Unit
) {
    if (tasks.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlanTasks)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All caught up for today!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "No pending tasks due today. Tap to plan ahead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tasks.take(4).forEach { task ->
                val priorityColor = when (task.priority) {
                    Priority.URGENT -> AccentRed
                    Priority.HIGH -> AccentAmber
                    Priority.MEDIUM -> AccentBlue
                    Priority.LOW -> AccentEmerald
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTaskClick(task.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored priority accent strip
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!task.category.isNullOrBlank()) {
                                Text(
                                    text = task.category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = task.priority.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedNotesList(
    notes: List<NoteEntity>,
    onNoteClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notes.take(3).forEach { note ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNoteClick(note.id) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (note.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityHubFooter() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Private & Encrypted Local Hub",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "On-device SQLCipher · AES-GCM 256-bit · Realtime sync ready",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
