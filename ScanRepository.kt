package com.example.data.local

import com.example.domain.model.BarcodePayloadParser
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanRepository(
    private val dao: ScanRecordDao
) {
    private val jsonFormat = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val allRecords: Flow<List<ScanRecordEntity>> = dao.observeAll()

    suspend fun saveScannedCode(
        rawValue: String,
        mlKitFormat: Int,
        decodeLatencyMs: Long,
        isGenerated: Boolean = false
    ): ScanRecordEntity {
        val parsed = BarcodePayloadParser.parse(rawValue, mlKitFormat)
        val formatName = BarcodePayloadParser.formatNameForCode(mlKitFormat)
        val byteSize = rawValue.toByteArray(Charsets.UTF_8).size
        val ecc = BarcodePayloadParser.estimateErrorCorrectionOrDetails(formatName, rawValue)

        val entity = ScanRecordEntity(
            rawValue = rawValue,
            displayTitle = parsed.displayTitle,
            category = parsed.category.name,
            formatName = formatName,
            formatCode = mlKitFormat,
            byteSize = byteSize,
            errorCorrectionLevel = ecc,
            decodeLatencyMs = decodeLatencyMs.coerceAtLeast(0L),
            isGenerated = isGenerated
        )
        val insertedId = dao.insert(entity)
        return entity.copy(id = insertedId)
    }

    suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id, note.trim())
    }

    suspend fun toggleBookmark(id: Long, currentlyBookmarked: Boolean) {
        dao.updateBookmark(id, !currentlyBookmarked)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun exportToJson(): String {
        val records = dao.getAllOnce()
        return jsonFormat.encodeToString(records)
    }

    suspend fun importFromJson(rawJson: String): Int {
        val decoded = runCatching {
            jsonFormat.decodeFromString<List<ScanRecordEntity>>(rawJson)
        }.getOrNull() ?: return 0

        var importedCount = 0
        for (record in decoded) {
            dao.insert(record.copy(id = 0L))
            importedCount++
        }
        return importedCount
    }

    suspend fun exportToCsv(): String {
        val records = dao.getAllOnce()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("id,category,format,displayTitle,rawValue,note,isBookmarked,isGenerated,byteSize,decodeLatencyMs,createdAt")
        for (item in records) {
            sb.append(item.id).append(',')
                .append(csvEscape(item.category)).append(',')
                .append(csvEscape(item.formatName)).append(',')
                .append(csvEscape(item.displayTitle)).append(',')
                .append(csvEscape(item.rawValue)).append(',')
                .append(csvEscape(item.note)).append(',')
                .append(item.isBookmarked).append(',')
                .append(item.isGenerated).append(',')
                .append(item.byteSize).append(',')
                .append(item.decodeLatencyMs).append(',')
                .append(csvEscape(dateFormat.format(Date(item.createdAt))))
                .appendLine()
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
