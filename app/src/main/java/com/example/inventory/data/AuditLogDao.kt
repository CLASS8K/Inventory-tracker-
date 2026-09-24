package com.example.inventory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditLogEntry>>

    @Insert
    suspend fun insert(entry: AuditLogEntry): Long

    @Query("DELETE FROM audit_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
