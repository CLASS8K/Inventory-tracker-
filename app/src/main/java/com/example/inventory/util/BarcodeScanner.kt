package com.example.inventory.util

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Launches Google Play services' on-device barcode scanner UI and reports the scanned value.
 * No camera permission needed — Play services owns the camera and the scanning UI for this,
 * so there's no preview surface or frame analysis to build or verify here. The client isn't
 * explicitly released on dispose — this build's resolved API surface has no callable close()
 * on it, unlike some documentation for this API — so it's left to normal GC instead.
 */
@Composable
fun rememberBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnScanned = rememberUpdatedState(onScanned)
    val scanner = remember(context) { GmsBarcodeScanning.getClient(context) }
    return remember(scanner) {
        {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { currentOnScanned.value(it) }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Couldn't start the barcode scanner", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
