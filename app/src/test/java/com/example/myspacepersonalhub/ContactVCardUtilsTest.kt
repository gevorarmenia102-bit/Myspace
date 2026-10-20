package com.example.myspacepersonalhub

import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.model.PhoneNumber
import com.example.myspacepersonalhub.core.util.ContactVCardUtils
import org.junit.Assert.*
import org.junit.Test

class ContactVCardUtilsTest {

    @Test
    fun testJsonExportAndImportRoundTrip() {
        val originalContacts = listOf(
            ContactEntity(
                displayName = "Արամ Խաչատրյան",
                email = "aram@example.am",
                phoneNumbers = listOf(
                    PhoneNumber(number = "+374 10 123456", label = "Work", isPrimary = true),
                    PhoneNumber(number = "+374 91 654321", label = "Mobile")
                ),
                company = "Yerevan Philharmonic",
                jobTitle = "Composer",
                city = "Yerevan",
                country = "Armenia",
                tags = listOf("music", "vip"),
                personalNotes = "Prominent 20th century composer"
            ),
            ContactEntity(
                displayName = "কাজী নজরুল ইসলাম",
                email = "nazrul@example.bd",
                phoneNumbers = listOf(
                    PhoneNumber(number = "+880 17 00000000", label = "Mobile", isPrimary = true)
                ),
                company = "National Poet Society",
                jobTitle = "Poet & Writer",
                city = "Dhaka",
                country = "Bangladesh",
                tags = listOf("literature", "rebel")
            )
        )

        val jsonString = ContactVCardUtils.exportToJson(originalContacts)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Արամ Խաչատրյան"))
        assertTrue(jsonString.contains("কাজী নজরুল ইসলাম"))

        val imported = ContactVCardUtils.importFromJson(jsonString)
        assertEquals(2, imported.size)
        assertEquals("Արամ Խաչատրյան", imported[0].displayName)
        assertEquals("কাজী নজরুল ইসলাম", imported[1].displayName)
        assertEquals(2, imported[0].phoneNumbers.size)
        assertEquals("+374 10 123456", imported[0].phoneNumbers[0].number)
    }

    @Test
    fun testVCardExportFormat() {
        val contact = ContactEntity(
            displayName = "Dr. Jane Doe",
            familyName = "Doe",
            givenName = "Jane",
            email = "jane.doe@example.org",
            phoneNumbers = listOf(PhoneNumber(number = "+1 555 123 4567", label = "Work")),
            company = "Acme Health",
            jobTitle = "Chief Physician"
        )

        val vCard = ContactVCardUtils.exportToVCard(listOf(contact))
        assertTrue(vCard.contains("BEGIN:VCARD"))
        assertTrue(vCard.contains("VERSION:3.0"))
        assertTrue(vCard.contains("FN:Dr. Jane Doe"))
        assertTrue(vCard.contains("N:Doe;Jane;;;"))
        assertTrue(vCard.contains("TEL;TYPE=WORK:+1 555 123 4567"))
        assertTrue(vCard.contains("EMAIL;TYPE=INTERNET:jane.doe@example.org"))
        assertTrue(vCard.contains("ORG:Acme Health"))
        assertTrue(vCard.contains("END:VCARD"))
    }
}
