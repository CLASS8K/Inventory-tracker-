package com.example.inventory.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlin.coroutines.resume

enum class LicenseStatus { ACTIVE, WARNING, READ_ONLY, OFFLINE_LOCKED, BLOCKED }

@Singleton
class LicenseChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * A per-device identifier for Firestore documents. Falls back to a locally generated, persisted
     * UUID when ANDROID_ID is unavailable or is the known-buggy value some Android builds return
     * (see AOSP issue 88083) — otherwise every such device would collide on one shared "unknown"
     * document, each overwriting the others' heartbeat and sharing a single block/unblock state.
     */
    val deviceId: String
        get() {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId != KNOWN_BAD_ANDROID_ID) return androidId
            return fallbackDeviceId()
        }

    private fun fallbackDeviceId(): String {
        val existing = prefs.getString(KEY_FALLBACK_DEVICE_ID, null)
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_FALLBACK_DEVICE_ID, generated) }
        return generated
    }

    /** The bar/club name an admin entered at setup — null until then. Self-reported, used to tell devices apart in Firebase. */
    fun businessName(): String? = prefs.getString(KEY_BUSINESS_NAME, null)

    fun setBusinessName(name: String) {
        prefs.edit { putString(KEY_BUSINESS_NAME, name) }
    }

    /**
     * Counts *other* devices already registered under the same name (case/whitespace-insensitive).
     * This is a heads-up for the admin, not a lock: several devices sharing one bar's name is normal
     * (one tablet per bartender), so a match here isn't necessarily a problem — just something to
     * glance at in Firebase if the count looks wrong. Returns null if the check couldn't reach Firestore.
     */
    suspend fun countOtherDevicesWithBusinessName(name: String): Int? {
        val normalized = normalizeBusinessName(name)
        if (normalized.isEmpty()) return 0
        return runCatching {
            suspendCancellableCoroutine<Int> { cont ->
                firestore.collection(COLLECTION).document(DOCUMENT)
                    .collection(DEVICES_SUBCOLLECTION)
                    .whereEqualTo(FIELD_BUSINESS_NAME_NORMALIZED, normalized)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val count = snapshot.documents.count { it.id != deviceId }
                        if (cont.isActive) cont.resume(count)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(-1) }
            }
        }.getOrNull()?.takeIf { it >= 0 }
    }

    suspend fun refresh() {
        val activeUntilMillis = runCatching { fetchActiveUntilMillis() }.getOrNull()
        val blocked = runCatching { fetchBlocked() }.getOrNull()
        if (activeUntilMillis == null && blocked == null) return

        prefs.edit {
            if (activeUntilMillis != null) putLong(KEY_ACTIVE_UNTIL, activeUntilMillis)
            if (blocked != null) putBoolean(KEY_BLOCKED, blocked)
            putLong(KEY_LAST_SYNC, System.currentTimeMillis())
        }
        runCatching { pushHeartbeat() }
    }

    fun currentStatus(): LicenseStatus {
        if (prefs.getBoolean(KEY_BLOCKED, false)) return LicenseStatus.BLOCKED
        if (daysSinceLastSync() > OFFLINE_LOCK_DAYS) return LicenseStatus.OFFLINE_LOCKED

        val daysUntilDue = daysUntilDue()
        if (daysUntilDue == Long.MAX_VALUE) return LicenseStatus.ACTIVE
        return when {
            daysUntilDue > WARNING_THRESHOLD_DAYS -> LicenseStatus.ACTIVE
            daysUntilDue > -GRACE_PERIOD_DAYS -> LicenseStatus.WARNING
            else -> LicenseStatus.READ_ONLY
        }
    }

    fun daysUntilDue(): Long {
        val activeUntil = prefs.getLong(KEY_ACTIVE_UNTIL, NEVER_CHECKED)
        if (activeUntil == NEVER_CHECKED) return Long.MAX_VALUE
        return (activeUntil - System.currentTimeMillis()) / DAY_MILLIS
    }

    /** Days since this device last reached Firebase — anchored to first app launch until the first sync succeeds. */
    fun daysSinceLastSync(): Long {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, NEVER_CHECKED)
        val anchor = if (lastSync != NEVER_CHECKED) {
            lastSync
        } else {
            firstSeenMillis()
        }
        return (System.currentTimeMillis() - anchor) / DAY_MILLIS
    }

    private fun firstSeenMillis(): Long {
        val existing = prefs.getLong(KEY_FIRST_SEEN, NEVER_CHECKED)
        if (existing != NEVER_CHECKED) return existing
        val now = System.currentTimeMillis()
        prefs.edit { putLong(KEY_FIRST_SEEN, now) }
        return now
    }

    private suspend fun fetchActiveUntilMillis(): Long? = suspendCancellableCoroutine { cont ->
        firestore.collection(COLLECTION).document(DOCUMENT).get()
            .addOnSuccessListener { snapshot ->
                val value = if (snapshot != null && snapshot.exists()) snapshot.getLong(FIELD_ACTIVE_UNTIL) else null
                if (cont.isActive) cont.resume(value)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    private suspend fun fetchBlocked(): Boolean? = suspendCancellableCoroutine { cont ->
        firestore.collection(COLLECTION).document(DOCUMENT)
            .collection(DEVICES_SUBCOLLECTION).document(deviceId).get()
            .addOnSuccessListener { snapshot ->
                val value = if (snapshot != null && snapshot.exists()) snapshot.getBoolean(FIELD_BLOCKED) ?: false else false
                if (cont.isActive) cont.resume(value)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    private suspend fun pushHeartbeat(): Unit = suspendCancellableCoroutine { cont ->
        val heartbeat = mapOf(
            FIELD_MODEL to "${Build.MANUFACTURER} ${Build.MODEL}",
            FIELD_LAST_SEEN to FieldValue.serverTimestamp(),
            FIELD_BUSINESS_NAME to businessName().orEmpty(),
            FIELD_BUSINESS_NAME_NORMALIZED to normalizeBusinessName(businessName().orEmpty()),
        )
        firestore.collection(COLLECTION).document(DOCUMENT)
            .collection(DEVICES_SUBCOLLECTION).document(deviceId)
            .set(heartbeat, SetOptions.merge())
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resume(Unit) }
    }

    companion object {
        private fun normalizeBusinessName(name: String): String = name.trim().lowercase()

        private const val PREFS_NAME = "license_prefs"
        private const val KEY_ACTIVE_UNTIL = "active_until_millis"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_LAST_SYNC = "last_sync_millis"
        private const val KEY_FIRST_SEEN = "first_seen_millis"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_FALLBACK_DEVICE_ID = "fallback_device_id"
        private const val KNOWN_BAD_ANDROID_ID = "9774d56d682e549c"
        private const val NEVER_CHECKED = -1L
        private const val COLLECTION = "license"
        private const val DOCUMENT = "status"
        private const val DEVICES_SUBCOLLECTION = "devices"
        private const val FIELD_ACTIVE_UNTIL = "activeUntilMillis"
        private const val FIELD_BLOCKED = "blocked"
        private const val FIELD_MODEL = "model"
        private const val FIELD_LAST_SEEN = "lastSeenMillis"
        private const val FIELD_BUSINESS_NAME = "businessName"
        private const val FIELD_BUSINESS_NAME_NORMALIZED = "businessNameNormalized"
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val WARNING_THRESHOLD_DAYS = 7L
        const val GRACE_PERIOD_DAYS = 5L
        const val OFFLINE_LOCK_DAYS = 5L
    }
}
