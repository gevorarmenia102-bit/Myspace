package com.example.myspacepersonalhub.ui.feature.contacts

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.database.entity.fullAddress
import com.example.myspacepersonalhub.core.database.entity.hasAddress
import com.example.myspacepersonalhub.core.util.CountryPhoneHelper
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactsViewModel,
    onContactClick: (String) -> Unit,
    onAddContactClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedContactForFullImage by remember { mutableStateOf<ContactEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Contacts",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${state.filteredContacts.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.IosShare,
                            contentDescription = "Export / Share Contacts",
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            onClick = { showExportDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
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
            // Search Bar
            SearchBarField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search name, phone, email, tags..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filters & Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Menu Chip
                Box {
                    FilterChip(
                        selected = state.sortOrder != ContactSortOrder.NAME_ASC,
                        onClick = { showSortMenu = true },
                        label = {
                            val label = when (state.sortOrder) {
                                ContactSortOrder.NAME_ASC -> "Sort: A-Z"
                                ContactSortOrder.RECENTLY_UPDATED -> "Sort: Updated"
                                ContactSortOrder.RECENTLY_ADDED -> "Sort: Added"
                                ContactSortOrder.FAVORITES_FIRST -> "Sort: Favorites"
                            }
                            Text(label, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name A–Z") },
                            onClick = {
                                viewModel.onSortOrderChange(ContactSortOrder.NAME_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Favorites First") },
                            onClick = {
                                viewModel.onSortOrderChange(ContactSortOrder.FAVORITES_FIRST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Updated") },
                            onClick = {
                                viewModel.onSortOrderChange(ContactSortOrder.RECENTLY_UPDATED)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Added") },
                            onClick = {
                                viewModel.onSortOrderChange(ContactSortOrder.RECENTLY_ADDED)
                                showSortMenu = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Favorites Only Chip
                FilterChip(
                    selected = state.filterFavoritesOnly,
                    onClick = viewModel::toggleFavoritesFilter,
                    label = { Text("Favorites", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            if (state.filterFavoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (state.filterFavoritesOnly) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Tag Chips
                state.availableTags.forEach { tag ->
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.selectedTag == tag,
                        onClick = { viewModel.onSelectTag(tag) },
                        label = { Text("#$tag", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact List or Empty State
            if (state.isLoading) {
                LoadingStateView(message = "Loading contacts...")
            } else if (state.filteredContacts.isEmpty()) {
                EmptyStateView(
                    title = if (state.searchQuery.isNotBlank()) "No Matching Contacts" else "No Contacts Yet",
                    message = if (state.searchQuery.isNotBlank()) "Try searching with a different name or number." else "Save personal contacts with phone numbers, emails, addresses, and notes.",
                    icon = Icons.Default.PersonSearch,
                    actionLabel = if (state.searchQuery.isBlank()) "Add Your First Contact" else null,
                    onAction = onAddContactClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredContacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onClick = { onContactClick(contact.id) },
                            onAvatarClick = {
                                if (!contact.profileImageLocalPath.isNullOrBlank()) {
                                    selectedContactForFullImage = contact
                                } else {
                                    onContactClick(contact.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(contact) }
                        )
                    }
                }
            }
        }
    }

    if (selectedContactForFullImage != null && !selectedContactForFullImage?.profileImageLocalPath.isNullOrBlank()) {
        FullImageViewerDialog(
            imagePath = selectedContactForFullImage!!.profileImageLocalPath,
            name = selectedContactForFullImage!!.displayName,
            onDismiss = { selectedContactForFullImage = null }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export & Share Contacts", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You can export your contacts as standard vCard (.vcf) or structured JSON. Notice: Exported files are in plaintext. Handle them with care.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vCard = viewModel.exportToVCard()
                        IntentUtils.shareText(context, vCard, "Share vCard Contacts")
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Share vCard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val json = viewModel.exportToJson()
                        IntentUtils.shareText(context, json, "Share Contacts JSON")
                        showExportDialog = false
                    }
                ) {
                    Text("Share JSON")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAvatarClick: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialAvatar(
                name = contact.displayName,
                imagePath = contact.profileImageLocalPath,
                size = 46.dp,
                onClick = onAvatarClick
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AccentEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                val primaryPhone = contact.phoneNumbers.firstOrNull()?.number
                val formattedPhone = primaryPhone?.let {
                    val country = CountryPhoneHelper.detectCountry(it)
                    val flag = country?.flag ?: "📞"
                    val display = CountryPhoneHelper.formatDisplay(it)
                    "$flag $display"
                }

                val subtitle = when {
                    !contact.jobTitle.isNullOrBlank() && !contact.company.isNullOrBlank() ->
                        "${contact.jobTitle} • ${contact.company}"
                    !contact.company.isNullOrBlank() -> contact.company
                    formattedPhone != null -> formattedPhone
                    !contact.email.isNullOrBlank() -> contact.email
                    else -> null
                }

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick WhatsApp launcher if primary phone is available
                val primaryPhone = contact.phoneNumbers.firstOrNull()?.number
                if (!primaryPhone.isNullOrBlank()) {
                    IconButton(
                        onClick = { IntentUtils.openWhatsApp(context, primaryPhone) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = AccentWhatsApp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Maps Icon beside Star if address is set
                if (contact.hasAddress()) {
                    IconButton(
                        onClick = {
                            contact.fullAddress()?.let { address ->
                                IntentUtils.openMapAddress(context, address)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Open in Maps",
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (contact.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (contact.isFavorite) AccentAmber else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
