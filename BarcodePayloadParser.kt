package com.example.domain.model

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

object BarcodePayloadParser {

    private val urlRegex = Regex(
        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?(\\?.*)?(#.*)?$",
        RegexOption.IGNORE_CASE
    )

    private val phoneRegex = Regex("^\\+?[0-9\\s\\-()]{6,20}$")

    fun formatNameForCode(mlKitFormat: Int): String = when (mlKitFormat) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF_417"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "CUSTOM_CODE"
    }

    fun is1DBarcodeFormat(mlKitFormat: Int): Boolean = mlKitFormat in setOf(
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_CODE_93,
        Barcode.FORMAT_CODABAR,
        Barcode.FORMAT_ITF
    )

    fun estimateErrorCorrectionOrDetails(formatName: String, rawValue: String): String {
        val bytes = rawValue.toByteArray(Charsets.UTF_8).size
        return when (formatName) {
            "QR_CODE" -> when {
                bytes <= 64 -> "Level M/Q • ISO-18004"
                bytes <= 256 -> "Level M • ISO-18004"
                else -> "Level L • High Density"
            }
            "EAN_13", "EAN_8", "UPC_A", "UPC_E" -> "Check Digit Verified (GS1)"
            "CODE_128" -> "Checksum Mod-103 • ASCII"
            "DATA_MATRIX" -> "ECC 200 • Reed-Solomon"
            "PDF_417" -> "Reed-Solomon ECC"
            "AZTEC" -> "Reed-Solomon Aztec"
            else -> "On-Device Verified"
        }
    }

    fun parse(rawValue: String, mlKitFormat: Int = Barcode.FORMAT_QR_CODE): ParsedPayload {
        val trimmed = rawValue.trim()
        val formatName = formatNameForCode(mlKitFormat)

        // 1. Wi-Fi QR
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed)
        }

        // 2. vCard or MeCard
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            return parseVCard(trimmed)
        }
        if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
            return parseMeCard(trimmed)
        }

        // 3. Phone
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.removePrefix("tel:").removePrefix("TEL:").trim()
            return ParsedPayload.PhonePayload(number)
        }

        // 4. Email
        if (trimmed.startsWith("mailto:", ignoreCase = true) ||
            trimmed.startsWith("MATMSG:", ignoreCase = true)
        ) {
            return parseEmail(trimmed)
        }

        // 5. Geo
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.substringAfter(":").substringBefore("?")
            val parts = coords.split(",")
            val lat = parts.getOrNull(0)?.trim().orEmpty()
            val lng = parts.getOrNull(1)?.trim().orEmpty()
            val query = trimmed.substringAfter("?q=", "").trim()
            return ParsedPayload.GeoPayload(lat, lng, query)
        }

        // 6. URL (explicit scheme or well-formed domain)
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
            val host = uri?.host.orEmpty()
            return ParsedPayload.UrlPayload(
                url = trimmed,
                host = host,
                isHttps = trimmed.startsWith("https://", ignoreCase = true)
            )
        }

        // 7. 1D Barcode (Product / SKU)
        if (is1DBarcodeFormat(mlKitFormat)) {
            return ParsedPayload.ProductPayload(code = trimmed, formatLabel = formatName)
        }

        // 8. Implicit URL (e.g. "github.com/user" or "www.example.com")
        if (!trimmed.contains(" ") && trimmed.contains(".") && urlRegex.matches(trimmed)) {
            val normalized = "https://$trimmed"
            val host = runCatching { Uri.parse(normalized).host.orEmpty() }.getOrDefault(trimmed)
            return ParsedPayload.UrlPayload(url = normalized, host = host, isHttps = true)
        }

        // 9. Plain numeric phone
        if (phoneRegex.matches(trimmed) && trimmed.count { it.isDigit() } >= 7) {
            return ParsedPayload.PhonePayload(trimmed)
        }

        return ParsedPayload.TextPayload(trimmed)
    }

    private fun parseWifi(raw: String): ParsedPayload.WifiPayload {
        val body = raw.removePrefix("WIFI:").removePrefix("wifi:")
        var ssid = ""
        var password = ""
        var security = "WPA"
        var hidden = false

        // Split on unescaped semicolons
        val tokens = splitUnescaped(body, ';')
        for (token in tokens) {
            when {
                token.startsWith("S:", ignoreCase = true) -> ssid = unescapeWifi(token.drop(2))
                token.startsWith("P:", ignoreCase = true) -> password = unescapeWifi(token.drop(2))
                token.startsWith("T:", ignoreCase = true) -> security = token.drop(2).ifBlank { "Open" }
                token.startsWith("H:", ignoreCase = true) -> hidden = token.drop(2).equals("true", ignoreCase = true)
            }
        }
        return ParsedPayload.WifiPayload(
            ssid = ssid,
            password = password,
            securityType = security.uppercase(),
            isHidden = hidden
        )
    }

    private fun parseVCard(raw: String): ParsedPayload.ContactPayload {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var address = ""
        for (line in raw.lines()) {
            val upper = line.uppercase()
            when {
                upper.startsWith("FN:") -> name = line.substringAfter(":").trim()
                upper.startsWith("N:") && name.isBlank() -> {
                    name = line.substringAfter(":").split(";").filter { it.isNotBlank() }.reversed().joinToString(" ")
                }
                upper.startsWith("TEL") -> if (phone.isBlank()) phone = line.substringAfter(":").trim()
                upper.startsWith("EMAIL") -> if (email.isBlank()) email = line.substringAfter(":").trim()
                upper.startsWith("ORG:") -> org = line.substringAfter(":").replace(";", ", ").trim()
                upper.startsWith("ADR") -> address = line.substringAfter(":").split(";").filter { it.isNotBlank() }.joinToString(", ")
            }
        }
        return ParsedPayload.ContactPayload(name, phone, email, org, address)
    }

    private fun parseMeCard(raw: String): ParsedPayload.ContactPayload {
        val body = raw.substringAfter(":")
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var address = ""
        for (token in splitUnescaped(body, ';')) {
            when {
                token.startsWith("N:", ignoreCase = true) -> name = token.drop(2).replace(",", " ").trim()
                token.startsWith("TEL:", ignoreCase = true) -> phone = token.drop(4).trim()
                token.startsWith("EMAIL:", ignoreCase = true) -> email = token.drop(6).trim()
                token.startsWith("ORG:", ignoreCase = true) -> org = token.drop(4).trim()
                token.startsWith("ADR:", ignoreCase = true) -> address = token.drop(4).trim()
            }
        }
        return ParsedPayload.ContactPayload(name, phone, email, org, address)
    }

    private fun parseEmail(raw: String): ParsedPayload.EmailPayload {
        if (raw.startsWith("MATMSG:", ignoreCase = true)) {
            var to = ""
            var sub = ""
            var body = ""
            for (token in splitUnescaped(raw.substringAfter(":"), ';')) {
                when {
                    token.startsWith("TO:", ignoreCase = true) -> to = token.drop(3).trim()
                    token.startsWith("SUB:", ignoreCase = true) -> sub = token.drop(4).trim()
                    token.startsWith("BODY:", ignoreCase = true) -> body = token.drop(5).trim()
                }
            }
            return ParsedPayload.EmailPayload(to, sub, body)
        }
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        val address = raw.substringAfter(":").substringBefore("?").trim()
        val subject = uri?.getQueryParameter("subject").orEmpty()
        val body = uri?.getQueryParameter("body").orEmpty()
        return ParsedPayload.EmailPayload(address, subject, body)
    }

    private fun splitUnescaped(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            if (escaped) {
                current.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == delimiter) {
                if (current.isNotEmpty()) result.add(current.toString())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun unescapeWifi(text: String): String =
        text.replace("\\;", ";")
            .replace("\\:", ":")
            .replace("\\\\", "\\")
}
