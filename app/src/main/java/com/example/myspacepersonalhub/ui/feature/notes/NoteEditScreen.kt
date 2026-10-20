package com.example.myspacepersonalhub.ui.feature.notes

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.ConfirmationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: String?,
    isJournal: Boolean,
    viewModel: NotesViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val isEditMode = noteId != null && noteId != "new"
    val existingNoteFlow = remember(noteId) {
        if (isEditMode) viewModel.getNoteById(noteId!!) else kotlinx.coroutines.flow.flowOf(null)
    }
    val existingNote by existingNoteFlow.collectAsState(initial = null)

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (isJournal) "Journal" else "General") }
    var tagsInput by remember { mutableStateOf("") }
    var journalMood by remember { mutableStateOf(if (isJournal) "Good" else null) }
    var journalDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var isPinned by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var showTrashConfirm by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    val moodOptions = listOf("Great", "Good", "Neutral", "Low", "Tired", "Stressed", "Reflective")

    LaunchedEffect(existingNote) {
        existingNote?.let { n ->
            if (!isLoaded) {
                title = n.title
                body = n.body
                category = n.category
                tagsInput = n.tags.joinToString(", ")
                journalMood = n.journalMood
                journalDate = n.journalDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                isPinned = n.isPinned
                isFavorite = n.isFavorite
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit ${if (isJournal) "Journal" else "Note"}" else "New ${if (isJournal) "Journal" else "Note"}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) AccentEmerald else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isEditMode) {
                        IconButton(onClick = { showTrashConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = AccentRed)
                        }
                    }
                    Button(
                        onClick = {
                            if (title.trim().isBlank() && body.trim().isBlank()) {
                                titleError = "Please enter a title or body"
                                return@Button
                            }

                            val parsedTags = tagsInput.split(",")
                                .map { it.trim().removePrefix("#") }
                                .filter { it.isNotBlank() }

                            val noteToSave = NoteEntity(
                                id = if (isEditMode) noteId!! else UUID.randomUUID().toString(),
                                title = title.trim().ifBlank { "Untitled Note" },
                                body = body,
                                category = category.trim().ifBlank { "General" },
                                tags = parsedTags,
                                isJournal = isJournal || existingNote?.isJournal == true,
                                journalMood = journalMood,
                                journalDate = journalDate,
                                isPinned = isPinned,
                                isFavorite = isFavorite,
                                createdAt = existingNote?.createdAt ?: System.currentTimeMillis()
                            )

                            viewModel.saveNote(
                                note = noteToSave,
                                onSuccess = onSaveSuccess,
                                onError = { titleError = it }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Journal Mood Selector
            if (isJournal || existingNote?.isJournal == true) {
                Text("MOOD", style = MaterialTheme.typography.labelMedium, color = AccentPurple, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moodOptions.forEach { mood ->
                        FilterChip(
                            selected = journalMood == mood,
                            onClick = { journalMood = mood },
                            label = { Text(mood, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Title") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category & Tags Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Body
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(if (isJournal) "Journal entry..." else "Note content...") },
                minLines = 10,
                maxLines = 25,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showTrashConfirm) {
        ConfirmationDialog(
            title = "Move to Trash",
            message = "This note will be moved to the Trash. You can restore it later or delete it permanently.",
            confirmLabel = "Move to Trash",
            isDestructive = true,
            onConfirm = {
                noteId?.let { viewModel.moveToTrash(it) }
                onSaveSuccess()
            },
            onDismiss = { showTrashConfirm = false }
        )
    }
}
