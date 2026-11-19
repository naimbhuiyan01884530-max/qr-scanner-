package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawValue: String,
    val displayTitle: String,
    val category: String,
    val formatName: String,
    val formatCode: Int,
    val byteSize: Int,
    val errorCorrectionLevel: String,
    val decodeLatencyMs: Long,
    val note: String = "",
    val isBookmarked: Boolean = false,
    val isGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
