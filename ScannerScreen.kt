package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.ScanFormatFilter
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonMint
import com.example.ui.theme.ThemeMode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun ScannerScreen(
    formatFilter: ScanFormatFilter,
    torchEnabled: Boolean,
    zoomLevel: Float,
    useFrontCamera: Boolean,
    themeMode: ThemeMode,
    isResultSheetOpen: Boolean,
    onFormatFilterChange: (ScanFormatFilter) -> Unit,
    onToggleTorch: () -> Unit,
    onSelectZoom: (Float) -> Unit,
    onToggleCameraLens: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onLiveCodeDetected: (String, Int, Long) -> Unit,
    onGalleryImagePicked: (android.net.Uri) -> Unit,
    onManualCodeSubmitted: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onGalleryImagePicked)
    }

    var showManualDialog by remember { mutableStateOf(false) }
    var detectedBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060A0E))
    ) {
        if (hasCameraPermission) {
            val cameraController = remember {
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                }
            }

            // Re-configure ML Kit scanner when formatFilter changes
            val barcodeScanner = remember(formatFilter) {
                val optionsBuilder = BarcodeScannerOptions.Builder()
                when (formatFilter) {
                    ScanFormatFilter.AUTO -> optionsBuilder.setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    ScanFormatFilter.QR_ONLY -> optionsBuilder.setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417
                    )
                    ScanFormatFilter.BARCODE_ONLY -> optionsBuilder.setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODABAR
                    )
                }
                BarcodeScanning.getClient(optionsBuilder.build())
            }

            LaunchedEffect(cameraController, lifecycleOwner) {
                runCatching { cameraController.bindToLifecycle(lifecycleOwner) }
            }

            LaunchedEffect(useFrontCamera) {
                cameraController.cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            }

            LaunchedEffect(torchEnabled) {
                runCatching { cameraController.enableTorch(torchEnabled) }
            }

            LaunchedEffect(zoomLevel) {
                val maxZoom = cameraController.zoomState.value?.maxZoomRatio ?: 4f
                runCatching { cameraController.setZoomRatio(zoomLevel.coerceAtMost(maxZoom)) }
            }

            DisposableEffect(barcodeScanner, isResultSheetOpen) {
                val executor = ContextCompat.getMainExecutor(context)
                val analyzer = MlKitAnalyzer(
                    listOf(barcodeScanner),
                    COORDINATE_SYSTEM_VIEW_REFERENCED,
                    executor
                ) { result ->
                    if (isResultSheetOpen) {
                        detectedBounds = null
                        return@MlKitAnalyzer
                    }
                    val startMark = SystemClock.elapsedRealtime()
                    val barcodes = result.getValue(barcodeScanner)
                    val first = barcodes?.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first != null) {
                        detectedBounds = first.boundingBox
                        val latency = (SystemClock.elapsedRealtime() - startMark).coerceAtLeast(0L)
                        onLiveCodeDetected(
                            first.rawValue.orEmpty(),
                            first.format,
                            latency
                        )
                    } else {
                        detectedBounds = null
                    }
                }
                cameraController.setImageAnalysisAnalyzer(executor, analyzer)
                onDispose {
                    cameraController.clearImageAnalysisAnalyzer()
                    barcodeScanner.close()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission-denied / emulator-friendly sleek dark fallback canvas
            CameraPermissionFallback(
                onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }

        // Animated Viewfinder Laser + Reticle Overlay (always visible when camera active)
        if (hasCameraPermission) {
            ViewfinderLaserOverlay(
                isWideBarcodeMode = formatFilter == ScanFormatFilter.BARCODE_ONLY,
                detectedBounds = detectedBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Bar: Offline Ready Indicator + Theme & Language Quick Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color(0xCC0D151D),
                shape = CircleShape,
                border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("offline_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingOfflineDot()
                    Column {
                        Text(
                            text = strings.offlineReadyBadge,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Text(
                            text = strings.zeroMsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonMint
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xCC0D151D),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleLanguage)
                        .testTag("lang_toggle_button")
                ) {
                    Text(
                        text = strings.toggleLangLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Surface(
                    color = Color(0xCC0D151D),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = strings.toggleThemeDesc,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Controls Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasCameraPermission) {
                Text(
                    text = strings.scannerGuidance,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            // Format Filter Chips (Auto / QR / Barcode)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatChip(
                    label = strings.filterAuto,
                    selected = formatFilter == ScanFormatFilter.AUTO,
                    onClick = { onFormatFilterChange(ScanFormatFilter.AUTO) },
                    tag = "filter_auto_chip"
                )
                FormatChip(
                    label = strings.filterQr,
                    selected = formatFilter == ScanFormatFilter.QR_ONLY,
                    onClick = { onFormatFilterChange(ScanFormatFilter.QR_ONLY) },
                    tag = "filter_qr_chip"
                )
                FormatChip(
                    label = strings.filterBarcode,
                    selected = formatFilter == ScanFormatFilter.BARCODE_ONLY,
                    onClick = { onFormatFilterChange(ScanFormatFilter.BARCODE_ONLY) },
                    tag = "filter_barcode_chip"
                )
            }

            // Quick Camera Controls: Torch + 1x/2x/4x Zoom + Switch Camera
            if (hasCameraPermission) {
                Surface(
                    color = Color(0xD90E1720),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onToggleTorch,
                            enabled = !useFrontCamera,
                            modifier = Modifier.testTag("torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = if (torchEnabled) strings.torchOn else strings.torchOff,
                                tint = if (torchEnabled) NeonMint else Color.White
                            )
                        }

                        listOf(1f to "1x", 2f to "2x", 4f to "4x").forEach { (ratio, label) ->
                            val selected = zoomLevel == ratio
                            Surface(
                                color = if (selected) NeonMint else Color.Transparent,
                                shape = CircleShape,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onSelectZoom(ratio) }
                                    .testTag("zoom_${label}_chip")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color(0xFF00261C) else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleCameraLens,
                            modifier = Modifier.testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = strings.switchCamera,
                                tint = ElectricCyan
                            )
                        }
                    }
                }
            }

            // Utility Row: Gallery Scan + Manual Code Entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xE613212E),
                        contentColor = NeonMint
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gallery_scan_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.galleryScan, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showManualDialog = true },
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xCC0D151D),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("manual_entry_button")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.manualEntry, maxLines = 1)
                }
            }
        }
    }

    if (showManualDialog) {
        ManualCodeEntryDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { input ->
                showManualDialog = false
                onManualCodeSubmitted(input)
            }
        )
    }
}

@Composable
private fun PulsingOfflineDot() {
    val transition = rememberInfiniteTransition(label = "offline_dot")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(EmeraldSuccess.copy(alpha = alpha))
    )
}

@Composable
private fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xCC0D151D),
            labelColor = Color.White.copy(alpha = 0.85f),
            selectedContainerColor = NeonMint,
            selectedLabelColor = Color(0xFF00261C)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.2f),
            selectedBorderColor = NeonMint
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun ViewfinderLaserOverlay(
    isWideBarcodeMode: Boolean,
    detectedBounds: Rect?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        val frameWidth = if (isWideBarcodeMode) {
            (size.width * 0.80f).coerceAtMost(360.dp.toPx())
        } else {
            (size.minDimension * 0.64f).coerceAtMost(280.dp.toPx())
        }
        val frameHeight = if (isWideBarcodeMode) {
            frameWidth * 0.54f
        } else {
            frameWidth
        }

        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f - 36.dp.toPx()
        val cornerRadius = 22.dp.toPx()

        // Dimmed backdrop
        drawRect(color = Color.Black.copy(alpha = 0.52f))

        // Cutout window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Subtle inner guide border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )

        // Glowing Mint -> Cyan Corner Brackets
        val bracketLen = 30.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val bracketBrush = Brush.linearGradient(
            colors = listOf(NeonMint, ElectricCyan),
            start = Offset(left, top),
            end = Offset(left + frameWidth, top + frameHeight)
        )

        val brackets = Path().apply {
            // Top-left
            moveTo(left, top + bracketLen)
            lineTo(left, top + cornerRadius)
            quadraticTo(left, top, left + cornerRadius, top)
            lineTo(left + bracketLen, top)

            // Top-right
            moveTo(left + frameWidth - bracketLen, top)
            lineTo(left + frameWidth - cornerRadius, top)
            quadraticTo(left + frameWidth, top, left + frameWidth, top + cornerRadius)
            lineTo(left + frameWidth, top + bracketLen)

            // Bottom-right
            moveTo(left + frameWidth, top + frameHeight - bracketLen)
            lineTo(left + frameWidth, top + frameHeight - cornerRadius)
            quadraticTo(left + frameWidth, top + frameHeight, left + frameWidth - cornerRadius, top + frameHeight)
            lineTo(left + frameWidth - bracketLen, top + frameHeight)

            // Bottom-left
            moveTo(left + bracketLen, top + frameHeight)
            lineTo(left + cornerRadius, top + frameHeight)
            quadraticTo(left, top + frameHeight, left, top + frameHeight - cornerRadius)
            lineTo(left, top + frameHeight - bracketLen)
        }
        drawPath(
            path = brackets,
            brush = bracketBrush,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Animated Sweeping Laser Line
        val laserPadding = 14.dp.toPx()
        val laserY = top + laserPadding + (frameHeight - laserPadding * 2f) * scanProgress
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonMint.copy(alpha = 0.28f),
                    Color.Transparent
                ),
                startY = laserY - 18.dp.toPx(),
                endY = laserY + 18.dp.toPx()
            ),
            topLeft = Offset(left + laserPadding, laserY - 18.dp.toPx()),
            size = Size(frameWidth - laserPadding * 2f, 36.dp.toPx())
        )
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, NeonMint, ElectricCyan, Color.Transparent)
            ),
            start = Offset(left + laserPadding, laserY),
            end = Offset(left + frameWidth - laserPadding, laserY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Optional real-time bounding box lock if detected
        detectedBounds?.let { rect ->
            drawRoundRect(
                color = NeonMint,
                topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                size = Size(rect.width().toFloat(), rect.height().toFloat()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        color = Color(0xFF101821),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.35f)),
        modifier = modifier.widthIn(max = 420.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonMint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = NeonMint,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = strings.cameraPermissionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.cameraPermissionDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9AB0C2),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMint,
                    contentColor = Color(0xFF00261C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_camera_button")
            ) {
                Text(strings.grantCameraBtn, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ManualCodeEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.manualDialogTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(strings.manualDialogHint) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_code_textfield")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("manual_code_confirm_button")
            ) {
                Text(strings.manualDialogConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
