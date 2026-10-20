package com.example.myspacepersonalhub.ui.feature.tasks

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.model.TaskStatus
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onTaskClick: (String) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Reminders", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
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
                placeholder = "Search tasks..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // View Filter Tabs
            TabRow(
                selectedTabIndex = state.viewFilter.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.viewFilter == TaskViewFilter.ALL,
                    onClick = { viewModel.setViewFilter(TaskViewFilter.ALL) },
                    text = { Text("All", fontSize = 10.sp) }
                )
                Tab(
                    selected = state.viewFilter == TaskViewFilter.TODAY,
                    onClick = { viewModel.setViewFilter(TaskViewFilter.TODAY) },
                    text = { Text("Today", fontSize = 10.sp) }
                )
                Tab(
                    selected = state.viewFilter == TaskViewFilter.UPCOMING,
                    onClick = { viewModel.setViewFilter(TaskViewFilter.UPCOMING) },
                    text = { Text("Upcoming", fontSize = 10.sp) }
                )
                Tab(
                    selected = state.viewFilter == TaskViewFilter.COMPLETED,
                    onClick = { viewModel.setViewFilter(TaskViewFilter.COMPLETED) },
                    text = { Text("Done", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Priority filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.selectedPriority == null,
                    onClick = { viewModel.setPriorityFilter(null) },
                    label = { Text("All Priorities", fontSize = 12.sp) }
                )
                Priority.values().forEach { p ->
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.selectedPriority == p,
                        onClick = { viewModel.setPriorityFilter(p) },
                        label = { Text(p.name, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task list or empty
            if (state.isLoading) {
                LoadingStateView(message = "Loading tasks...")
            } else if (state.filteredTasks.isEmpty()) {
                val msg = when (state.viewFilter) {
                    TaskViewFilter.TODAY -> "No tasks due today. Enjoy your day or plan ahead!"
                    TaskViewFilter.UPCOMING -> "No upcoming scheduled tasks."
                    TaskViewFilter.COMPLETED -> "No completed tasks in history."
                    else -> "No tasks found. Create a new task to stay organized."
                }
                EmptyStateView(
                    title = "All Clear",
                    message = msg,
                    icon = Icons.Default.CheckCircleOutline,
                    actionLabel = if (state.viewFilter != TaskViewFilter.COMPLETED) "Add Task" else null,
                    onAction = onAddTaskClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onToggleComplete = { viewModel.toggleTaskComplete(task, context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = task.status == TaskStatus.DONE
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()) }

    val priorityColor = when (task.priority) {
        Priority.URGENT -> AccentRed
        Priority.HIGH -> AccentAmber
        Priority.MEDIUM -> AccentBlue
        Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isOverdue = task.dueAt != null && task.dueAt < System.currentTimeMillis() && !isDone

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
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Priority tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due date badge
                    if (task.dueAt != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isOverdue) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateFormat.format(Date(task.dueAt)),
                                color = if (isOverdue) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Recurrence badge
                    if (!task.recurrenceRule.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = AccentEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
