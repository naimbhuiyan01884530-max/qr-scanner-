package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<ScanRecordEntity>

    @Query("SELECT * FROM scan_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScanRecordEntity): Long

    @Query("UPDATE scan_records SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE scan_records SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, bookmarked: Boolean)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun clearAll()
}
