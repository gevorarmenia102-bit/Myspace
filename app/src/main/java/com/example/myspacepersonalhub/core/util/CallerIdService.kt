package com.example.myspacepersonalhub.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CallerIdResult(
    val name: String,
    val source: String,
    val country: String? = null,
    val carrier: String? = null
)

object CallerIdService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Looks up name for a phone number using:
     * 1. Device Contacts & Call Logs (ContactsContract.PhoneLookup)
     * 2. Truecaller / Cloud Caller ID API if available
     */
    suspend fun lookupCallerId(
        context: Context,
        rawPhone: String,
        countryIso: String? = null,
        truecallerToken: String? = null
    ): CallerIdResult? = withContext(Dispatchers.IO) {
        val clean = rawPhone.trim()
        if (clean.length < 5) return@withContext null

        // 1. Check local Android Contacts & SIM via PhoneLookup
        val localResult = queryDeviceContacts(context, clean)
        if (localResult != null) {
            return@withContext localResult
        }

        // 2. Query Truecaller API if token is provided or fallback cloud reverse directory
        val onlineResult = queryOnlineCallerId(clean, countryIso, truecallerToken)
        if (onlineResult != null) {
            return@withContext onlineResult
        }

        null
    }

    private fun queryDeviceContacts(context: Context, phoneNumber: String): CallerIdResult? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        var cursor: Cursor? = null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )

            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        return CallerIdResult(name = name.trim(), source = "Device Contacts")
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    private fun queryOnlineCallerId(
        phoneNumber: String,
        countryIso: String?,
        truecallerToken: String?
    ): CallerIdResult? {
        val digits = phoneNumber.filter { it.isDigit() }
        if (digits.length < 6) return null

        val iso = countryIso ?: CountryPhoneHelper.detectCountry(phoneNumber)?.iso2 ?: "BD"

        // If Truecaller Bearer token is provided
        if (!truecallerToken.isNullOrBlank()) {
            try {
                val url = "https://search5-noneu.truecaller.com/v2/search?q=${phoneNumber}&countryCode=$iso&type=4"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer ${truecallerToken.trim()}")
                    .header("User-Agent", "Truecaller/14.0.0 (Android;14)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val data = json.optJSONArray("data")
                            if (data != null && data.length() > 0) {
                                val first = data.getJSONObject(0)
                                val name = first.optString("name").trim()
                                if (name.isNotBlank()) {
                                    val carrier = first.optJSONObject("phones")?.optString("carrier")
                                    return CallerIdResult(
                                        name = name,
                                        source = "Truecaller",
                                        country = iso,
                                        carrier = carrier
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Public reverse lookup fallback query
        try {
            val normalized = CountryPhoneHelper.normalizeToE164(phoneNumber)
            val fallbackUrl = "https://api.veriphone.io/v2/verify?phone=${Uri.encode(normalized)}"
            val request = Request.Builder()
                .url(fallbackUrl)
                .header("User-Agent", "Mozilla/5.0 MySpaceHub/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val name = json.optString("carrier_name", "").ifBlank {
                            json.optString("company_name", "")
                        }
                        if (name.isNotBlank() && name != "null") {
                            return CallerIdResult(
                                name = name,
                                source = "Caller ID Directory",
                                country = json.optString("country", iso),
                                carrier = json.optString("carrier").ifBlank { null }
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Splits full name into (Given Name, Family Name).
     */
    fun splitFullName(fullName: String): Pair<String, String> {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> parts[0] to ""
            else -> parts[0] to parts.subList(1, parts.size).joinToString(" ")
        }
    }
}
