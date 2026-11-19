package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.domain.model.ParsedPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DeviceActions {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun triggerScanHaptic(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45L)
                }
            }
        }
    }

    fun openUrl(context: Context, url: String) {
        val normalized = if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
            url
        } else {
            "https://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun dialPhone(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun sendEmail(context: Context, email: ParsedPayload.EmailPayload) {
        val uri = Uri.Builder()
            .scheme("mailto")
            .opaquePart(email.address)
            .build()
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            if (email.subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, email.subject)
            if (email.body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, email.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun insertContact(context: Context, contact: ParsedPayload.ContactPayload) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            if (contact.name.isNotBlank()) putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
            if (contact.phone.isNotBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, contact.phone)
            if (contact.email.isNotBlank()) putExtra(ContactsContract.Intents.Insert.EMAIL, contact.email)
            if (contact.organization.isNotBlank()) putExtra(ContactsContract.Intents.Insert.COMPANY, contact.organization)
            if (contact.address.isNotBlank()) putExtra(ContactsContract.Intents.Insert.POSTAL, contact.address)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openGeoMap(context: Context, geo: ParsedPayload.GeoPayload) {
        val uri = if (geo.query.isNotBlank()) {
            Uri.parse("geo:${geo.latitude},${geo.longitude}?q=${Uri.encode(geo.query)}")
        } else {
            Uri.parse("geo:${geo.latitude},${geo.longitude}?q=${geo.latitude},${geo.longitude}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWifiConnect(context: Context, wifi: ParsedPayload.WifiPayload) {
        // Also copy password automatically for instant convenience
        if (wifi.password.isNotBlank()) {
            copyToClipboard(context, "Wi-Fi Password", wifi.password)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifi.ssid.isNotBlank()) {
            val suggestionBuilder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
            when {
                wifi.securityType.contains("WPA", ignoreCase = true) && wifi.password.isNotBlank() ->
                    suggestionBuilder.setWpa2Passphrase(wifi.password)
                wifi.securityType.contains("SAE", ignoreCase = true) && wifi.password.isNotBlank() ->
                    suggestionBuilder.setWpa3Passphrase(wifi.password)
            }
            val suggestions = ArrayList<WifiNetworkSuggestion>().apply {
                add(suggestionBuilder.build())
            }
            val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, suggestions)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }

        val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        runCatching { context.startActivity(fallback) }
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    suspend fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "$fileName.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrNull()
        } ?: return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/QRLens"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            }.getOrDefault(false)
        }

    suspend fun readTextFromUri(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }

    fun printBitmap(context: Context, bitmap: Bitmap, jobName: String) {
        runCatching {
            val helper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            helper.printBitmap(jobName, bitmap)
        }
    }
}
