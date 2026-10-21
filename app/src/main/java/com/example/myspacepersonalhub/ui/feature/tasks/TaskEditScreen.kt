package com.example.myspacepersonalhub.ui.feature.tasks

import androidx.compose.foundation.horizontalScroll
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
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.model.TaskStatus
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String?,
    viewModel: TasksViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = taskId != null && taskId != "new"
    val existingTaskFlow = remember(taskId) {
        if (isEditMode) viewModel.getTaskById(taskId!!) else kotlinx.coroutines.flow.flowOf(null)
    }
    val existingTask by existingTaskFlow.collectAsState(initial = null)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var status by remember { mutableStateOf(TaskStatus.TODO) }
    var dueAt by remember { mutableStateOf<Long?>(null) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var recurrenceRule by remember { mutableStateOf("NONE") }
    var category by remember { mutableStateOf("Personal") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(existingTask) {
        existingTask?.let { t ->
            if (!isLoaded) {
                title = t.title
                description = t.description ?: ""
                priority = t.priority
                status = t.status
                dueAt = t.dueAt
                reminderEnabled = t.reminderAt != null
                recurrenceRule = t.recurrenceRule ?: "NONE"
                category = t.category ?: "Personal"
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Cancel",
                            onClick = onBackClick
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        if (isEditMode) {
                            CircleIconButton(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = AccentRed,
                                containerColor = AccentRed.copy(alpha = 0.12f),
                                borderColor = AccentRed.copy(alpha = 0.3f),
                                onClick = { showDeleteConfirm = true }
                            )
                        }
                        HeaderSaveButton(
                            onClick = {
                                if (title.trim().isBlank()) {
                                    titleError = "Task title is required"
                                    return@HeaderSaveButton
                                }

                                val taskToSave = TaskEntity(
                                    id = if (isEditMode) taskId!! else UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    dueAt = dueAt,
                                    reminderAt = if (reminderEnabled) dueAt else null,
                                    priority = priority,
                                    status = status,
                                    recurrenceRule = if (recurrenceRule == "NONE") null else recurrenceRule,
                                    category = category.trim().ifBlank { "Personal" },
                                    createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                                )

                                viewModel.saveTask(
                                    task = taskToSave,
                                    context = context,
                                    onSuccess = onSaveSuccess,
                                    onError = { titleError = it }
                                )
                            }
                        )
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Task Title *") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Priority Selector
            Text("PRIORITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.values().forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name, fontSize = 12.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }

            // Status Selector (if editing)
            if (isEditMode) {
                Text("STATUS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE).forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.name.replace("_", " "), fontSize = 12.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }

            // Quick Due Date Selector
            Text("SCHEDULE DUE DATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy - h:mm a", Locale.getDefault()) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (dueAt != null) "Due: ${dateFormat.format(Date(dueAt!!))}" else "No due date set",
                        fontWeight = FontWeight.SemiBold,
                        color = if (dueAt != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 18)
                                    set(Calendar.MINUTE, 0)
                                }
                                dueAt = cal.timeInMillis
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Today 6 PM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                }
                                dueAt = cal.timeInMillis
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Tomorrow 9 AM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 7)
                                    set(Calendar.HOUR_OF_DAY, 10)
                                    set(Calendar.MINUTE, 0)
                                }
                                dueAt = cal.timeInMillis
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("In 1 Week", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (dueAt != null) {
                            TextButton(onClick = { dueAt = null }) {
                                Text("Clear", color = AccentRed)
                            }
                        }
                    }
                }
            }

            // Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Remind Me with Notification", fontWeight = FontWeight.SemiBold)
                    Text("Trigger an alarm when the task is due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reminderEnabled && dueAt != null, onCheckedChange = { reminderEnabled = it && dueAt != null }, enabled = dueAt != null)
            }

            // Recurrence
            Text("REPEAT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { rule ->
                    FilterChip(
                        selected = recurrenceRule == rule,
                        onClick = { recurrenceRule = rule },
                        label = { Text(if (rule == "NONE") "Never" else rule.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Task",
            message = "Are you sure you want to delete this task?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                existingTask?.let { viewModel.deleteTask(it, context) }
                onSaveSuccess()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
