package com.example.myspacepersonalhub.core.util

import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.model.PhoneNumber
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ContactExportDto(
    val id: String,
    val displayName: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val phoneNumbers: List<PhoneNumber> = emptyList(),
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
    val isPinned: Boolean = false
)

object ContactVCardUtils {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun exportToJson(contacts: List<ContactEntity>): String {
        val dtos = contacts.map { c ->
            ContactExportDto(
                id = c.id,
                displayName = c.displayName,
                givenName = c.givenName,
                familyName = c.familyName,
                nickname = c.nickname,
                email = c.email,
                phoneNumbers = c.phoneNumbers,
                addressLine1 = c.addressLine1,
                addressLine2 = c.addressLine2,
                city = c.city,
                region = c.region,
                postalCode = c.postalCode,
                country = c.country,
                company = c.company,
                jobTitle = c.jobTitle,
                birthday = c.birthday,
                tags = c.tags,
                personalNotes = c.personalNotes,
                isFavorite = c.isFavorite,
                isPinned = c.isPinned
            )
        }
        return json.encodeToString(dtos)
    }

    fun importFromJson(jsonString: String): List<ContactEntity> {
        val dtos: List<ContactExportDto> = json.decodeFromString(jsonString)
        return dtos.map { d ->
            ContactEntity(
                id = d.id.ifBlank { java.util.UUID.randomUUID().toString() },
                displayName = d.displayName.trim(),
                givenName = d.givenName?.trim(),
                familyName = d.familyName?.trim(),
                nickname = d.nickname?.trim(),
                email = d.email?.trim(),
                phoneNumbers = d.phoneNumbers,
                addressLine1 = d.addressLine1?.trim(),
                addressLine2 = d.addressLine2?.trim(),
                city = d.city?.trim(),
                region = d.region?.trim(),
                postalCode = d.postalCode?.trim(),
                country = d.country?.trim(),
                company = d.company?.trim(),
                jobTitle = d.jobTitle?.trim(),
                birthday = d.birthday?.trim(),
                tags = d.tags,
                personalNotes = d.personalNotes?.trim(),
                isFavorite = d.isFavorite,
                isPinned = d.isPinned
            )
        }
    }

    fun exportToVCard(contacts: List<ContactEntity>): String {
        val sb = StringBuilder()
        for (c in contacts) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            sb.appendLine("FN:${c.displayName}")
            if (!c.familyName.isNullOrBlank() || !c.givenName.isNullOrBlank()) {
                sb.appendLine("N:${c.familyName ?: ""};${c.givenName ?: ""};;;")
            }
            if (!c.nickname.isNullOrBlank()) {
                sb.appendLine("NICKNAME:${c.nickname}")
            }
            if (!c.company.isNullOrBlank()) {
                sb.appendLine("ORG:${c.company}")
            }
            if (!c.jobTitle.isNullOrBlank()) {
                sb.appendLine("TITLE:${c.jobTitle}")
            }
            if (!c.email.isNullOrBlank()) {
                sb.appendLine("EMAIL;TYPE=INTERNET:${c.email}")
            }
            for (p in c.phoneNumbers) {
                val type = p.label.uppercase().ifBlank { "CELL" }
                sb.appendLine("TEL;TYPE=$type:${p.number}")
            }
            val addressParts = listOfNotNull(
                c.addressLine1,
                c.addressLine2,
                c.city,
                c.region,
                c.postalCode,
                c.country
            )
            if (addressParts.isNotEmpty()) {
                sb.appendLine("ADR;TYPE=HOME:;;${c.addressLine1 ?: ""};${c.city ?: ""};${c.region ?: ""};${c.postalCode ?: ""};${c.country ?: ""}")
            }
            if (!c.birthday.isNullOrBlank()) {
                sb.appendLine("BDAY:${c.birthday}")
            }
            if (!c.personalNotes.isNullOrBlank()) {
                sb.appendLine("NOTE:${c.personalNotes.replace("\n", "\\n")}")
            }
            sb.appendLine("END:VCARD")
        }
        return sb.toString()
    }
}
