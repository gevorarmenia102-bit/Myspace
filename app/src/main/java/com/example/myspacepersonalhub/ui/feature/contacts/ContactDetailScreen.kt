package com.example.myspacepersonalhub.ui.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun ContactDetailScreen(
    contactId: String,
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val contactFlow = remember(contactId) { viewModel.getContactById(contactId) }
    val contact by contactFlow.collectAsState(initial = null)
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullPhotoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Contact Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        CircleIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBackClick
                        )
                    }
                },
                actions = {
                    contact?.let { c ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            CircleIconButton(
                                icon = if (c.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (c.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                containerColor = if (c.isFavorite) AccentAmber.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                borderColor = if (c.isFavorite) AccentAmber.copy(alpha = 0.4f) else null,
                                onClick = { viewModel.toggleFavorite(c) }
                            )
                            CircleIconButton(
                                icon = Icons.Default.Edit,
                                contentDescription = "Edit Contact",
                                tint = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                onClick = { onEditClick(c.id) }
                            )
                            CircleIconButton(
                                icon = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Contact",
                                tint = AccentRed,
                                containerColor = AccentRed.copy(alpha = 0.12f),
                                borderColor = AccentRed.copy(alpha = 0.3f),
                                onClick = { showDeleteConfirm = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val currentContact = contact
        if (currentContact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            if (showFullPhotoDialog && !currentContact.profileImageLocalPath.isNullOrBlank()) {
                FullImageViewerDialog(
                    imagePath = currentContact.profileImageLocalPath,
                    name = currentContact.displayName,
                    onDismiss = { showFullPhotoDialog = false }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    InitialAvatar(
                        name = currentContact.displayName,
                        imagePath = currentContact.profileImageLocalPath,
                        size = 90.dp,
                        onClick = {
                            if (!currentContact.profileImageLocalPath.isNullOrBlank()) {
                                showFullPhotoDialog = true
                            }
                        }
                    )
                    if (!currentContact.profileImageLocalPath.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showFullPhotoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = "View Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name and Nickname
                Text(
                    text = currentContact.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!currentContact.nickname.isNullOrBlank()) {
                    Text(
                        text = "\"${currentContact.nickname}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Company & Title
                if (!currentContact.company.isNullOrBlank() || !currentContact.jobTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val titleOrg = listOfNotNull(currentContact.jobTitle, currentContact.company).joinToString(" at ")
                    Text(
                        text = titleOrg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Action Bar: Email, Maps, Share (Call, WhatsApp, and Telegram are on each Phone Number card below)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Email,
                        label = "Email",
                        enabled = !currentContact.email.isNullOrBlank(),
                        onClick = { currentContact.email?.let { IntentUtils.sendEmail(context, it) } }
                    )

                    val fullAddress = listOfNotNull(
                        currentContact.addressLine1,
                        currentContact.city,
                        currentContact.region,
                        currentContact.country
                    ).joinToString(", ")

                    QuickActionButton(
                        icon = Icons.Default.Place,
                        label = "Maps",
                        enabled = fullAddress.isNotBlank(),
                        onClick = { IntentUtils.openMapAddress(context, fullAddress) }
                    )

                    QuickActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        enabled = true,
                        onClick = {
                            val shareSummary = buildString {
                                appendLine(currentContact.displayName)
                                currentContact.phoneNumbers.forEach {
                                    val formatted = CountryPhoneHelper.formatDisplay(it.number)
                                    appendLine("${it.label}: $formatted")
                                }
                                currentContact.email?.let { appendLine("Email: $it") }
                                if (fullAddress.isNotBlank()) appendLine("Address: $fullAddress")
                            }
                            IntentUtils.shareText(context, shareSummary, "Share ${currentContact.displayName}")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Phone Numbers Section
                if (currentContact.phoneNumbers.isNotEmpty()) {
                    DetailSectionCard(title = "Phone Numbers") {
                        currentContact.phoneNumbers.forEach { phone ->
                            val detectedCountry = CountryPhoneHelper.detectCountry(phone.number)
                            val formattedNumber = CountryPhoneHelper.formatDisplay(phone.number)
                            val countryFlag = detectedCountry?.flag ?: "🌐"
                            val countryName = detectedCountry?.name ?: "International"

                            PhoneDetailCardRow(
                                flag = countryFlag,
                                countryName = countryName,
                                label = phone.label,
                                rawNumber = phone.number,
                                formattedNumber = formattedNumber,
                                onCall = { IntentUtils.openDialer(context, phone.number) },
                                onWhatsApp = { IntentUtils.openWhatsApp(context, phone.number) },
                                onTelegram = { IntentUtils.openTelegram(context, phone.number) },
                                onCopy = { IntentUtils.copyToClipboard(context, "Phone Number", phone.number) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Email Section
                if (!currentContact.email.isNullOrBlank()) {
                    DetailSectionCard(title = "Email") {
                        DetailFieldRow(
                            label = "Email Address",
                            value = currentContact.email,
                            icon = Icons.Default.Email,
                            onValueClick = { IntentUtils.sendEmail(context, currentContact.email) },
                            onCopyClick = { IntentUtils.copyToClipboard(context, "Email", currentContact.email) }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Address Section
                val hasAddress = currentContact.hasAddress()
                if (hasAddress) {
                    DetailSectionCard(title = "Address") {
                        val addrText = currentContact.fullAddress() ?: listOfNotNull(
                            currentContact.addressLine1,
                            currentContact.addressLine2,
                            currentContact.city,
                            currentContact.region,
                            currentContact.postalCode,
                            currentContact.country
                        ).filter { it.isNotBlank() }.joinToString(", ")

                        DetailFieldRow(
                            label = "Location",
                            value = addrText,
                            icon = Icons.Default.Place,
                            onValueClick = { IntentUtils.openMapAddress(context, addrText) },
                            onCopyClick = { IntentUtils.copyToClipboard(context, "Address", addrText) }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Birthday / Important Date Section
                if (!currentContact.birthday.isNullOrBlank()) {
                    DetailSectionCard(title = "Important Dates") {
                        DetailFieldRow(
                            label = "Birthday",
                            value = currentContact.birthday,
                            icon = Icons.Default.Cake,
                            onValueClick = {},
                            onCopyClick = { IntentUtils.copyToClipboard(context, "Birthday", currentContact.birthday) }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Tags Section
                if (currentContact.tags.isNotEmpty()) {
                    DetailSectionCard(title = "Tags") {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                            currentContact.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("#$tag", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Personal Notes Section
                if (!currentContact.personalNotes.isNullOrBlank()) {
                    DetailSectionCard(title = "Personal Notes") {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                text = currentContact.personalNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Contact",
            message = "Are you sure you want to delete ${contact?.displayName}? This action will also delete the contact from cloud sync.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                contact?.id?.let {
                    viewModel.deleteContact(it)
                    onBackClick()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color? = null,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (!enabled) MaterialTheme.colorScheme.surface
                    else containerColor ?: MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (!enabled) MaterialTheme.colorScheme.outline
                       else tint ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) (tint ?: MaterialTheme.colorScheme.onSurfaceVariant) else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun PhoneDetailCardRow(
    flag: String,
    countryName: String,
    label: String,
    rawNumber: String,
    formattedNumber: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onTelegram: () -> Unit,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flag, fontSize = 26.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedNumber.ifBlank { rawNumber },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$countryName • $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = onCall,
                label = { Text("Call", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                },
                shape = RoundedCornerShape(8.dp),
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )

            AssistChip(
                onClick = onWhatsApp,
                label = { Text("WhatsApp", fontSize = 12.sp, color = AccentWhatsApp) },
                leadingIcon = {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentWhatsApp)
                },
                shape = RoundedCornerShape(8.dp),
                colors = AssistChipDefaults.assistChipColors(containerColor = AccentWhatsApp.copy(alpha = 0.12f))
            )

            AssistChip(
                onClick = onTelegram,
                label = { Text("Telegram", fontSize = 12.sp, color = AccentTelegram) },
                leadingIcon = {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentTelegram)
                },
                shape = RoundedCornerShape(8.dp),
                colors = AssistChipDefaults.assistChipColors(containerColor = AccentTelegram.copy(alpha = 0.12f))
            )

            CircleIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                onClick = onCopy,
                size = 32.dp,
                iconSize = 15.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    icon: ImageVector,
    onValueClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onValueClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        CircleIconButton(
            icon = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            onClick = onCopyClick,
            size = 32.dp,
            iconSize = 15.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}
