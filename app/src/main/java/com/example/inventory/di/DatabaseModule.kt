package com.example.inventory.di

import android.content.Context
import androidx.room.Room
import com.example.inventory.data.AuditLogDao
import com.example.inventory.data.InventoryDao
import com.example.inventory.data.InventoryDatabase
import com.example.inventory.data.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideInventoryDatabase(@ApplicationContext context: Context): InventoryDatabase =
        Room.databaseBuilder(context, InventoryDatabase::class.java, "inventory.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideInventoryDao(database: InventoryDatabase): InventoryDao = database.inventoryDao()

    @Provides
    fun provideAuditLogDao(database: InventoryDatabase): AuditLogDao = database.auditLogDao()

    @Provides
    fun provideUserDao(database: InventoryDatabase): UserDao = database.userDao()
}
