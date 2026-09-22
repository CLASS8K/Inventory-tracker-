package com.example.inventory.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Local-file backup/restore for the on-device database — there is no server to sync to. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: InventoryDatabase,
) {
    private val dbFile: File get() = context.getDatabasePath(DB_NAME)

    /** Merges the write-ahead log into the main database file, then copies it to [destination]. */
    suspend fun exportTo(destination: Uri) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(full)").close()
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Could not open destination for writing")
        output.use { out -> dbFile.inputStream().use { it.copyTo(out) } }
    }

    /** Replaces the local database with [source]. The app must restart afterwards. */
    suspend fun importFrom(source: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(source)
            ?: error("Could not open backup file for reading")
        input.use { inStream -> dbFile.outputStream().use { inStream.copyTo(it) } }
        // The restored file may not match a stale WAL/SHM sidecar; drop them so Room
        // reads the restored file as-is on the next (post-restart) open.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    companion object {
        private const val DB_NAME = "inventory.db"
    }
}
