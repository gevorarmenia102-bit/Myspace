package com.example.myspacepersonalhub.ui.feature.tracker

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTrackerScreen(
    viewModel: TrackerViewModel,
    onSyncClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar & Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Sync Tracker",
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
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
            if (state.selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddHabitDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit")
                }
            } else if (state.selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddGoalDialog = true },
                    containerColor = AccentBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Habits (${state.habits.size})", fontSize = 10.sp) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Goals (${state.goals.size})", fontSize = 10.sp) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Calendar (${state.upcomingEvents.size})", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (state.selectedTab) {
                0 -> HabitsTabContent(
                    habits = state.habits,
                    onToggle = { viewModel.toggleHabitToday(it) },
                    onDelete = { viewModel.deleteHabit(it) },
                    onAddHabit = { showAddHabitDialog = true }
                )
                1 -> GoalsTabContent(
                    goals = state.goals,
                    onIncrement = { goal -> viewModel.updateGoalProgress(goal, goal.currentProgress + 1.0) },
                    onDelete = { viewModel.deleteGoal(it) },
                    onAddGoal = { showAddGoalDialog = true }
                )
                2 -> CalendarTabContent(events = state.upcomingEvents)
            }
        }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, desc ->
                val habit = HabitEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = desc.ifBlank { null },
                    startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                )
                viewModel.saveHabit(habit, onSuccess = { showAddHabitDialog = false }, onError = {})
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, unit ->
                val goal = GoalEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    targetValue = target,
                    currentProgress = 0.0,
                    unit = unit
                )
                viewModel.saveGoal(goal, onSuccess = { showAddGoalDialog = false }, onError = {})
            }
        )
    }
}

@Composable
private fun HabitsTabContent(
    habits: List<HabitWithStreak>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddHabit: () -> Unit
) {
    if (habits.isEmpty()) {
        EmptyStateView(
            title = "No Habits Configured",
            message = "Build positive routines like daily reading, drinking water, or exercising.",
            icon = Icons.Default.DirectionsRun,
            actionLabel = "Create Habit",
            onAction = onAddHabit
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(habits, key = { it.habit.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggle(item.habit.id) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (item.isCompletedToday) AccentEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Complete today",
                                tint = if (item.isCompletedToday) AccentEmerald else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (!item.habit.description.isNullOrBlank()) {
                                Text(
                                    text = item.habit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        CircleDeleteButton(
                            onClick = { onDelete(item.habit.id) },
                            size = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsTabContent(
    goals: List<GoalEntity>,
    onIncrement: (GoalEntity) -> Unit,
    onDelete: (String) -> Unit,
    onAddGoal: () -> Unit
) {
    if (goals.isEmpty()) {
        EmptyStateView(
            title = "No Goals Yet",
            message = "Track measurable milestones like savings, reading 12 books, or running 100km.",
            icon = Icons.Default.EmojiEvents,
            actionLabel = "Set a Goal",
            onAction = onAddGoal
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                val progressFraction = (goal.currentProgress / goal.targetValue).coerceIn(0.0, 1.0).toFloat()
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onIncrement(goal) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("+1", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                                }
                                CircleDeleteButton(
                                    onClick = { onDelete(goal.id) },
                                    size = 32.dp,
                                    iconSize = 16.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${goal.currentProgress.toInt()} / ${goal.targetValue.toInt()} ${goal.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTabContent(events: List<CalendarEventItem>) {
    if (events.isEmpty()) {
        EmptyStateView(
            title = "No Upcoming Events",
            message = "Scheduled tasks and birthdays from contacts will automatically appear here.",
            icon = Icons.Default.EventAvailable
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(events) { event ->
                val icon = if (event.type == "BIRTHDAY") Icons.Default.Cake else Icons.Default.Assignment
                val iconColor = if (event.type == "BIRTHDAY") AccentAmber else AccentBlue

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
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
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                event.dateStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name (e.g. Read 20 pages)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), desc.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("km") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Run 100km)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target Value") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = target.toDoubleOrNull()
                    if (title.isNotBlank() && t != null && t > 0) {
                        onSave(title.trim(), t, unit.trim().ifBlank { "%" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
