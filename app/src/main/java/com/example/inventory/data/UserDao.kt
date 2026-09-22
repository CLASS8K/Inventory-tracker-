package com.example.inventory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<UserProfile>>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM user_profiles WHERE role = 'ADMIN'")
    suspend fun adminCount(): Int

    @Insert
    suspend fun insert(user: UserProfile): Long

    @Update
    suspend fun update(user: UserProfile)

    @Delete
    suspend fun delete(user: UserProfile)
}
