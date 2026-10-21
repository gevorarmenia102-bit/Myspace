package com.example.myspacepersonalhub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.navigation.Screen
import com.example.myspacepersonalhub.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySpaceScaffold(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    syncStatus: CloudSyncStatus,
    onSyncClick: () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var showMoreSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 8.dp,
                    shadowElevation = 14.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navItems = listOf(
                            Triple(Screen.Dashboard, "Home", Icons.Default.Home),
                            Triple(Screen.Contacts, "Contacts", Icons.Default.Contacts),
                            Triple(Screen.Notes, "Notes", Icons.Default.EditNote),
                            Triple(Screen.Tasks, "Tasks", Icons.Default.CheckCircleOutline),
                            Triple(Screen.Money, "Money", Icons.Default.AccountBalanceWallet)
                        )

                        navItems.forEach { (screen, label, icon) ->
                            val isSelected = currentScreen::class == screen::class
                            MySpaceNavItem(
                                label = label,
                                icon = icon,
                                isSelected = isSelected,
                                onClick = { onNavigate(screen) }
                            )
                        }

                        // More item
                        val isMoreSelected = currentScreen in listOf(Screen.Vault, Screen.Tracker, Screen.QuickAccess, Screen.Settings)
                        MySpaceNavItem(
                            label = "More",
                            icon = Icons.Default.Widgets,
                            isSelected = isMoreSelected,
                            onClick = { showMoreSheet = true }
                        )
                    }
                }
            }
        },
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            content(paddingValues)

            if (showMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "More Features",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            SyncStatusBadge(status = syncStatus, onSyncClick = onSyncClick)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val moreItems = listOf(
                            MoreModuleItem(
                                title = "Private Vault",
                                subtitle = "Encrypted documents & secure notes",
                                icon = Icons.Default.Lock,
                                color = AccentPurple,
                                target = Screen.Vault
                            ),
                            MoreModuleItem(
                                title = "Calendar & Habits",
                                subtitle = "Daily streaks & progress goals",
                                icon = Icons.Default.CalendarToday,
                                color = AccentBlue,
                                target = Screen.Tracker
                            ),
                            MoreModuleItem(
                                title = "Quick Access",
                                subtitle = "URLs, phones, maps & text snippets",
                                icon = Icons.Default.FlashOn,
                                color = AccentAmber,
                                target = Screen.QuickAccess
                            ),
                            MoreModuleItem(
                                title = "App Settings",
                                subtitle = "Theme, currency, backup & security",
                                icon = Icons.Default.Settings,
                                color = AccentEmerald,
                                target = Screen.Settings
                            )
                        )

                        moreItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showMoreSheet = false
                                        onNavigate(item.target)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(item.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private data class MoreModuleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val target: Screen
)

@Composable
private fun RowScope.MySpaceNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val animatedIndicatorColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "nav_indicator"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else unselectedColor,
        animationSpec = tween(200),
        label = "nav_icon_color"
    )

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(animatedIndicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = animatedIconColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedIconColor
        )
    }
}

