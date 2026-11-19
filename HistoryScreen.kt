package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.BarcodePayloadParser
import com.example.domain.model.ParsedPayload
import com.example.domain.model.ScanCategory
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemeMode
import com.example.utils.DeviceActions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<ScanRecordEntity>,
    searchQuery: String,
    selectedFilter: HistoryFilter,
    selectedDetailRecord: ScanRecordEntity?,
    isExpandedScreen: Boolean,
    themeMode: ThemeMode,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onSelectRecord: (ScanRecordEntity) -> Unit,
    onToggleBookmark: (ScanRecordEntity) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit,
    onExportJsonUri: (android.net.Uri) -> Unit,
    onExportCsvUri: (android.net.Uri) -> Unit,
    onImportJsonUri: (android.net.Uri) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(onExportJsonUri)
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let(onExportCsvUri)
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImportJsonUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.historyTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${records.size} • ${strings.offlineReadyBadge}",
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

                if (records.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("history_clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = strings.clearAll,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(strings.historySearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = strings.cancel)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 20.dp)
                .testTag("history_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(filter) },
                    label = { Text(historyFilterLabel(filter, strings)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}")
                )
            }
        }

        // Offline Backup Export Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    jsonExportLauncher.launch("qrlens_vault_${System.currentTimeMillis()}.json")
                },
                enabled = records.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_json_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.exportJson, maxLines = 1)
            }

            OutlinedButton(
                onClick = {
                    csvExportLauncher.launch("qrlens_vault_${System.currentTimeMillis()}.csv")
                },
                enabled = records.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_csv_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.exportCsv, maxLines = 1)
            }

            OutlinedButton(
                onClick = {
                    jsonImportLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                modifier = Modifier.testTag("import_json_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.importJson, maxLines = 1)
            }
        }

        // Main Content (List or Adaptive List-Detail on Tablet)
        if (records.isEmpty()) {
            EmptyVaultState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        } else if (isExpandedScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        HistoryRecordCard(
                            record = record,
                            isSelected = selectedDetailRecord?.id == record.id,
                            strings = strings,
                            onClick = { onSelectRecord(record) },
                            onCopy = {
                                val parsed = BarcodePayloadParser.parse(record.rawValue, record.formatCode)
                                DeviceActions.copyToClipboard(context, "QRLens", parsed.primaryCopyValue)
                                onShowMessage(strings.copiedToast)
                            },
                            onQuickReuse = {
                                val parsed = BarcodePayloadParser.parse(record.rawValue, record.formatCode)
                                triggerQuickReuse(context, parsed) {
                                    onShowMessage(strings.copiedToast)
                                }
                            },
                            onToggleBookmark = { onToggleBookmark(record) },
                            onDelete = { onDeleteRecord(record.id) }
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = 16.dp)
                ) {
                    if (selectedDetailRecord != null) {
                        ScanResultContent(
                            record = selectedDetailRecord,
                            onToggleBookmark = onToggleBookmark,
                            onSaveNote = onSaveNote,
                            onShowMessage = onShowMessage,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.selectItemPrompt,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        isSelected = false,
                        strings = strings,
                        onClick = { onSelectRecord(record) },
                        onCopy = {
                            val parsed = BarcodePayloadParser.parse(record.rawValue, record.formatCode)
                            DeviceActions.copyToClipboard(context, "QRLens", parsed.primaryCopyValue)
                            onShowMessage(strings.copiedToast)
                        },
                        onQuickReuse = {
                            val parsed = BarcodePayloadParser.parse(record.rawValue, record.formatCode)
                            triggerQuickReuse(context, parsed) {
                                onShowMessage(strings.copiedToast)
                            }
                        },
                        onToggleBookmark = { onToggleBookmark(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(strings.clearConfirmTitle) },
            text = { Text(strings.clearConfirmDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.clearConfirmYes)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun HistoryRecordCard(
    record: ScanRecordEntity,
    isSelected: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onQuickReuse: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit
) {
    val category = remember(record.category) {
        ScanCategory.entries.firstOrNull { it.name == record.category } ?: ScanCategory.TEXT
    }
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("history_card_${record.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = categoryLabel(category, strings),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = categoryLabel(category, strings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• ${record.formatName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = record.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.rawValue,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (record.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (record.isBookmarked) strings.bookmarkRemove else strings.bookmarkAdd,
                        tint = if (record.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormatter.format(Date(record.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = strings.copyAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (category in setOf(ScanCategory.URL, ScanCategory.WIFI, ScanCategory.PHONE)) {
                        IconButton(onClick = onQuickReuse) {
                            Icon(
                                imageVector = when (category) {
                                    ScanCategory.WIFI -> Icons.Default.Wifi
                                    ScanCategory.PHONE -> Icons.Default.Call
                                    else -> Icons.AutoMirrored.Filled.OpenInNew
                                },
                                contentDescription = strings.openLinkAction,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.deleteRecordDesc,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultState(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vault_empty_1795110776232),
            contentDescription = strings.emptyHistoryTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = strings.emptyHistoryTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.emptyHistoryDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
    }
}

private fun historyFilterLabel(filter: HistoryFilter, strings: AppStrings): String = when (filter) {
    HistoryFilter.ALL -> strings.catAll
    HistoryFilter.BOOKMARKS -> strings.catBookmarks
    HistoryFilter.URL -> strings.catUrl
    HistoryFilter.WIFI -> strings.catWifi
    HistoryFilter.CONTACT -> strings.catContact
    HistoryFilter.PRODUCT -> strings.catProduct
    HistoryFilter.TEXT -> strings.catText
}

private fun triggerQuickReuse(
    context: android.content.Context,
    parsed: ParsedPayload,
    onCopied: () -> Unit
) {
    when (parsed) {
        is ParsedPayload.UrlPayload -> DeviceActions.openUrl(context, parsed.url)
        is ParsedPayload.WifiPayload -> {
            DeviceActions.openWifiConnect(context, parsed)
            onCopied()
        }
        is ParsedPayload.PhonePayload -> DeviceActions.dialPhone(context, parsed.number)
        else -> {
            DeviceActions.copyToClipboard(context, "QRLens", parsed.primaryCopyValue)
            onCopied()
        }
    }
}
