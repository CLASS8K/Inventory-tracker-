package com.example.inventory.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

const val DB_VERSION = 6

@Database(
    entities = [InventoryItem::class, AuditLogEntry::class, UserProfile::class],
    version = DB_VERSION,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao
}

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)
}
