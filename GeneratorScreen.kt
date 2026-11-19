package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.GeneratorPalette
import com.example.domain.model.GeneratorSize
import com.example.domain.model.GeneratorType
import com.example.domain.model.QrEccLevel
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.ThemeMode
import com.example.utils.DeviceActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(
    state: GeneratorUiState,
    themeMode: ThemeMode,
    onTypeChange: (GeneratorType) -> Unit,
    onUrlChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onWifiChange: (String?, String?, String?) -> Unit,
    onVcardChange: (String?, String?, String?, String?) -> Unit,
    onBarcode128Change: (String) -> Unit,
    onPaletteChange: (GeneratorPalette) -> Unit,
    onSizeChange: (GeneratorSize) -> Unit,
    onEccChange: (QrEccLevel) -> Unit,
    onBuildExportBitmap: () -> android.graphics.Bitmap?,
    onExportSvgUri: (android.net.Uri) -> Unit,
    onSaveToVault: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val svgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        uri?.let(onExportSvgUri)
    }

    val hasValidPreview = state.previewBitmap != null && !state.isInvalidBarcode128

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.generatorTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.offlineReadyBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleLanguage)
                ) {
                    Text(
                        text = strings.toggleLangLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = strings.toggleThemeDesc,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Format Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GeneratorType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(generatorTypeLabel(type, strings)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("gen_type_${type.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Form + Preview Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic Form Fields
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (state.type) {
                        GeneratorType.URL -> {
                            OutlinedTextField(
                                value = state.urlInput,
                                onValueChange = onUrlChange,
                                label = { Text(strings.genTypeUrl) },
                                placeholder = { Text(strings.genUrlHint) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_url_input")
                            )
                        }
                        GeneratorType.TEXT -> {
                            OutlinedTextField(
                                value = state.textInput,
                                onValueChange = onTextChange,
                                label = { Text(strings.genTypeText) },
                                placeholder = { Text(strings.genTextHint) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_text_input")
                            )
                        }
                        GeneratorType.WIFI -> {
                            OutlinedTextField(
                                value = state.wifiSsid,
                                onValueChange = { onWifiChange(it, null, null) },
                                label = { Text(strings.genWifiSsidHint) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_wifi_ssid_input")
                            )
                            OutlinedTextField(
                                value = state.wifiPassword,
                                onValueChange = { onWifiChange(null, it, null) },
                                label = { Text(strings.genWifiPassHint) },
                                enabled = state.wifiSecurity != "nopass",
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_wifi_pass_input")
                            )
                            Text(
                                text = strings.genWifiSecurity,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("WPA" to "WPA/WPA2", "WEP" to "WEP", "nopass" to "Open").forEach { (sec, label) ->
                                    FilterChip(
                                        selected = state.wifiSecurity == sec,
                                        onClick = { onWifiChange(null, null, sec) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                        GeneratorType.VCARD -> {
                            OutlinedTextField(
                                value = state.vcardName,
                                onValueChange = { onVcardChange(it, null, null, null) },
                                label = { Text(strings.genVcardName) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_vcard_name_input")
                            )
                            OutlinedTextField(
                                value = state.vcardPhone,
                                onValueChange = { onVcardChange(null, it, null, null) },
                                label = { Text(strings.genVcardPhone) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_vcard_phone_input")
                            )
                            OutlinedTextField(
                                value = state.vcardEmail,
                                onValueChange = { onVcardChange(null, null, it, null) },
                                label = { Text(strings.genVcardEmail) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.vcardOrg,
                                onValueChange = { onVcardChange(null, null, null, it) },
                                label = { Text(strings.genVcardOrg) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        GeneratorType.BARCODE_128 -> {
                            OutlinedTextField(
                                value = state.barcode128Input,
                                onValueChange = onBarcode128Change,
                                label = { Text(strings.genTypeBarcode128) },
                                placeholder = { Text(strings.genBarcode128Hint) },
                                isError = state.isInvalidBarcode128,
                                supportingText = if (state.isInvalidBarcode128) {
                                    { Text(strings.genInvalidBarcode128) }
                                } else null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gen_barcode128_input")
                            )
                        }
                    }
                }
            }

            // Real-Time Live Preview Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = state.previewBitmap
                    if (bmp != null && !state.isInvalidBarcode128) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = state.type.name,
                            modifier = Modifier
                                .then(
                                    if (state.type == GeneratorType.BARCODE_128) {
                                        Modifier
                                            .fillMaxWidth()
                                            .height(145.dp)
                                    } else {
                                        Modifier.size(220.dp)
                                    }
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("gen_live_preview_image")
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                modifier = Modifier.size(60.dp)
                            )
                            Text(
                                text = if (state.isInvalidBarcode128) strings.genInvalidBarcode128 else strings.genEmptyPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Palette Selector (Neon Mint, Cyan, Classic White)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.genPaletteTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GeneratorPalette.entries.forEach { palette ->
                        val selected = state.palette == palette
                        val swatchColor = when (palette) {
                            GeneratorPalette.NEON_MINT -> Color(0xFF00F5B8)
                            GeneratorPalette.ELECTRIC_CYAN -> Color(0xFF00D1FF)
                            GeneratorPalette.CLASSIC_WHITE -> Color(0xFFFFFFFF)
                        }
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onPaletteChange(palette) }
                                .testTag("gen_palette_${palette.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                )
                                Text(
                                    text = paletteLabel(palette, strings),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Resolution Size Selector & Error Correction (for QR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.genSizeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeneratorSize.entries.forEach { sz ->
                            FilterChip(
                                selected = state.size == sz,
                                onClick = { onSizeChange(sz) },
                                label = { Text(sz.label) }
                            )
                        }
                    }
                }
            }

            if (state.type != GeneratorType.BARCODE_128) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.genEccTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QrEccLevel.entries.forEach { ecc ->
                            FilterChip(
                                selected = state.ecc == ecc,
                                onClick = { onEccChange(ecc) },
                                label = { Text(ecc.name) }
                            )
                        }
                    }
                }
            }

            // Export & Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val bmp = withContext(Dispatchers.Default) { onBuildExportBitmap() }
                            if (bmp != null) {
                                val ok = DeviceActions.saveBitmapToGallery(
                                    context,
                                    bmp,
                                    "QRLens_Gen_${System.currentTimeMillis()}"
                                )
                                if (ok) onShowMessage(strings.savedToGalleryToast)
                            }
                        }
                    },
                    enabled = hasValidPreview,
                    modifier = Modifier.testTag("gen_save_png_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.genSavePng)
                }

                FilledTonalButton(
                    onClick = {
                        svgLauncher.launch("qrlens_code_${System.currentTimeMillis()}.svg")
                    },
                    enabled = hasValidPreview,
                    modifier = Modifier.testTag("gen_save_svg_button")
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.genSaveSvg)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val bmp = withContext(Dispatchers.Default) { onBuildExportBitmap() }
                            if (bmp != null) {
                                DeviceActions.shareBitmap(context, bmp, "qrlens_share_${System.currentTimeMillis()}")
                            }
                        }
                    },
                    enabled = hasValidPreview,
                    modifier = Modifier.testTag("gen_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.genShare)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val bmp = withContext(Dispatchers.Default) { onBuildExportBitmap() }
                            if (bmp != null) {
                                DeviceActions.printBitmap(context, bmp, strings.printJobTitle)
                            }
                        }
                    },
                    enabled = hasValidPreview,
                    modifier = Modifier.testTag("gen_print_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.genPrint)
                }

                OutlinedButton(
                    onClick = onSaveToVault,
                    enabled = hasValidPreview,
                    modifier = Modifier.testTag("gen_save_vault_button")
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.genSaveToVault)
                }
            }
        }
    }
}

private fun generatorTypeLabel(type: GeneratorType, strings: AppStrings): String = when (type) {
    GeneratorType.URL -> strings.genTypeUrl
    GeneratorType.TEXT -> strings.genTypeText
    GeneratorType.WIFI -> strings.genTypeWifi
    GeneratorType.VCARD -> strings.genTypeVcard
    GeneratorType.BARCODE_128 -> strings.genTypeBarcode128
}

private fun paletteLabel(palette: GeneratorPalette, strings: AppStrings): String = when (palette) {
    GeneratorPalette.NEON_MINT -> strings.paletteMint
    GeneratorPalette.ELECTRIC_CYAN -> strings.paletteCyan
    GeneratorPalette.CLASSIC_WHITE -> strings.paletteClassic
}
