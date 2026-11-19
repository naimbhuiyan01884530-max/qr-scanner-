package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.ScanRecordEntity
import com.example.data.local.ScanRepository
import com.example.data.local.UserPreferences
import com.example.domain.model.GeneratorPalette
import com.example.domain.model.GeneratorSize
import com.example.domain.model.GeneratorType
import com.example.domain.model.QrEccLevel
import com.example.domain.model.ScanCategory
import com.example.domain.model.ScanFormatFilter
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.utils.DeviceActions
import com.example.utils.QrCodeEngine
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HistoryFilter {
    ALL, BOOKMARKS, URL, WIFI, CONTACT, PRODUCT, TEXT
}

data class GeneratorUiState(
    val type: GeneratorType = GeneratorType.URL,
    val urlInput: String = "https://",
    val textInput: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val vcardName: String = "",
    val vcardPhone: String = "",
    val vcardEmail: String = "",
    val vcardOrg: String = "",
    val barcode128Input: String = "",
    val palette: GeneratorPalette = GeneratorPalette.NEON_MINT,
    val size: GeneratorSize = GeneratorSize.MEDIUM,
    val ecc: QrEccLevel = QrEccLevel.M,
    val encodedPayload: String = "",
    val previewBitmap: Bitmap? = null,
    val isInvalidBarcode128: Boolean = false
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data object NoCodeInGalleryImage : UiEvent
    data object SavedToVault : UiEvent
    data object ExportSuccess : UiEvent
    data object ImportSuccess : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ScanRepository(database.scanRecordDao())
    private val preferencesManager = PreferencesManager(application)

    val preferences: StateFlow<UserPreferences> = preferencesManager.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val allRecords: StateFlow<List<ScanRecordEntity>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val filteredHistory: StateFlow<List<ScanRecordEntity>> = combine(
        allRecords,
        _historySearchQuery,
        _historyFilter
    ) { records, query, filter ->
        records.filter { record ->
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.BOOKMARKS -> record.isBookmarked
                HistoryFilter.URL -> record.category == ScanCategory.URL.name
                HistoryFilter.WIFI -> record.category == ScanCategory.WIFI.name
                HistoryFilter.CONTACT -> record.category == ScanCategory.CONTACT.name
                HistoryFilter.PRODUCT -> record.category == ScanCategory.PRODUCT.name
                HistoryFilter.TEXT -> record.category in setOf(
                    ScanCategory.TEXT.name,
                    ScanCategory.PHONE.name,
                    ScanCategory.EMAIL.name,
                    ScanCategory.GEO.name
                )
            }
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                record.rawValue.contains(q, ignoreCase = true) ||
                record.displayTitle.contains(q, ignoreCase = true) ||
                record.note.contains(q, ignoreCase = true) ||
                record.formatName.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeResult = MutableStateFlow<ScanRecordEntity?>(null)
    val activeResult: StateFlow<ScanRecordEntity?> = _activeResult.asStateFlow()

    private val _formatFilter = MutableStateFlow(ScanFormatFilter.AUTO)
    val formatFilter: StateFlow<ScanFormatFilter> = _formatFilter.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _generatorState = MutableStateFlow(GeneratorUiState())
    val generatorState: StateFlow<GeneratorUiState> = _generatorState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var lastDetectedValue: String? = null
    private var lastDetectedTimestamp: Long = 0L

    fun toggleThemeMode() {
        viewModelScope.launch {
            val next = when (preferences.value.themeMode) {
                ThemeMode.DARK -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.SYSTEM -> ThemeMode.DARK
            }
            preferencesManager.setThemeMode(next)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (preferences.value.language == AppLanguage.BN) {
                AppLanguage.EN
            } else {
                AppLanguage.BN
            }
            preferencesManager.setLanguage(next)
        }
    }

    fun setFormatFilter(filter: ScanFormatFilter) {
        _formatFilter.value = filter
    }

    fun toggleTorch() {
        _torchEnabled.update { !it }
    }

    fun setZoomLevel(zoom: Float) {
        _zoomLevel.value = zoom
    }

    fun toggleCameraLens() {
        _useFrontCamera.update { !it }
        if (_useFrontCamera.value) {
            _torchEnabled.value = false
        }
    }

    fun onLiveCodeDetected(rawValue: String, formatCode: Int, latencyMs: Long) {
        if (rawValue.isBlank()) return
        if (_activeResult.value != null) return

        val now = System.currentTimeMillis()
        if (rawValue == lastDetectedValue && now - lastDetectedTimestamp < 2200L) {
            return
        }
        lastDetectedValue = rawValue
        lastDetectedTimestamp = now

        if (preferences.value.vibrateOnScan) {
            DeviceActions.triggerScanHaptic(getApplication())
        }

        viewModelScope.launch {
            val saved = repository.saveScannedCode(rawValue, formatCode, latencyMs)
            _activeResult.value = saved
        }
    }

    fun onManualCodeEntered(rawValue: String) {
        val trimmed = rawValue.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val saved = repository.saveScannedCode(
                rawValue = trimmed,
                mlKitFormat = Barcode.FORMAT_QR_CODE,
                decodeLatencyMs = 0L
            )
            _activeResult.value = saved
        }
    }

    fun scanFromGalleryUri(uri: Uri) {
        viewModelScope.launch {
            val result = QrCodeEngine.scanImageUri(getApplication(), uri)
            if (result != null) {
                if (preferences.value.vibrateOnScan) {
                    DeviceActions.triggerScanHaptic(getApplication())
                }
                val saved = repository.saveScannedCode(
                    rawValue = result.rawValue,
                    mlKitFormat = result.format,
                    decodeLatencyMs = result.latencyMs
                )
                _activeResult.value = saved
            } else {
                _events.emit(UiEvent.NoCodeInGalleryImage)
            }
        }
    }

    fun openRecordDetail(record: ScanRecordEntity) {
        _activeResult.value = record
    }

    fun dismissActiveResult() {
        _activeResult.value = null
    }

    fun saveRecordNote(recordId: Long, note: String) {
        viewModelScope.launch {
            repository.updateNote(recordId, note)
            _activeResult.update { current ->
                if (current?.id == recordId) current.copy(note = note.trim()) else current
            }
        }
    }

    fun toggleBookmark(record: ScanRecordEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(record.id, record.isBookmarked)
            _activeResult.update { current ->
                if (current?.id == record.id) current.copy(isBookmarked = !record.isBookmarked) else current
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_activeResult.value?.id == id) {
                _activeResult.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _activeResult.value = null
        }
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun exportHistoryJsonToUri(uri: Uri) {
        viewModelScope.launch {
            val json = repository.exportToJson()
            val ok = DeviceActions.writeTextToUri(getApplication(), uri, json)
            if (ok) _events.emit(UiEvent.ExportSuccess)
        }
    }

    fun exportHistoryCsvToUri(uri: Uri) {
        viewModelScope.launch {
            val csv = repository.exportToCsv()
            val ok = DeviceActions.writeTextToUri(getApplication(), uri, csv)
            if (ok) _events.emit(UiEvent.ExportSuccess)
        }
    }

    fun importHistoryJsonFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = DeviceActions.readTextFromUri(getApplication(), uri) ?: return@launch
            val count = repository.importFromJson(text)
            if (count > 0) _events.emit(UiEvent.ImportSuccess)
        }
    }

    // --- Generator Actions ---

    fun updateGeneratorType(type: GeneratorType) {
        _generatorState.update { it.copy(type = type) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorUrl(value: String) {
        _generatorState.update { it.copy(urlInput = value) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorText(value: String) {
        _generatorState.update { it.copy(textInput = value) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorWifi(ssid: String? = null, password: String? = null, security: String? = null) {
        _generatorState.update {
            it.copy(
                wifiSsid = ssid ?: it.wifiSsid,
                wifiPassword = password ?: it.wifiPassword,
                wifiSecurity = security ?: it.wifiSecurity
            )
        }
        refreshGeneratorPreview()
    }

    fun updateGeneratorVcard(
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        org: String? = null
    ) {
        _generatorState.update {
            it.copy(
                vcardName = name ?: it.vcardName,
                vcardPhone = phone ?: it.vcardPhone,
                vcardEmail = email ?: it.vcardEmail,
                vcardOrg = org ?: it.vcardOrg
            )
        }
        refreshGeneratorPreview()
    }

    fun updateGeneratorBarcode128(value: String) {
        _generatorState.update { it.copy(barcode128Input = value) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorPalette(palette: GeneratorPalette) {
        _generatorState.update { it.copy(palette = palette) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorSize(size: GeneratorSize) {
        _generatorState.update { it.copy(size = size) }
        refreshGeneratorPreview()
    }

    fun updateGeneratorEcc(ecc: QrEccLevel) {
        _generatorState.update { it.copy(ecc = ecc) }
        refreshGeneratorPreview()
    }

    fun saveGeneratedToVault() {
        val state = _generatorState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.isInvalidBarcode128) return
        val format = if (state.type == GeneratorType.BARCODE_128) {
            Barcode.FORMAT_CODE_128
        } else {
            Barcode.FORMAT_QR_CODE
        }
        viewModelScope.launch {
            repository.saveScannedCode(
                rawValue = payload,
                mlKitFormat = format,
                decodeLatencyMs = 0L,
                isGenerated = true
            )
            _events.emit(UiEvent.SavedToVault)
        }
    }

    fun buildExportBitmapForGenerator(): Bitmap? {
        val state = _generatorState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.isInvalidBarcode128) return null
        return QrCodeEngine.generateBitmap(
            content = payload,
            isCode128 = state.type == GeneratorType.BARCODE_128,
            fgColor = state.palette.fgHex,
            bgColor = state.palette.bgHex,
            sizePx = state.size.px,
            ecc = state.ecc
        )
    }

    fun exportGeneratedSvgToUri(uri: Uri) {
        val state = _generatorState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.isInvalidBarcode128) return
        viewModelScope.launch {
            val svg = withContext(Dispatchers.Default) {
                QrCodeEngine.generateSvg(
                    content = payload,
                    isCode128 = state.type == GeneratorType.BARCODE_128,
                    fgColor = state.palette.fgHex,
                    bgColor = state.palette.bgHex,
                    sizePx = state.size.px,
                    ecc = state.ecc
                )
            } ?: return@launch
            val ok = DeviceActions.writeTextToUri(getApplication(), uri, svg)
            if (ok) _events.emit(UiEvent.ExportSuccess)
        }
    }

    private fun refreshGeneratorPreview() {
        val state = _generatorState.value
        val payload = buildGeneratorPayload(state)
        val isCode128 = state.type == GeneratorType.BARCODE_128
        val invalid128 = isCode128 && payload.isNotBlank() && !QrCodeEngine.isValidCode128Input(payload)

        if (payload.isBlank() || invalid128) {
            _generatorState.update {
                it.copy(
                    encodedPayload = payload,
                    previewBitmap = null,
                    isInvalidBarcode128 = invalid128
                )
            }
            return
        }

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QrCodeEngine.generateBitmap(
                    content = payload,
                    isCode128 = isCode128,
                    fgColor = state.palette.fgHex,
                    bgColor = state.palette.bgHex,
                    sizePx = 560,
                    ecc = state.ecc
                )
            }
            _generatorState.update {
                it.copy(
                    encodedPayload = payload,
                    previewBitmap = bitmap,
                    isInvalidBarcode128 = false
                )
            }
        }
    }

    private fun buildGeneratorPayload(state: GeneratorUiState): String = when (state.type) {
        GeneratorType.URL -> {
            val trimmed = state.urlInput.trim()
            if (trimmed == "https://" || trimmed == "http://") "" else trimmed
        }
        GeneratorType.TEXT -> state.textInput.trim()
        GeneratorType.WIFI -> {
            val ssid = state.wifiSsid.trim()
            if (ssid.isBlank()) "" else {
                val sec = state.wifiSecurity
                val pass = if (sec == "nopass") "" else state.wifiPassword
                "WIFI:T:$sec;S:${escapeWifi(ssid)};P:${escapeWifi(pass)};;"
            }
        }
        GeneratorType.VCARD -> {
            val name = state.vcardName.trim()
            val phone = state.vcardPhone.trim()
            val email = state.vcardEmail.trim()
            val org = state.vcardOrg.trim()
            if (name.isBlank() && phone.isBlank() && email.isBlank()) "" else {
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    if (name.isNotBlank()) appendLine("FN:$name")
                    if (phone.isNotBlank()) appendLine("TEL:$phone")
                    if (email.isNotBlank()) appendLine("EMAIL:$email")
                    if (org.isNotBlank()) appendLine("ORG:$org")
                    append("END:VCARD")
                }
            }
        }
        GeneratorType.BARCODE_128 -> state.barcode128Input.trim()
    }

    private fun escapeWifi(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:")
}
