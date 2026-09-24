package com.example.inventory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun observeAll(): Flow<List<Supplier>>

    @Insert
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)
}
