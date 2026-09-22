package com.example.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    STOCK_KEEPER,
}

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarEmoji: String,
    val role: UserRole,
    val pinSalt: String,
    val pinHash: String,
    val xp: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val level: Int get() = (xp / XP_PER_LEVEL) + 1
    val xpIntoLevel: Int get() = xp % XP_PER_LEVEL
    val xpForNextLevel: Int get() = XP_PER_LEVEL

    companion object {
        const val XP_PER_LEVEL = 100
        const val XP_PER_RESTOCK = 10

        val AVATAR_CHOICES = listOf("🦁", "🐘", "🦓", "🦜", "🐆", "🦒", "🐍", "🦉", "🐊", "🦚", "🌾", "🛖")
    }
}
