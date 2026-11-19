package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.BarcodePayloadParser
import com.example.domain.model.ParsedPayload
import com.example.domain.model.ScanCategory
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.utils.DeviceActions
import com.example.utils.QrCodeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    record: ScanRecordEntity,
    onDismiss: () -> Unit,
    onToggleBookmark: (ScanRecordEntity) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        ScanResultContent(
            record = record,
            onToggleBookmark = onToggleBookmark,
            onSaveNote = onSaveNote,
            onShowMessage = onShowMessage,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultContent(
    record: ScanRecordEntity,
    onToggleBookmark: (ScanRecordEntity) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val parsed = remember(record.rawValue, record.formatCode) {
        BarcodePayloadParser.parse(record.rawValue, record.formatCode)
    }

    var noteText by remember(record.id, record.note) { mutableStateOf(record.note) }
    var showWifiPassword by remember { mutableStateOf(false) }

    val is1DBarcode = remember(record.formatCode) {
        BarcodePayloadParser.is1DBarcodeFormat(record.formatCode)
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val fgInt = MaterialTheme.colorScheme.onSurface.toArgb()
    val bgInt = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb()

    LaunchedEffect(record.rawValue, is1DBarcode, fgInt, bgInt) {
        previewBitmap = withContext(Dispatchers.Default) {
            QrCodeEngine.generateBitmap(
                content = record.rawValue,
                isCode128 = record.formatName == "CODE_128",
                fgColor = fgInt,
                bgColor = bgInt,
                sizePx = 400
            )
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Offline verification + Category + Bookmark
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = strings.decodedOfflineBadge,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.decodedOfflineBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(
                onClick = { onToggleBookmark(record) },
                modifier = Modifier.testTag("result_bookmark_button")
            ) {
                Icon(
                    imageVector = if (record.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (record.isBookmarked) strings.bookmarkRemove else strings.bookmarkAdd,
                    tint = if (record.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main Decoded Summary Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(parsed.category),
                        contentDescription = categoryLabel(parsed.category, strings),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryLabel(parsed.category, strings),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parsed.displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                previewBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = record.formatName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        // Structured Payload Details
        StructuredPayloadCard(
            parsed = parsed,
            rawValue = record.rawValue,
            showWifiPassword = showWifiPassword,
            onToggleWifiPassword = { showWifiPassword = !showWifiPassword },
            strings = strings
        )

        // Primary Quick Actions
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary Contextual Action (Open Link / Wi-Fi / Call / Contact / Lookup)
            PrimaryActionButton(
                parsed = parsed,
                strings = strings,
                onClick = {
                    when (parsed) {
                        is ParsedPayload.UrlPayload -> DeviceActions.openUrl(context, parsed.url)
                        is ParsedPayload.WifiPayload -> {
                            DeviceActions.openWifiConnect(context, parsed)
                            if (parsed.password.isNotBlank()) {
                                onShowMessage(strings.copiedToast)
                            }
                        }
                        is ParsedPayload.PhonePayload -> DeviceActions.dialPhone(context, parsed.number)
                        is ParsedPayload.EmailPayload -> DeviceActions.sendEmail(context, parsed)
                        is ParsedPayload.ContactPayload -> DeviceActions.insertContact(context, parsed)
                        is ParsedPayload.GeoPayload -> DeviceActions.openGeoMap(context, parsed)
                        is ParsedPayload.ProductPayload -> DeviceActions.openUrl(
                            context,
                            "https://www.google.com/search?q=${parsed.code}"
                        )
                        is ParsedPayload.TextPayload -> {
                            DeviceActions.copyToClipboard(context, "QRLens", parsed.text)
                            onShowMessage(strings.copiedToast)
                        }
                    }
                }
            )

            // One-tap Copy
            FilledTonalButton(
                onClick = {
                    DeviceActions.copyToClipboard(context, "QRLens", parsed.primaryCopyValue)
                    onShowMessage(strings.copiedToast)
                },
                modifier = Modifier.testTag("result_copy_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.copyAction)
            }

            // Share
            OutlinedButton(
                onClick = { DeviceActions.shareText(context, record.rawValue) },
                modifier = Modifier.testTag("result_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shareAction)
            }

            // Save QR Image
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val exportBmp = withContext(Dispatchers.Default) {
                            QrCodeEngine.generateBitmap(
                                content = record.rawValue,
                                isCode128 = record.formatName == "CODE_128",
                                fgColor = 0xFF0B1016.toInt(),
                                bgColor = 0xFFFFFFFF.toInt(),
                                sizePx = 1024
                            )
                        }
                        if (exportBmp != null) {
                            val ok = DeviceActions.saveBitmapToGallery(
                                context,
                                exportBmp,
                                "QRLens_${System.currentTimeMillis()}"
                            )
                            if (ok) onShowMessage(strings.savedToGalleryToast)
                        }
                    }
                },
                modifier = Modifier.testTag("result_save_image_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.saveQrAction)
            }
        }

        // Offline Personal Note
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.personalNoteLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(strings.personalNoteHint) },
                    singleLine = false,
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_note_input")
                )
                FilledTonalButton(
                    onClick = {
                        onSaveNote(record.id, noteText)
                        onShowMessage(strings.noteSavedToast)
                    },
                    modifier = Modifier.testTag("result_save_note_button")
                ) {
                    Text(strings.saveNoteBtn)
                }
            }
        }

        // Technical Parameters Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.techSpecsTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                TechSpecRow(strings.techFormat, record.formatName)
                TechSpecRow(strings.techSize, "${record.byteSize} bytes (${record.rawValue.length} chars)")
                TechSpecRow(strings.techErrorCorrection, record.errorCorrectionLevel)
                TechSpecRow(strings.techLatency, "${record.decodeLatencyMs} ms (Offline)")
            }
        }
    }
}

@Composable
private fun StructuredPayloadCard(
    parsed: ParsedPayload,
    rawValue: String,
    showWifiPassword: Boolean,
    onToggleWifiPassword: () -> Unit,
    strings: AppStrings
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (parsed) {
                is ParsedPayload.WifiPayload -> {
                    TechSpecRow(strings.wifiSsidLabel, parsed.ssid)
                    TechSpecRow(strings.wifiSecurityLabel, parsed.securityType)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.wifiPasswordLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (parsed.password.isBlank()) "—" else if (showWifiPassword) parsed.password else "••••••••",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (parsed.password.isNotBlank()) {
                            IconButton(onClick = onToggleWifiPassword) {
                                Icon(
                                    imageVector = if (showWifiPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = strings.wifiPasswordLabel
                                )
                            }
                        }
                    }
                }
                is ParsedPayload.ContactPayload -> {
                    if (parsed.name.isNotBlank()) TechSpecRow(strings.contactNameLabel, parsed.name)
                    if (parsed.phone.isNotBlank()) TechSpecRow(strings.contactPhoneLabel, parsed.phone)
                    if (parsed.email.isNotBlank()) TechSpecRow(strings.contactEmailLabel, parsed.email)
                    if (parsed.organization.isNotBlank()) TechSpecRow(strings.contactOrgLabel, parsed.organization)
                }
                else -> {
                    Text(
                        text = strings.rawPayloadLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rawValue,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    parsed: ParsedPayload,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val (label, icon) = when (parsed) {
        is ParsedPayload.UrlPayload -> strings.openLinkAction to Icons.AutoMirrored.Filled.OpenInNew
        is ParsedPayload.WifiPayload -> strings.connectWifiAction to Icons.Default.Wifi
        is ParsedPayload.PhonePayload -> strings.dialPhoneAction to Icons.Default.Call
        is ParsedPayload.EmailPayload -> strings.sendEmailAction to Icons.Default.Email
        is ParsedPayload.ContactPayload -> strings.addContactAction to Icons.Default.PersonAdd
        is ParsedPayload.GeoPayload -> strings.openMapAction to Icons.Default.LocationOn
        is ParsedPayload.ProductPayload -> strings.copyAction to Icons.Default.ContentCopy
        is ParsedPayload.TextPayload -> strings.copyAction to Icons.Default.ContentCopy
    }

    if (parsed !is ParsedPayload.TextPayload && parsed !is ParsedPayload.ProductPayload) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("result_primary_action_button")
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label)
        }
    }
}

@Composable
private fun TechSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun categoryIcon(category: ScanCategory): ImageVector = when (category) {
    ScanCategory.URL -> Icons.Default.Link
    ScanCategory.WIFI -> Icons.Default.Wifi
    ScanCategory.CONTACT -> Icons.Default.PersonAdd
    ScanCategory.PRODUCT -> Icons.Default.ShoppingBag
    ScanCategory.PHONE -> Icons.Default.Call
    ScanCategory.EMAIL -> Icons.Default.Email
    ScanCategory.GEO -> Icons.Default.LocationOn
    ScanCategory.TEXT -> Icons.Outlined.Description
}

fun categoryLabel(category: ScanCategory, strings: AppStrings): String = when (category) {
    ScanCategory.URL -> strings.catUrl
    ScanCategory.WIFI -> strings.catWifi
    ScanCategory.CONTACT -> strings.catContact
    ScanCategory.PRODUCT -> strings.catProduct
    ScanCategory.PHONE -> strings.catPhone
    ScanCategory.EMAIL -> strings.catEmail
    ScanCategory.GEO -> strings.catGeo
    ScanCategory.TEXT -> strings.catText
}
