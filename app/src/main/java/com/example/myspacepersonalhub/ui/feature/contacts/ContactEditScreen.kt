package com.example.myspacepersonalhub.ui.feature.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import java.io.ByteArrayInputStream
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.model.PhoneNumber
import com.example.myspacepersonalhub.core.util.CallerIdService
import com.example.myspacepersonalhub.core.util.CountryInfo
import com.example.myspacepersonalhub.core.util.CountryPhoneHelper
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.ConfirmationDialog
import com.example.myspacepersonalhub.ui.components.CountryPickerBottomSheet
import com.example.myspacepersonalhub.ui.components.FullImageViewerDialog
import com.example.myspacepersonalhub.ui.components.InitialAvatar
import com.example.myspacepersonalhub.core.util.SnackbarController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import java.io.File
import java.util.UUID

data class PhoneEditItem(
    val id: String = UUID.randomUUID().toString(),
    val country: CountryInfo = CountryPhoneHelper.defaultCountry(),
    val nationalNumber: String = "",
    val label: String = "Mobile"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    contactId: String?,
    viewModel: ContactsViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isEditMode = contactId != null && contactId != "new"
    val existingContactFlow = remember(contactId) {
        if (isEditMode) viewModel.getContactById(contactId!!) else kotlinx.coroutines.flow.flowOf(null)
    }
    val existingContact by existingContactFlow.collectAsState(initial = null)

    var displayName by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneEditItems by remember {
        mutableStateOf(
            listOf(
                PhoneEditItem(
                    country = CountryPhoneHelper.defaultCountry(),
                    nationalNumber = "",
                    label = "Mobile"
                )
            )
        )
    }
    var profileImageLocalPath by rememberSaveable { mutableStateOf<String?>(null) }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var personalNotes by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }

    var displayNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullPhotoDialog by remember { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriString?.let { Uri.parse(it) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    var showCountryPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var isLookingUpCallerId by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SnackbarController.showMessage("Contacts permission granted. Tap Auto-fill again.")
        } else {
            SnackbarController.showMessage("Contacts permission needed to read phone names")
        }
    }

    val processAndSaveImage: (Uri) -> Unit = { sourceUri ->
        isProcessingImage = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    input.readBytes()
                }

                if (bytes == null || bytes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        isProcessingImage = false
                        SnackbarController.showMessage("Could not read selected image file")
                    }
                    return@launch
                }

                val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
                val destFile = File(avatarDir, "avatar_${UUID.randomUUID()}.jpg")

                var orientation = ExifInterface.ORIENTATION_NORMAL
                try {
                    ByteArrayInputStream(bytes).use { stream ->
                        val exif = ExifInterface(stream)
                        orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                } catch (_: Exception) {
                }

                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (original != null) {
                    val rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    val rotated = if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                    } else original

                    val maxDim = 1024
                    val scaled = if (rotated.width > maxDim || rotated.height > maxDim) {
                        val ratio = rotated.width.toFloat() / rotated.height.toFloat()
                        val (w, h) = if (ratio > 1f) maxDim to (maxDim / ratio).toInt() else (maxDim * ratio).toInt() to maxDim
                        Bitmap.createScaledBitmap(rotated, w, h, true)
                    } else rotated

                    destFile.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    withContext(Dispatchers.Main) {
                        profileImageLocalPath = destFile.absolutePath
                        isProcessingImage = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isProcessingImage = false
                        SnackbarController.showMessage("Could not decode selected image")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isProcessingImage = false
                    SnackbarController.showMessage("Failed to process image: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            processAndSaveImage(sourceUri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUriString?.let { uriStr ->
                processAndSaveImage(Uri.parse(uriStr))
            }
        }
    }

    val launchCamera: () -> Unit = {
        try {
            val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val tempPhotoFile = File(photosDir, "captured_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempPhotoFile
            )
            tempCameraUriString = uri.toString()
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            SnackbarController.showMessage("Unable to open camera: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            SnackbarController.showMessage("Camera permission is required to take a picture")
        }
    }

    val requestCamera: () -> Unit = {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    // Populate existing values
    LaunchedEffect(existingContact) {
        existingContact?.let { c ->
            if (!isLoaded) {
                displayName = c.displayName
                givenName = c.givenName ?: ""
                familyName = c.familyName ?: ""
                nickname = c.nickname ?: ""
                email = c.email ?: ""
                phoneEditItems = if (c.phoneNumbers.isNotEmpty()) {
                    c.phoneNumbers.map { pn ->
                        val detected = CountryPhoneHelper.detectCountry(pn.number) ?: CountryPhoneHelper.defaultCountry()
                        val national = CountryPhoneHelper.extractNationalPart(pn.number, detected)
                        PhoneEditItem(
                            country = detected,
                            nationalNumber = national,
                            label = pn.label
                        )
                    }
                } else {
                    listOf(PhoneEditItem(country = CountryPhoneHelper.defaultCountry(), nationalNumber = "", label = "Mobile"))
                }
                profileImageLocalPath = c.profileImageLocalPath
                company = c.company ?: ""
                jobTitle = c.jobTitle ?: ""
                birthday = c.birthday ?: ""
                addressLine1 = c.addressLine1 ?: ""
                city = c.city ?: ""
                region = c.region ?: ""
                postalCode = c.postalCode ?: ""
                country = c.country ?: ""
                tagsInput = c.tags.joinToString(", ")
                personalNotes = c.personalNotes ?: ""
                isFavorite = c.isFavorite
                isPinned = c.isPinned
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Contact" else "New Contact", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                        }
                    }
                    Button(
                        onClick = {
                            if (displayName.trim().isBlank()) {
                                displayNameError = "Display name is required"
                                return@Button
                            } else {
                                displayNameError = null
                            }

                            if (email.isNotBlank() && !email.contains("@")) {
                                emailError = "Invalid email format"
                                return@Button
                            } else {
                                emailError = null
                            }

                            val cleanPhones = phoneEditItems
                                .map { item ->
                                    val full = CountryPhoneHelper.combineToInternational(item.country, item.nationalNumber)
                                    PhoneNumber(number = full.trim(), label = item.label)
                                }
                                .filter { it.number.isNotBlank() }

                            val parsedTags = tagsInput.split(",")
                                .map { it.trim().removePrefix("#") }
                                .filter { it.isNotBlank() }

                            val contactToSave = ContactEntity(
                                id = if (isEditMode) contactId!! else UUID.randomUUID().toString(),
                                displayName = displayName.trim(),
                                givenName = givenName.trim().ifBlank { null },
                                familyName = familyName.trim().ifBlank { null },
                                nickname = nickname.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                phoneNumbers = cleanPhones,
                                profileImageLocalPath = profileImageLocalPath,
                                profileImageStoragePath = if (profileImageLocalPath == existingContact?.profileImageLocalPath) {
                                    existingContact?.profileImageStoragePath
                                } else {
                                    null
                                },
                                addressLine1 = addressLine1.trim().ifBlank { null },
                                city = city.trim().ifBlank { null },
                                region = region.trim().ifBlank { null },
                                postalCode = postalCode.trim().ifBlank { null },
                                country = country.trim().ifBlank { null },
                                company = company.trim().ifBlank { null },
                                jobTitle = jobTitle.trim().ifBlank { null },
                                birthday = birthday.trim().ifBlank { null },
                                tags = parsedTags,
                                personalNotes = personalNotes.trim().ifBlank { null },
                                isFavorite = isFavorite,
                                isPinned = isPinned,
                                createdAt = existingContact?.createdAt ?: System.currentTimeMillis()
                            )

                            viewModel.saveContact(
                                contact = contactToSave,
                                onSuccess = onSaveSuccess,
                                onError = { displayNameError = it }
                            )
                        },
                        enabled = !isProcessingImage,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isProcessingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo & Avatar Picker Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        InitialAvatar(
                            name = displayName.ifBlank { "?" },
                            imagePath = profileImageLocalPath,
                            size = 100.dp,
                            onClick = {
                                if (isProcessingImage) return@InitialAvatar
                                if (!profileImageLocalPath.isNullOrBlank()) {
                                    showFullPhotoDialog = true
                                } else {
                                    showPhotoSourceSheet = true
                                }
                            }
                        )
                        if (isProcessingImage) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clickable { showPhotoSourceSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Pick Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showPhotoSourceSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (profileImageLocalPath.isNullOrBlank()) "Set Photo" else "Change Photo", fontSize = 13.sp)
                        }

                        if (!profileImageLocalPath.isNullOrBlank()) {
                            TextButton(
                                onClick = { profileImageLocalPath = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (showFullPhotoDialog && !profileImageLocalPath.isNullOrBlank()) {
                FullImageViewerDialog(
                    imagePath = profileImageLocalPath,
                    name = displayName.ifBlank { "Contact Photo" },
                    onDismiss = { showFullPhotoDialog = false }
                )
            }

            // Basic Info Section
            Text("BASIC INFORMATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    if (it.isNotBlank()) displayNameError = null
                },
                label = { Text("Display Name *") },
                isError = displayNameError != null,
                supportingText = displayNameError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = givenName,
                    onValueChange = { givenName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Phone Numbers Section
            Text("PHONE NUMBERS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            phoneEditItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Country Selector Button with Flag and Dial Code (e.g. 🇦🇲 +374 ▼)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCountryPickerForIndex = index }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                ) {
                                    Text(item.country.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        item.country.dialCode,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select country",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Phone Number TextField: ONLY national number digits without country code!
                            // Enforces numeric phone keyboard and strictly strips non-digits
                            val sampleNational = remember(item.country) {
                                item.country.sampleFormat.removePrefix(item.country.dialCode).trim()
                            }

                            OutlinedTextField(
                                value = item.nationalNumber,
                                onValueChange = { input ->
                                    if (input.contains("+")) {
                                        // Pasted international number (e.g. +374 94 443286)
                                        val detected = CountryPhoneHelper.detectCountry(input) ?: item.country
                                        val national = CountryPhoneHelper.extractNationalPart(input, detected)
                                        val updated = phoneEditItems.toMutableList()
                                        updated[index] = item.copy(country = detected, nationalNumber = national)
                                        phoneEditItems = updated
                                    } else {
                                        // Allow digits, spaces, and hyphens without mutating string underneath cursor
                                        val clean = input.filter { it.isDigit() || it == ' ' || it == '-' }
                                        val updated = phoneEditItems.toMutableList()
                                        updated[index] = item.copy(nationalNumber = clean)
                                        phoneEditItems = updated
                                    }
                                },
                                label = { Text("Local Number (${item.country.iso2})") },
                                placeholder = { Text(sampleNational) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Remove button
                            IconButton(
                                onClick = {
                                    if (phoneEditItems.size > 1) {
                                        phoneEditItems = phoneEditItems.filterIndexed { i, _ -> i != index }
                                    } else {
                                        phoneEditItems = listOf(item.copy(nationalNumber = ""))
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions & Label Row: Auto-fill Name | WhatsApp | Telegram | Label Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val digitCount = item.nationalNumber.filter { it.isDigit() }.length
                            val fullNumber = remember(item.country, item.nationalNumber) {
                                CountryPhoneHelper.combineToInternational(item.country, item.nationalNumber)
                            }

                            // Caller ID / Truecaller Auto-Fill Button
                            FilledTonalButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }

                                    coroutineScope.launch {
                                        isLookingUpCallerId = true
                                        // 1. First check local contacts
                                        val localFound = withContext(Dispatchers.IO) {
                                            CallerIdService.queryDeviceContacts(context, fullNumber)
                                        }

                                        if (localFound != null) {
                                            isLookingUpCallerId = false
                                            displayName = localFound.name
                                            val (first, last) = CallerIdService.splitFullName(localFound.name)
                                            if (givenName.isBlank()) givenName = first
                                            if (familyName.isBlank()) familyName = last
                                            SnackbarController.showMessage("✨ Auto-filled: ${localFound.name} (Device Contacts)")
                                            return@launch
                                        }

                                        // 2. Live query with token configured in Cloud Settings (no popup dialog)
                                        val result = CallerIdService.lookupCallerId(
                                            context = context,
                                            rawPhone = fullNumber,
                                            countryIso = item.country.iso2
                                        )
                                        isLookingUpCallerId = false
                                        if (result != null) {
                                            displayName = result.name
                                            val (first, last) = CallerIdService.splitFullName(result.name)
                                            if (givenName.isBlank()) givenName = first
                                            if (familyName.isBlank()) familyName = last
                                            SnackbarController.showMessage("✨ Auto-filled: ${result.name} (${result.source})")
                                        } else {
                                            val token = CallerIdService.getTruecallerToken(context)
                                            if (token.isNullOrBlank()) {
                                                SnackbarController.showMessage("No contact found (Configure Truecaller in Cloud Settings for live search)")
                                            } else {
                                                SnackbarController.showMessage("No name found for $fullNumber")
                                            }
                                        }
                                    }
                                },
                                enabled = digitCount >= 4 && !isLookingUpCallerId,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isLookingUpCallerId) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Auto-fill Name", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val hasDigits = digitCount >= 5

                                // WhatsApp quick launch
                                IconButton(
                                    onClick = { IntentUtils.openWhatsApp(context, fullNumber) },
                                    enabled = hasDigits,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = if (hasDigits) AccentWhatsApp else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Telegram quick launch
                                IconButton(
                                    onClick = { IntentUtils.openTelegram(context, fullNumber) },
                                    enabled = hasDigits,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Telegram",
                                        tint = if (hasDigits) AccentTelegram else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Label dropdown/selector
                                var showLabelMenu by remember { mutableStateOf(false) }
                                Box {
                                    FilterChip(
                                        selected = false,
                                        onClick = { showLabelMenu = true },
                                        label = { Text(item.label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    )
                                    DropdownMenu(
                                        expanded = showLabelMenu,
                                        onDismissRequest = { showLabelMenu = false }
                                    ) {
                                        listOf("Mobile", "WhatsApp", "Telegram", "Work", "Home", "Main", "Other").forEach { lbl ->
                                            DropdownMenuItem(
                                                text = { Text(lbl) },
                                                onClick = {
                                                    val updated = phoneEditItems.toMutableList()
                                                    updated[index] = item.copy(label = lbl)
                                                    phoneEditItems = updated
                                                    showLabelMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            TextButton(
                onClick = {
                    phoneEditItems = phoneEditItems + PhoneEditItem(
                        country = CountryPhoneHelper.defaultCountry(),
                        nationalNumber = "",
                        label = "Mobile"
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Another Phone Number")
            }

            // Email Section
            Text("EMAIL ADDRESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email (Optional)") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Company & Work
            Text("ORGANIZATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Address
            Text("ADDRESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = addressLine1,
                onValueChange = { addressLine1 = it },
                label = { Text("Street Address (e.g. Amerikyan St 102)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City (e.g. Yerevan)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country (e.g. Armenia)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Birthday & Tags
            Text("DATES & TAGS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = birthday,
                onValueChange = { birthday = it },
                label = { Text("Birthday (e.g. 1995-08-24)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated, e.g. friend, work)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Personal Notes
            Text("PERSONAL NOTES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = personalNotes,
                onValueChange = { personalNotes = it },
                label = { Text("Private Notes (Treat as confidential)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pin to Top of List", fontWeight = FontWeight.Medium)
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mark as Favorite", fontWeight = FontWeight.Medium)
                Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Contact",
            message = "Are you sure you want to permanently delete this contact?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                contactId?.let { viewModel.deleteContact(it) }
                onSaveSuccess()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showPhotoSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select an option to update contact photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                )

                // Option 1: Take Photo with Camera
                Surface(
                    onClick = {
                        showPhotoSourceSheet = false
                        requestCamera()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Take Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use camera to capture a new photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Pick from Gallery
                Surface(
                    onClick = {
                        showPhotoSourceSheet = false
                        photoPickerLauncher.launch("image/*")
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select an existing photo from storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!profileImageLocalPath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 3: Remove Current Photo
                    Surface(
                        onClick = {
                            showPhotoSourceSheet = false
                            profileImageLocalPath = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = AccentRed.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = AccentRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Remove Photo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentRed
                                )
                                Text(
                                    text = "Remove current contact picture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCountryPickerForIndex != null) {
        val targetIdx = showCountryPickerForIndex!!
        val currentItem = phoneEditItems.getOrNull(targetIdx)
        val selectedIso = currentItem?.country?.iso2

        CountryPickerBottomSheet(
            selectedIso = selectedIso,
            onDismissRequest = { showCountryPickerForIndex = null },
            onCountrySelected = { country ->
                if (targetIdx in phoneEditItems.indices) {
                    val p = phoneEditItems[targetIdx]
                    val reFormatted = CountryPhoneHelper.formatNationalAsYouType(p.nationalNumber, country)
                    val updated = phoneEditItems.toMutableList()
                    updated[targetIdx] = p.copy(country = country, nationalNumber = reFormatted)
                    phoneEditItems = updated
                }
                showCountryPickerForIndex = null
            }
        )
    }
}
