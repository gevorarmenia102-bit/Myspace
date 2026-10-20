package com.example.myspacepersonalhub.data.repository

import com.example.myspacepersonalhub.core.database.dao.ContactDao
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.util.ContactVCardUtils
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {

    fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()

    fun getFavoriteContacts(): Flow<List<ContactEntity>> = contactDao.getFavoriteContacts()

    fun searchContacts(query: String): Flow<List<ContactEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) contactDao.getAllContacts() else contactDao.searchContacts(trimmed)
    }

    fun getContactById(id: String): Flow<ContactEntity?> = contactDao.getContactById(id)

    suspend fun saveContact(contact: ContactEntity): Result<Unit> {
        val trimmedName = contact.displayName.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Contact display name cannot be blank"))
        }

        val updatedContact = contact.copy(
            displayName = trimmedName,
            givenName = contact.givenName?.trim(),
            familyName = contact.familyName?.trim(),
            nickname = contact.nickname?.trim(),
            email = contact.email?.trim()?.ifBlank { null },
            addressLine1 = contact.addressLine1?.trim()?.ifBlank { null },
            addressLine2 = contact.addressLine2?.trim()?.ifBlank { null },
            city = contact.city?.trim()?.ifBlank { null },
            region = contact.region?.trim()?.ifBlank { null },
            postalCode = contact.postalCode?.trim()?.ifBlank { null },
            country = contact.country?.trim()?.ifBlank { null },
            company = contact.company?.trim()?.ifBlank { null },
            jobTitle = contact.jobTitle?.trim()?.ifBlank { null },
            birthday = contact.birthday?.trim()?.ifBlank { null },
            personalNotes = contact.personalNotes?.trim()?.ifBlank { null },
            updatedAt = System.currentTimeMillis(),
            syncState = if (contact.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else contact.syncState
        )

        contactDao.insertContact(updatedContact)
        return Result.success(Unit)
    }

    suspend fun deleteContact(id: String) {
        contactDao.softDeleteContact(id)
    }

    suspend fun importContacts(contacts: List<ContactEntity>) {
        contactDao.insertContacts(contacts)
    }

    fun getContactCount(): Flow<Int> = contactDao.getContactCount()
}
