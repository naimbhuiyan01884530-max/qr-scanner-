package com.example.domain.model

enum class ScanCategory {
    URL,
    WIFI,
    CONTACT,
    PRODUCT,
    PHONE,
    EMAIL,
    GEO,
    TEXT
}

enum class ScanFormatFilter {
    AUTO,
    QR_ONLY,
    BARCODE_ONLY
}

sealed interface ParsedPayload {
    val category: ScanCategory
    val displayTitle: String
    val primaryCopyValue: String

    data class UrlPayload(
        val url: String,
        val host: String,
        val isHttps: Boolean
    ) : ParsedPayload {
        override val category = ScanCategory.URL
        override val displayTitle = host.ifBlank { url }
        override val primaryCopyValue = url
    }

    data class WifiPayload(
        val ssid: String,
        val password: String,
        val securityType: String,
        val isHidden: Boolean
    ) : ParsedPayload {
        override val category = ScanCategory.WIFI
        override val displayTitle = ssid.ifBlank { "Wi-Fi Network" }
        override val primaryCopyValue = password.ifBlank { ssid }
    }

    data class ContactPayload(
        val name: String,
        val phone: String,
        val email: String,
        val organization: String,
        val address: String
    ) : ParsedPayload {
        override val category = ScanCategory.CONTACT
        override val displayTitle = name.ifBlank { phone.ifBlank { email.ifBlank { "vCard Contact" } } }
        override val primaryCopyValue = phone.ifBlank { email.ifBlank { name } }
    }

    data class ProductPayload(
        val code: String,
        val formatLabel: String
    ) : ParsedPayload {
        override val category = ScanCategory.PRODUCT
        override val displayTitle = code
        override val primaryCopyValue = code
    }

    data class PhonePayload(
        val number: String
    ) : ParsedPayload {
        override val category = ScanCategory.PHONE
        override val displayTitle = number
        override val primaryCopyValue = number
    }

    data class EmailPayload(
        val address: String,
        val subject: String,
        val body: String
    ) : ParsedPayload {
        override val category = ScanCategory.EMAIL
        override val displayTitle = address
        override val primaryCopyValue = address
    }

    data class GeoPayload(
        val latitude: String,
        val longitude: String,
        val query: String
    ) : ParsedPayload {
        override val category = ScanCategory.GEO
        override val displayTitle = "$latitude, $longitude"
        override val primaryCopyValue = "$latitude,$longitude"
    }

    data class TextPayload(
        val text: String
    ) : ParsedPayload {
        override val category = ScanCategory.TEXT
        override val displayTitle = text.lineSequence().firstOrNull()?.take(60) ?: text
        override val primaryCopyValue = text
    }
}

enum class GeneratorType {
    URL,
    TEXT,
    WIFI,
    VCARD,
    BARCODE_128
}

enum class GeneratorPalette(val fgHex: Int, val bgHex: Int) {
    NEON_MINT(0xFF00F5B8.toInt(), 0xFF0B1218.toInt()),
    ELECTRIC_CYAN(0xFF00D1FF.toInt(), 0xFF0B1218.toInt()),
    CLASSIC_WHITE(0xFF0B1016.toInt(), 0xFFFFFFFF.toInt())
}

enum class GeneratorSize(val px: Int, val label: String) {
    SMALL(512, "512px"),
    MEDIUM(1024, "1024px"),
    HIGH(2048, "2048px")
}

enum class QrEccLevel(val code: String) {
    L("L (7%)"),
    M("M (15%)"),
    Q("Q (25%)"),
    H("H (30%)")
}
