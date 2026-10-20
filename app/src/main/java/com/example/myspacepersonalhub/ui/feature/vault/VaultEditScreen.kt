package com.example.myspacepersonalhub.ui.feature.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myspacepersonalhub.theme.AccentPurple
import com.example.myspacepersonalhub.theme.AccentRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEditScreen(
    itemId: String?,
    viewModel: VaultViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val isEditMode = itemId != null && itemId != "new"
    val existingItemFlow = remember(itemId) {
        if (isEditMode) viewModel.getVaultItemById(itemId!!) else kotlinx.coroutines.flow.flowOf(null)
    }
    val existingItem by existingItemFlow.collectAsState(initial = null)

    var title by remember { mutableStateOf("") }
    var secretText by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(existingItem) {
        existingItem?.let { item ->
            if (!isLoaded) {
                title = item.title
                try {
                    secretText = viewModel.decryptNote(item)
                } catch (_: Exception) {}
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Secure Note" else "New Secure Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.trim().isBlank()) {
                                titleError = "Title is required"
                                return@Button
                            }

                            if (isEditMode) {
                                viewModel.updateSecureNote(
                                    id = itemId!!,
                                    title = title.trim(),
                                    secretText = secretText,
                                    onSuccess = onSaveSuccess,
                                    onError = { titleError = it }
                                )
                            } else {
                                viewModel.saveSecureNote(
                                    title = title.trim(),
                                    secretText = secretText,
                                    onSuccess = onSaveSuccess,
                                    onError = { titleError = it }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Encrypt & Save", fontWeight = FontWeight.SemiBold)
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
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Title *") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = secretText,
                onValueChange = { secretText = it },
                label = { Text("Confidential Note / Passwords / Sensitive Information") },
                minLines = 8,
                maxLines = 20,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "Notice: Text entered here will be encrypted using an AES-256 key generated in the Android Keystore. Plaintext is never stored in SQLite or written to logs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
