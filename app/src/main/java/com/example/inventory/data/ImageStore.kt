package com.example.inventory.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies picked photos into app-private storage so they don't depend on a picker Uri's
 * permission lifecycle (photo-picker content Uris aren't reliably persistable across restarts).
 */
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    suspend fun importImage(source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val destination = File(imagesDir, "${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(source) ?: return@withContext null
            input.use { inStream -> destination.outputStream().use { inStream.copyTo(it) } }
            destination.absolutePath
        } catch (e: IOException) {
            null
        }
    }

    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        if (path != null) {
            File(path).takeIf { it.exists() }?.delete()
        }
        Unit
    }
}
