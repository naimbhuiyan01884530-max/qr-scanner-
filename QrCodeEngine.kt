package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.SystemClock
import com.example.domain.model.QrEccLevel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ImageScanResult(
    val rawValue: String,
    val format: Int,
    val latencyMs: Long
)

object QrCodeEngine {

    fun isValidCode128Input(text: String): Boolean {
        if (text.isBlank()) return false
        return text.all { it.code in 32..126 }
    }

    fun generateBitmap(
        content: String,
        isCode128: Boolean,
        fgColor: Int,
        bgColor: Int,
        sizePx: Int,
        ecc: QrEccLevel = QrEccLevel.M
    ): Bitmap? {
        if (content.isBlank()) return null
        if (isCode128 && !isValidCode128Input(content)) return null

        return runCatching {
            val writer = MultiFormatWriter()
            if (isCode128) {
                val barHeight = (sizePx * 0.44f).toInt().coerceAtLeast(160)
                val totalHeight = (sizePx * 0.56f).toInt().coerceAtLeast(220)
                val hints = mapOf(EncodeHintType.MARGIN to 4)
                val matrix = writer.encode(content, BarcodeFormat.CODE_128, sizePx, barHeight, hints)
                val bitmap = Bitmap.createBitmap(sizePx, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(bgColor)

                val pixels = IntArray(sizePx * barHeight)
                for (y in 0 until barHeight) {
                    val offset = y * sizePx
                    for (x in 0 until sizePx) {
                        pixels[offset + x] = if (matrix.get(x, y)) fgColor else bgColor
                    }
                }
                val barsBitmap = Bitmap.createBitmap(pixels, sizePx, barHeight, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(barsBitmap, 0f, 12f, null)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = fgColor
                    textAlign = Paint.Align.CENTER
                    textSize = (totalHeight - barHeight) * 0.45f
                    typeface = Typeface.MONOSPACE
                }
                canvas.drawText(
                    content.take(40),
                    sizePx / 2f,
                    totalHeight - ((totalHeight - barHeight) * 0.30f),
                    textPaint
                )
                bitmap
            } else {
                val zxingEcc = when (ecc) {
                    QrEccLevel.L -> ErrorCorrectionLevel.L
                    QrEccLevel.M -> ErrorCorrectionLevel.M
                    QrEccLevel.Q -> ErrorCorrectionLevel.Q
                    QrEccLevel.H -> ErrorCorrectionLevel.H
                }
                val hints = mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to zxingEcc,
                    EncodeHintType.MARGIN to 2
                )
                val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
                matrixToBitmap(matrix, fgColor, bgColor)
            }
        }.getOrNull()
    }

    fun generateSvg(
        content: String,
        isCode128: Boolean,
        fgColor: Int,
        bgColor: Int,
        sizePx: Int,
        ecc: QrEccLevel = QrEccLevel.M
    ): String? {
        if (content.isBlank()) return null
        if (isCode128 && !isValidCode128Input(content)) return null

        return runCatching {
            val writer = MultiFormatWriter()
            val fgHex = String.format("#%06X", 0xFFFFFF and fgColor)
            val bgHex = String.format("#%06X", 0xFFFFFF and bgColor)

            if (isCode128) {
                val width = sizePx
                val barHeight = (sizePx * 0.44f).toInt().coerceAtLeast(160)
                val totalHeight = (sizePx * 0.56f).toInt().coerceAtLeast(220)
                val matrix = writer.encode(
                    content,
                    BarcodeFormat.CODE_128,
                    width,
                    barHeight,
                    mapOf(EncodeHintType.MARGIN to 4)
                )
                buildString {
                    append("""<?xml version="1.0" encoding="UTF-8"?>""")
                    append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $totalHeight" width="$width" height="$totalHeight">""")
                    append("""<rect width="100%" height="100%" fill="$bgHex"/>""")
                    var x = 0
                    while (x < matrix.width) {
                        if (matrix.get(x, 0)) {
                            var runWidth = 1
                            while (x + runWidth < matrix.width && matrix.get(x + runWidth, 0)) {
                                runWidth++
                            }
                            append("""<rect x="$x" y="12" width="$runWidth" height="$barHeight" fill="$fgHex"/>""")
                            x += runWidth
                        } else {
                            x++
                        }
                    }
                    val safeText = content
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    val fontSize = ((totalHeight - barHeight) * 0.45f).toInt()
                    val textY = totalHeight - ((totalHeight - barHeight) * 0.30f).toInt()
                    append("""<text x="${width / 2}" y="$textY" fill="$fgHex" font-family="monospace" font-size="$fontSize" text-anchor="middle">$safeText</text>""")
                    append("</svg>")
                }
            } else {
                val zxingEcc = when (ecc) {
                    QrEccLevel.L -> ErrorCorrectionLevel.L
                    QrEccLevel.M -> ErrorCorrectionLevel.M
                    QrEccLevel.Q -> ErrorCorrectionLevel.Q
                    QrEccLevel.H -> ErrorCorrectionLevel.H
                }
                val hints = mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to zxingEcc,
                    EncodeHintType.MARGIN to 2
                )
                // Encode at 1:1 module resolution for ultra-clean vector SVG
                val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
                val w = matrix.width
                val h = matrix.height
                buildString {
                    append("""<?xml version="1.0" encoding="UTF-8"?>""")
                    append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h" width="$sizePx" height="$sizePx" shape-rendering="crispEdges">""")
                    append("""<rect width="$w" height="$h" fill="$bgHex"/>""")
                    append("""<path fill="$fgHex" d="""")
                    for (y in 0 until h) {
                        var x = 0
                        while (x < w) {
                            if (matrix.get(x, y)) {
                                var run = 1
                                while (x + run < w && matrix.get(x + run, y)) {
                                    run++
                                }
                                append("M$x,$y h${run}v1h-${run}z")
                                x += run
                            } else {
                                x++
                            }
                        }
                    }
                    append(""""/>""")
                    append("</svg>")
                }
            }
        }.getOrNull()
    }

    private fun matrixToBitmap(matrix: BitMatrix, fgColor: Int, bgColor: Int): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) fgColor else bgColor
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    suspend fun scanImageUri(context: Context, uri: Uri): ImageScanResult? = withContext(Dispatchers.IO) {
        val startMs = SystemClock.elapsedRealtime()
        val inputImage = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            ?: return@withContext null

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        suspendCancellableCoroutine { cont ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val elapsed = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(0L)
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    scanner.close()
                    if (first != null) {
                        cont.resume(
                            ImageScanResult(
                                rawValue = first.rawValue.orEmpty(),
                                format = first.format,
                                latencyMs = elapsed
                            )
                        )
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    scanner.close()
                    cont.resume(null)
                }
        }
    }
}
