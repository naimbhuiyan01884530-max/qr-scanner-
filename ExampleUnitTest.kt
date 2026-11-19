package com.example

import com.example.domain.model.BarcodePayloadParser
import com.example.domain.model.ParsedPayload
import com.example.domain.model.QrEccLevel
import com.example.utils.QrCodeEngine
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parseWifiQrPayload_extractsFieldsOffline() {
        val raw = "WIFI:T:WPA;S:HomeFiber\\;5G;P:secret123;H:true;;"
        val parsed = BarcodePayloadParser.parse(raw, Barcode.FORMAT_QR_CODE)
        assertTrue(parsed is ParsedPayload.WifiPayload)
        val wifi = parsed as ParsedPayload.WifiPayload
        assertEquals("HomeFiber;5G", wifi.ssid)
        assertEquals("secret123", wifi.password)
        assertEquals("WPA", wifi.securityType)
        assertTrue(wifi.isHidden)
    }

    @Test
    fun parseVCardPayload_extractsContactOffline() {
        val raw = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Rahim Uddin
            TEL:+8801711000000
            EMAIL:rahim@example.com
            ORG:Dhaka Studio
            END:VCARD
        """.trimIndent()
        val parsed = BarcodePayloadParser.parse(raw, Barcode.FORMAT_QR_CODE)
        assertTrue(parsed is ParsedPayload.ContactPayload)
        val contact = parsed as ParsedPayload.ContactPayload
        assertEquals("Rahim Uddin", contact.name)
        assertEquals("+8801711000000", contact.phone)
        assertEquals("rahim@example.com", contact.email)
        assertEquals("Dhaka Studio", contact.organization)
    }

    @Test
    fun parseProductBarcode_identifies1DFormat() {
        val parsed = BarcodePayloadParser.parse("8901058852429", Barcode.FORMAT_EAN_13)
        assertTrue(parsed is ParsedPayload.ProductPayload)
        val product = parsed as ParsedPayload.ProductPayload
        assertEquals("8901058852429", product.code)
        assertEquals("EAN_13", product.formatLabel)
    }

    @Test
    fun generateSvg_producesValidQrAndCode128Vector() {
        val qrSvg = QrCodeEngine.generateSvg(
            content = "https://example.org",
            isCode128 = false,
            fgColor = 0xFF00F5B8.toInt(),
            bgColor = 0xFF0B1218.toInt(),
            sizePx = 512,
            ecc = QrEccLevel.M
        )
        assertNotNull(qrSvg)
        assertTrue(qrSvg!!.contains("<svg"))
        assertTrue(qrSvg.contains("#00F5B8"))

        val code128Svg = QrCodeEngine.generateSvg(
            content = "INV-2026-884",
            isCode128 = true,
            fgColor = 0xFF0B1016.toInt(),
            bgColor = 0xFFFFFFFF.toInt(),
            sizePx = 512
        )
        assertNotNull(code128Svg)
        assertTrue(code128Svg!!.contains("<svg"))
        assertFalse(QrCodeEngine.isValidCode128Input("বাংলা"))
    }
}
