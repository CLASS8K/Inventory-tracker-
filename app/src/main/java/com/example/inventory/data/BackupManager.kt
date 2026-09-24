package com.example.inventory.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportResult {
    data object Success : ImportResult
    data object InvalidFile : ImportResult
    data object ReadError : ImportResult
}

/** Local-file backup/restore for the on-device database — there is no server to sync to. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: InventoryDatabase,
) {
    private val dbFile: File get() = context.getDatabasePath(DB_NAME)
    private val requiredTables = setOf("inventory_items", "user_profiles", "audit_log", "suppliers")

    /** Merges the write-ahead log into the main database file, then copies it to [destination]. */
    suspend fun exportTo(destination: Uri) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(full)").close()
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Could not open destination for writing")
        output.use { out -> dbFile.inputStream().use { it.copyTo(out) } }
    }

    /**
     * Stages [source] to a temp file and validates it's a Nkhokwe database on the schema
     * version this build expects *before* touching the live database — a stale-version backup
     * would otherwise get destructively migrated (i.e. wiped) the moment Room reopened it, which
     * is worse than just refusing the restore. On success the app must restart to reload it.
     */
    suspend fun importFrom(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-staging.db")
        try {
            val input = context.contentResolver.openInputStream(source) ?: return@withContext ImportResult.ReadError
            input.use { inStream -> staging.outputStream().use { inStream.copyTo(it) } }

            if (!isValidBackup(staging)) return@withContext ImportResult.InvalidFile

            // Close Room's connection before swapping the file out from under it — otherwise a
            // write from an in-flight coroutine (e.g. a quick sell tap) can race the overwrite
            // and corrupt or partially lose the restore. No further queries can land after this;
            // the required app restart is what reopens the database on the restored file.
            database.close()
            staging.copyTo(dbFile, overwrite = true)
            // The restored file won't match any stale WAL/SHM sidecar; drop them so Room
            // reads the restored file as-is on the next (post-restart) open.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            ImportResult.Success
        } catch (e: java.io.IOException) {
            ImportResult.ReadError
        } finally {
            staging.delete()
        }
    }

    private fun isValidBackup(file: File): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            val version = db.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else -1
            }
            val tables = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            version == DB_VERSION && tables.containsAll(requiredTables)
        } catch (e: SQLiteException) {
            false
        } finally {
            db?.close()
        }
    }

    companion object {
        private const val DB_NAME = "inventory.db"
    }
}
