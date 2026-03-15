package com.rahees.quickscan.util

enum class ContentType {
    URL, WIFI, CONTACT, PHONE, EMAIL, SMS, GEO, TEXT
}

data class WifiConfig(
    val ssid: String,
    val password: String,
    val encryption: String
)

data class ContactInfo(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val organization: String = ""
)

fun detectContentType(content: String): ContentType {
    val trimmed = content.trim()
    return when {
        trimmed.startsWith("WIFI:", ignoreCase = true) -> ContentType.WIFI
        trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> ContentType.CONTACT
        trimmed.startsWith("MECARD:", ignoreCase = true) -> ContentType.CONTACT
        trimmed.startsWith("geo:", ignoreCase = true) -> ContentType.GEO
        trimmed.startsWith("tel:", ignoreCase = true) -> ContentType.PHONE
        trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true) -> ContentType.SMS
        trimmed.startsWith("mailto:", ignoreCase = true) -> ContentType.EMAIL
        trimmed.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE)) -> ContentType.URL
        trimmed.matches(Regex("^www\\..*", RegexOption.IGNORE_CASE)) -> ContentType.URL
        trimmed.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) -> ContentType.EMAIL
        trimmed.matches(Regex("^\\+?[0-9\\s\\-()]{7,15}$")) -> ContentType.PHONE
        else -> ContentType.TEXT
    }
}

fun parseWifiConfig(content: String): WifiConfig? {
    if (!content.startsWith("WIFI:", ignoreCase = true)) return null
    val data = content.substringAfter("WIFI:")
    val ssid = Regex("S:([^;]*);").find(data)?.groupValues?.getOrNull(1) ?: ""
    val password = Regex("P:([^;]*);").find(data)?.groupValues?.getOrNull(1) ?: ""
    val encryption = Regex("T:([^;]*);").find(data)?.groupValues?.getOrNull(1) ?: "WPA"
    return WifiConfig(ssid = ssid, password = password, encryption = encryption)
}

fun parseVCard(content: String): ContactInfo? {
    if (!content.startsWith("BEGIN:VCARD", ignoreCase = true)) return null
    val name = Regex("FN:(.+)", RegexOption.MULTILINE).find(content)?.groupValues?.getOrNull(1)?.trim()
        ?: Regex("N:([^;]*);?", RegexOption.MULTILINE).find(content)?.groupValues?.getOrNull(1)?.trim()
        ?: ""
    val phone = Regex("TEL[^:]*:(.+)", RegexOption.MULTILINE).find(content)?.groupValues?.getOrNull(1)?.trim() ?: ""
    val email = Regex("EMAIL[^:]*:(.+)", RegexOption.MULTILINE).find(content)?.groupValues?.getOrNull(1)?.trim() ?: ""
    val org = Regex("ORG:(.+)", RegexOption.MULTILINE).find(content)?.groupValues?.getOrNull(1)?.trim() ?: ""
    return ContactInfo(name = name, phone = phone, email = email, organization = org)
}
