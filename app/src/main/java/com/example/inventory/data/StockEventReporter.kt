package com.example.inventory.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Best-effort live feed of stock-take activity to Firestore, so an owner can watch a
 * dashboard and see when a bartender counts stock without opening the app themselves.
 * Never blocks or fails the actual stock take on a network problem: every write is
 * fire-and-forget and silently dropped on failure, same posture as [LicenseChecker].
 */
@Singleton
class StockEventReporter @Inject constructor(
    private val licenseChecker: LicenseChecker,
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun reportStockTake(itemName: String, detail: String, actorName: String) {
        runCatching { push(itemName, detail, actorName) }
    }

    private suspend fun push(itemName: String, detail: String, actorName: String): Unit =
        suspendCancellableCoroutine { cont ->
            val businessName = licenseChecker.businessName().orEmpty()
            val event = mapOf(
                FIELD_BUSINESS_NAME to businessName,
                FIELD_BUSINESS_NAME_NORMALIZED to normalizeBusinessName(businessName),
                FIELD_DEVICE_ID to licenseChecker.deviceId,
                FIELD_ITEM_NAME to itemName,
                FIELD_DETAIL to detail,
                FIELD_ACTOR_NAME to actorName,
                FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            )
            firestore.collection(COLLECTION).add(event)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { if (cont.isActive) cont.resume(Unit) }
        }

    companion object {
        private fun normalizeBusinessName(name: String): String = name.trim().lowercase()

        private const val COLLECTION = "stockEvents"
        private const val FIELD_BUSINESS_NAME = "businessName"
        private const val FIELD_BUSINESS_NAME_NORMALIZED = "businessNameNormalized"
        private const val FIELD_DEVICE_ID = "deviceId"
        private const val FIELD_ITEM_NAME = "itemName"
        private const val FIELD_DETAIL = "detail"
        private const val FIELD_ACTOR_NAME = "actorName"
        private const val FIELD_TIMESTAMP = "timestamp"
    }
}
