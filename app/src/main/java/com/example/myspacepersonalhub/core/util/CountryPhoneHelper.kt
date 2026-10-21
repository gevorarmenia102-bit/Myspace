package com.example.myspacepersonalhub.core.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber as ProtoPhoneNumber

data class CountryInfo(
    val iso2: String,
    val dialCode: String,
    val name: String,
    val flag: String,
    val sampleFormat: String
)

object CountryPhoneHelper {

    private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    val countries: List<CountryInfo> = listOf(
        CountryInfo("BD", "+880", "Bangladesh", "🇧🇩", "+880 1642-581346"),
        CountryInfo("AM", "+374", "Armenia", "🇦🇲", "+374 95 110691"),
        CountryInfo("OM", "+968", "Oman", "🇴🇲", "+968 7812 3052"),
        CountryInfo("US", "+1", "United States", "🇺🇸", "+1 (555) 123-4567"),
        CountryInfo("AE", "+971", "United Arab Emirates", "🇦🇪", "+971 50 123 4567"),
        CountryInfo("SA", "+966", "Saudi Arabia", "🇸🇦", "+966 50 123 4567"),
        CountryInfo("IN", "+91", "India", "🇮🇳", "+91 98765 43210"),
        CountryInfo("GB", "+44", "United Kingdom", "🇬🇧", "+44 7911 123456"),
        CountryInfo("RU", "+7", "Russia", "🇷🇺", "+7 912 345-67-89"),
        CountryInfo("CA", "+1", "Canada", "🇨🇦", "+1 (416) 555-0199"),
        CountryInfo("QA", "+974", "Qatar", "🇶🇦", "+974 5512 3456"),
        CountryInfo("KW", "+965", "Kuwait", "🇰🇼", "+965 9123 4567"),
        CountryInfo("BH", "+973", "Bahrain", "🇧🇭", "+973 3912 3456"),
        CountryInfo("TR", "+90", "Turkey", "🇹🇷", "+90 532 123 45 67"),
        CountryInfo("MY", "+60", "Malaysia", "🇲🇾", "+60 12-345 6789"),
        CountryInfo("SG", "+65", "Singapore", "🇸🇬", "+65 9123 4567"),
        CountryInfo("PK", "+92", "Pakistan", "🇵🇰", "+92 300 1234567"),
        CountryInfo("DE", "+49", "Germany", "🇩🇪", "+49 151 1234567"),
        CountryInfo("FR", "+33", "France", "🇫🇷", "+33 6 12 34 56 78"),
        CountryInfo("IT", "+39", "Italy", "🇮🇹", "+39 312 345 6789"),
        CountryInfo("ES", "+34", "Spain", "🇪🇸", "+34 612 34 56 78"),
        CountryInfo("AU", "+61", "Australia", "🇦🇺", "+61 412 345 678"),
        CountryInfo("JP", "+81", "Japan", "🇯🇵", "+81 90 1234 5678"),
        CountryInfo("KR", "+82", "South Korea", "🇰🇷", "+82 10-1234-5678"),
        CountryInfo("CN", "+86", "China", "🇨🇳", "+86 138 0000 0000"),
        CountryInfo("ID", "+62", "Indonesia", "🇮🇩", "+62 812-3456-7890"),
        CountryInfo("PH", "+63", "Philippines", "🇵🇭", "+63 917 123 4567"),
        CountryInfo("TH", "+66", "Thailand", "🇹🇭", "+66 81 234 5678"),
        CountryInfo("VN", "+84", "Vietnam", "🇻🇳", "+84 91 234 56 78"),
        CountryInfo("EG", "+20", "Egypt", "🇪🇬", "+20 10 1234 5678"),
        CountryInfo("ZA", "+27", "South Africa", "🇿🇦", "+27 82 123 4567"),
        CountryInfo("NG", "+234", "Nigeria", "🇳🇬", "+234 803 123 4567"),
        CountryInfo("BR", "+55", "Brazil", "🇧🇷", "+55 11 91234-5678"),
        CountryInfo("MX", "+52", "Mexico", "🇲🇽", "+52 55 1234 5678"),
        CountryInfo("IR", "+98", "Iran", "🇮🇷", "+98 912 345 6789"),
        CountryInfo("IQ", "+964", "Iraq", "🇮🇶", "+964 770 123 4567"),
        CountryInfo("JO", "+962", "Jordan", "🇯🇴", "+962 7 9123 4567"),
        CountryInfo("LB", "+961", "Lebanon", "🇱🇧", "+961 70 123 456"),
        CountryInfo("GE", "+995", "Georgia", "🇬🇪", "+995 555 12 34 56"),
        CountryInfo("UZ", "+998", "Uzbekistan", "🇺🇿", "+998 90 123 45 67"),
        CountryInfo("KZ", "+7", "Kazakhstan", "🇰🇿", "+7 701 123 4567"),
        CountryInfo("UA", "+380", "Ukraine", "🇺🇦", "+380 50 123 4567"),
        CountryInfo("PL", "+48", "Poland", "🇵🇱", "+48 512 345 678"),
        CountryInfo("NL", "+31", "Netherlands", "🇳🇱", "+31 6 12345678"),
        CountryInfo("SE", "+46", "Sweden", "🇸🇪", "+46 70 123 45 67"),
        CountryInfo("CH", "+41", "Switzerland", "🇨🇭", "+41 79 123 45 67")
    )

    fun defaultCountry(): CountryInfo = countries.first { it.iso2 == "BD" }

    fun findCountryByIso(iso2: String): CountryInfo? {
        return countries.find { it.iso2.equals(iso2, ignoreCase = true) }
    }

    fun findCountryByDialCode(dialCode: String): CountryInfo? {
        val clean = dialCode.trim().let { if (!it.startsWith("+")) "+$it" else it }
        return countries.find { it.dialCode == clean }
    }

    /**
     * Detects country from phone string.
     * E.g. "+374 95..." matches Armenia (+374)
     * "+968 78..." matches Oman (+968)
     * "+880 16..." matches Bangladesh (+880)
     */
    fun detectCountry(phone: String): CountryInfo? {
        val trimmed = phone.trim()
        if (trimmed.startsWith("+")) {
            // Sort by longest dialCode first to match +880 before +8 or +968 before +9
            val matched = countries
                .sortedByDescending { it.dialCode.length }
                .firstOrNull { trimmed.startsWith(it.dialCode) }
            if (matched != null) return matched
            try {
                val parsed = phoneUtil.parse(trimmed, null)
                val region = phoneUtil.getRegionCodeForNumber(parsed)
                if (!region.isNullOrBlank()) {
                    return findCountryByIso(region)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Formats number dynamically as the user types, using libphonenumber AsYouTypeFormatter.
     */
    fun formatAsYouType(raw: String, countryIso: String): String {
        if (raw.isBlank()) return ""
        return try {
            val formatter = phoneUtil.getAsYouTypeFormatter(countryIso.uppercase())
            formatter.clear()
            var formatted = ""
            for (char in raw) {
                if (char.isDigit() || char == '+') {
                    formatted = formatter.inputDigit(char)
                }
            }
            formatted.ifBlank { raw }
        } catch (_: Exception) {
            raw
        }
    }

    /**
     * Extracts ONLY the national/local digits/formatting from a phone string,
     * stripping out the country's dial code if present.
     * e.g. "+374 95 110691" with Armenia (+374) -> "95 110691"
     * "+968 7812 3052" with Oman (+968) -> "7812 3052"
     * "+880 1642-581346" with Bangladesh (+880) -> "1642-581346"
     */
    fun extractNationalPart(phone: String, country: CountryInfo): String {
        var clean = phone.trim()
        if (clean.startsWith("+")) {
            if (clean.startsWith(country.dialCode)) {
                clean = clean.removePrefix(country.dialCode).trim()
            } else {
                val matched = detectCountry(clean)
                if (matched != null && clean.startsWith(matched.dialCode)) {
                    clean = clean.removePrefix(matched.dialCode).trim()
                } else {
                    clean = clean.removePrefix("+").trim()
                }
            }
        } else {
            val dialDigits = country.dialCode.removePrefix("+")
            if (clean.startsWith(dialDigits)) {
                clean = clean.removePrefix(dialDigits).trim()
            }
        }
        return formatNationalAsYouType(clean, country)
    }

    /**
     * Formats national digits as-you-type based on the country:
     * - Armenia (+374): e.g. "95 110691"
     * - Oman (+968): e.g. "7812 3052"
     * - Bangladesh (+880): e.g. "1642-581346" or "01642-581346"
     * - US/Canada (+1): e.g. "(555) 123-4567"
     * - UAE / Saudi (+971/+966): e.g. "50 123 4567"
     */
    fun formatNationalAsYouType(input: String, country: CountryInfo): String {
        val digitsOnly = input.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return ""

        return when (country.iso2) {
            "AM" -> {
                val digits = digitsOnly
                when {
                    digits.length <= 2 -> digits
                    digits.length <= 5 -> "${digits.substring(0, 2)} ${digits.substring(2)}"
                    digits.length <= 8 -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5)}"
                    else -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"
                }
            }
            "OM" -> {
                val digits = digitsOnly
                when {
                    digits.length <= 4 -> digits
                    digits.length <= 8 -> "${digits.substring(0, 4)} ${digits.substring(4)}"
                    else -> "${digits.substring(0, 4)} ${digits.substring(4, 8)} ${digits.substring(8)}"
                }
            }
            "BD" -> {
                val digits = digitsOnly
                if (digits.startsWith("0")) {
                    when {
                        digits.length <= 5 -> digits
                        digits.length <= 11 -> "${digits.substring(0, 5)}-${digits.substring(5)}"
                        else -> "${digits.substring(0, 5)}-${digits.substring(5, 11)} ${digits.substring(11)}"
                    }
                } else {
                    when {
                        digits.length <= 4 -> digits
                        digits.length <= 10 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
                        else -> "${digits.substring(0, 4)}-${digits.substring(4, 10)} ${digits.substring(10)}"
                    }
                }
            }
            "US", "CA" -> {
                val digits = digitsOnly
                when {
                    digits.length <= 3 -> if (digits.length == 3) "($digits) " else digits
                    digits.length <= 6 -> "(${digits.substring(0, 3)}) ${digits.substring(3)}"
                    digits.length <= 10 -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
                    else -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 10)} ${digits.substring(10)}"
                }
            }
            "AE", "SA" -> {
                val digits = digitsOnly
                when {
                    digits.length <= 2 -> digits
                    digits.length <= 5 -> "${digits.substring(0, 2)} ${digits.substring(2)}"
                    digits.length <= 9 -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5)}"
                    else -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5, 9)} ${digits.substring(9)}"
                }
            }
            "IN" -> {
                val digits = digitsOnly
                when {
                    digits.length <= 5 -> digits
                    digits.length <= 10 -> "${digits.substring(0, 5)} ${digits.substring(5)}"
                    else -> "${digits.substring(0, 5)} ${digits.substring(5, 10)} ${digits.substring(10)}"
                }
            }
            else -> {
                val digits = digitsOnly
                when {
                    digits.length <= 3 -> digits
                    digits.length <= 7 -> "${digits.substring(0, 3)} ${digits.substring(3)}"
                    digits.length <= 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
                    else -> digits
                }
            }
        }
    }

    /**
     * Combines country and national number into a clean full international string for saving or intent launching.
     * e.g. Armenia (+374) + "95 110691" -> "+374 95 110691"
     */
    fun combineToInternational(country: CountryInfo, nationalNumber: String): String {
        val clean = nationalNumber.trim()
        if (clean.isBlank()) return ""
        if (clean.startsWith("+")) return clean

        val dialDigits = country.dialCode.removePrefix("+")
        var stripped = if (clean.startsWith(dialDigits)) {
            clean.removePrefix(dialDigits).trim()
        } else clean

        if (country.iso2 == "BD" && stripped.startsWith("0")) {
            stripped = stripped.removePrefix("0")
        }

        val formatted = formatNationalAsYouType(stripped, country)
        return "${country.dialCode} $formatted"
    }

    /**
     * Cleanly formats a phone number for display into international standard with country spacing.
     * e.g. "+37495110691" -> "+374 95 110691"
     * "01642581346" (with BD default) -> "+880 1642-581346"
     */
    fun formatDisplay(raw: String, defaultCountryIso: String = "BD"): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""

        return try {
            val proto: ProtoPhoneNumber = if (clean.startsWith("+")) {
                phoneUtil.parse(clean, null)
            } else {
                phoneUtil.parse(clean, defaultCountryIso.uppercase())
            }
            phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (_: NumberParseException) {
            manualFormatFallback(clean)
        } catch (_: Exception) {
            clean
        }
    }

    /**
     * Normalizes to clean E.164 string (e.g. "+8801642581346") for saving or WhatsApp/Telegram launching.
     */
    fun normalizeToE164(raw: String, defaultDialCode: String = "+880"): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""

        return try {
            val proto = if (clean.startsWith("+")) {
                phoneUtil.parse(clean, null)
            } else {
                val iso = findCountryByDialCode(defaultDialCode)?.iso2 ?: "BD"
                phoneUtil.parse(clean, iso)
            }
            phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (_: Exception) {
            val digits = clean.filter { it.isDigit() }
            if (clean.startsWith("+")) "+$digits"
            else {
                val dialDigits = defaultDialCode.filter { it.isDigit() }
                val localWithoutZero = digits.trimStart('0')
                "+$dialDigits$localWithoutZero"
            }
        }
    }

    /**
     * Extracts digits only without '+' for WhatsApp API and Telegram intents.
     */
    fun cleanDigitsOnly(raw: String, defaultDialCode: String = "+880"): String {
        val e164 = normalizeToE164(raw, defaultDialCode)
        return e164.filter { it.isDigit() }
    }

    private fun manualFormatFallback(clean: String): String {
        // Simple graceful fallback if number is incomplete or partial
        val country = detectCountry(clean)
        if (country != null) {
            val localPart = clean.removePrefix(country.dialCode).trim()
            if (localPart.length >= 8) {
                // e.g. 7812 3052 or 95 110691
                return "${country.dialCode} $localPart"
            }
        }
        return clean
    }
}
