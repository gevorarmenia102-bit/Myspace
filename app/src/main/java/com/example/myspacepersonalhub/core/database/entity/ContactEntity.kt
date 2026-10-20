package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.PhoneNumber
import com.example.myspacepersonalhub.core.model.SyncState
import java.util.UUID

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    val profileImageLocalPath: String? = null,
    val profileImageStoragePath: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val company: String? = null,
    val jobTitle: String? = null,
    val birthday: String? = null,
    val tags: List<String> = emptyList(),
    val personalNotes: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)

fun ContactEntity.fullAddress(): String? {
    val parts = listOfNotNull(
        addressLine1,
        addressLine2,
        city,
        region,
        postalCode,
        country
    ).filter { it.isNotBlank() }
    return if (parts.isEmpty()) null else parts.joinToString(", ")
}

fun ContactEntity.hasAddress(): Boolean = !fullAddress().isNullOrBlank()
