<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/fab4b4cb-a002-4e63-8cdd-566dc7b1e858

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## Getting an APK without Android Studio

Every push to `main` (and manual runs via the Actions tab) builds a debug APK in CI and uploads it as a workflow artifact — see `.github/workflows/build-apk.yml`. Go to the **Actions** tab, open the latest "Build debug APK" run, and download the `inventory-tracker-debug-apk` artifact. No local Android SDK needed.

## Features

- **Gamified sign-in** — pick a profile card and unlock it with a 4-digit PIN; the first account created becomes the admin
- **Role-based access** — 👑 Admins have full control (pricing, thresholds, item deletion, user management); 📦 Stock Keepers can restock existing items and earn XP, but can't touch pricing/config or delete anything
- **Admin Panel** — create/remove users, promote a Stock Keeper to Admin or demote an Admin back (blocked if it would leave zero admins, same protection as delete), reset a user's PIN without losing their XP/level, see a restocker leaderboard ranked by XP, and back up or restore the entire local database as a file
- **Dashboard** — Total Value (MWK), total units, and low-stock count, with animated entrance; Admins also see Potential Profit (current stock at selling price minus cost price)
- **Inventory list** — search by name, SKU, or category; category filter chips; sort by name, quantity, price, or recently updated; colour-coded In Stock / Low Stock / Out of Stock badges
- **Quick sell / restock from the item card** — tap `−` to sell 1 unit; hold `−` to sell a specific quantity at once (e.g. "sold 3 shots"). Either way it's a real sale: revenue and admin-only profit are computed and logged immediately, the same math as a Stock Take's "Sold" branch — a barman tapping through a shift doesn't need to run a reconciliation afterward just to have it show up as revenue. Tap `+` to restock 1 (both roles; Stock Keepers earn XP for it). Clamps to what's on hand — can't sell past zero
- **Add / edit / delete items** (admin only) — name, SKU, category, unit (Bottle/Can/Shot/Unit), quantity, low-stock threshold, selling price in MWK, cost price, and an optional item photo
- **Cost price is admin-only, everywhere** — a Stock Keeper never sees the Cost Price field at all (not disabled, not rendered), never sees a Profit figure on a stock take, and never sees the Profit line on an audit log entry. Selling price and revenue are visible to both roles (a barman already knows what a drink sells for); only the margin is withheld
- **Receipt photos** — either role can attach a photo of the supplier receipt/delivery note to a restock; it's saved on that audit log entry, viewable full-size from the audit log
- **Stock take** — reconcile a physical count against the running total: enter opening and closing stock for an item and it computes quantity sold, revenue, and (Admins only) profit against the cost price. Closing ≥ opening is treated as an unlogged restock, not a sale, so it's never misreported as MWK 0 in revenue. When the count comes in lower than opening, you pick why — Sold, Spillage/Breakage, Complimentary/Staff, Theft/Loss, or Other — and only "Sold" counts as revenue; the rest are logged as a cost with zero revenue, so a dropped bottle or a comped drink can never inflate the numbers. Logged as a "Stock Take" audit entry; either role can record one. This is for catching discrepancies (spillage, theft, miscounts) against what the quick sell taps already logged — not the only way to record a sale
- **Undo a stock take** — saving one shows a Snackbar with an Undo action for a few seconds; tapping it restores the item's prior quantity and removes the audit entry outright (not a correcting entry — a typo caught in the next few seconds was never a real business event). Missing the window is permanent, same as any other stock take
- **Barcode scan** — a scanner icon on the main search bar and on the SKU field in the item editor launches Google Play services' on-device barcode scanner. Scanning in search jumps straight to a matching item by SKU; scanning in the editor fills the SKU field. No camera permission in this app — Play services owns the camera and scanning UI directly
- **Low-stock push notifications** — the moment an item's quantity crosses at-or-below its threshold, a local notification fires (edge-triggered — it won't re-fire on every subsequent restock tap while still low). Purely on-device; there's no server to push from. Undoing the stock take that caused it cancels the notification
- **Low-stock banner** — animated banner when any item is at or under its threshold; tap to expand it and see each low-stock item's linked supplier with one-tap Call / WhatsApp buttons to reorder. WhatsApp numbers entered in the local format (e.g. "0991234567") are normalized to +265 automatically, since wa.me links otherwise silently fail on a number without a country code
- **Supplier directory** (admin only) — a lightweight contacts list (name, phone, notes) managed from the Admin Panel's Suppliers tab; items can be linked to a supplier from the item editor's "Reorder from" picker, or a new supplier can be added inline without leaving the dialog. Deliberately not a purchase-order system — no order quantities, no delivery tracking, no approval flow — just "who do I call when this runs low," which is what a single-bar client actually needs day to day
- **Share / export report** — share a full or low-stock-only inventory report through the Android share sheet, or export the full inventory as a CSV file for bookkeeping/reconciliation
- **Audit log** — every create/update/delete is recorded with actor, action badge, and a relative timestamp
- **Auto sign-out** — a session is signed out automatically after 5 minutes backgrounded, so a shared device doesn't stay logged in as whoever last used it

All data is persisted locally via Room; nothing here requires the Gemini API key yet (it's wired up for future AI-assisted features per the project's Firebase AI dependency, not used by the current UI). Typography uses the Fraunces Google Font, loaded as a downloadable font via Google Play services.

## Architecture

```
app/src/main/java/com/example/
  MainActivity.kt                Hosts the Compose tree; switches between sign-in, admin panel, and main screen
  inventory/
    InventoryApplication.kt      @HiltAndroidApp entry point
    data/
      InventoryItem.kt            Room @Entity
      UserProfile.kt               Room @Entity — role, avatar, PIN hash, XP/level
      Supplier.kt                    Room @Entity — name, phone, notes
      InventoryDao.kt / AuditLogDao.kt / UserDao.kt / SupplierDao.kt
      InventoryDatabase.kt         Room @Database (v7 — fallbackToDestructiveMigration pre-release)
      InventoryRepository.kt        Combines DAOs, writes audit log entries on every mutation
      UserRepository.kt             User CRUD, PIN hashing (salted SHA-256), XP awards
      SupplierRepository.kt          Supplier CRUD
      SessionManager.kt             In-memory signed-in user for the process
      PinHasher.kt                  Salted SHA-256 PIN hashing
      BackupManager.kt              Exports/restores the on-device Room database as a file
      ImageStore.kt                  Copies picked photos into app-private storage (item photos, receipts)
      StockLossReason.kt             Why a stock take came in short — only SOLD counts as revenue
      LowStockNotifier.kt            Posts/cancels the local low-stock notification
    di/
      DatabaseModule.kt            Hilt module providing the Room database + DAOs
    ui/
      InventoryViewModel.kt        Exposes InventoryUiState (items, search, sort, filter, dashboard totals) + stock-take undo state
      AuthViewModel.kt              Exposes AuthUiState (users, current user, leaderboard)
      SupplierViewModel.kt          Exposes the supplier list; create/update/delete
      screens/
        SignInScreen.kt             Profile picker, PIN pad, user creation
        AdminPanelScreen.kt          User management + restocker leaderboard, Suppliers tab (add/edit/remove)
        InventoryMainScreen.kt       Dashboard + search (+ barcode scan) + filters + sort + list + FAB + low-stock supplier call/WhatsApp + stock-take undo Snackbar + quick sell (tap/hold) + bulk-sell dialog
        ItemEditorDialog.kt          Add/edit form, fields locked down for Stock Keepers, supplier picker, SKU barcode scan
        StockTakeDialog.kt            Opening/closing count → qty sold + revenue
        AuditLogSheet.kt              Audit log bottom sheet
    util/
      CurrencyFormatter.kt          MWK formatting
      RelativeTime.kt                "5m ago" style timestamps
      CsvExport.kt                   Full-inventory CSV for bookkeeping/reconciliation
      BarcodeScanner.kt              rememberBarcodeScanner() — wraps Play services' GmsBarcodeScanning
  ui/theme/                        MyApplicationTheme (Material3, Fraunces typography, dynamic color on Android 12+)
```

## Known gaps

- No pre-Android-12 (API < 26) launcher icon fallback — only the adaptive icon variant exists, since generating binary PNGs wasn't practical here. Fine for essentially all real devices at this point, but worth knowing.
- No Gradle wrapper binaries (`gradlew`/`gradlew.bat`/`gradle-wrapper.jar`) — couldn't generate them in the sandbox this was built in (no network access to Google's Maven repo). Android Studio will offer to regenerate the wrapper automatically on first open; the CI workflow provisions Gradle directly instead of relying on the wrapper.
- Retrofit/Moshi/Firebase AI dependencies are present but unused — they were already declared before this change, presumably for future AI-assisted features.
- Most of this was written without ever being able to run a real Gradle build locally (same network restriction — no Android SDK reachable in that sandbox). It's since been confirmed to compile via the `build-apk.yml` CI run on [#2](https://github.com/CLASS8K/Inventory-tracker-/pull/2), but any future changes made the same way should get the same CI confirmation before being called done.
- Backup files exported from the Admin Panel are a raw, unencrypted copy of the SQLite database — PINs inside are salted-hashed (not plaintext), but item data, prices, and names are not. Treat a backup file with the same care as the data it contains; it isn't meant to leave the business.
- Fraunces is loaded as a downloadable Google Font at runtime (via Google Play services), not bundled into the APK. On a device without Play services, or offline on first launch, text falls back to the system font until it downloads. This is the same rebuild-without-a-real-build caveat above — the `font_certs.xml` cert hashes are Google's well-known, unchanging downloadable-fonts certs (copied from `android/compose-samples`), not something specific to this app.
- The Room schema is now at v7 (`user_profiles` + `actorName` on `audit_log`, PIN-lockout columns, item/receipt photo paths, `unit` on `inventory_items`, `costPrice`/`profit`, then the `suppliers` table + `supplierId` on `inventory_items`). There's no migration path — `fallbackToDestructiveMigration` wipes local data on upgrade. Fine pre-release; revisit before a real release with user data on device. A restored backup from a different schema version is rejected outright rather than silently wiped (see `BackupManager.isValidBackup`) — the version check has to be kept in sync with `DB_VERSION` by hand since there's no migration path to lean on instead.
- Supplier tracking is deliberately minimal: a name, phone number, and notes, linked to an item. There's no purchase-order flow, no order history, no delivery/received-quantity tracking, and no reorder quantity suggestion — it answers "who do I call," not "what did I order and when does it arrive." If that's ever needed, it's a separate, larger feature.
- Cash/till reconciliation is explicitly out of scope for this app — that's an accounting/POS concern, not inventory. Flagging this as a deliberate non-goal so it doesn't read as an oversight.
- PINs are 4 digits, hashed with a per-user salted SHA-256 (not bcrypt/Argon2) and stored locally — appropriate for a shared-device staff gate, not a substitute for real authentication if this app ever handles more sensitive data. 5 wrong attempts locks the account for 60 seconds (`UserProfile.MAX_PIN_ATTEMPTS` / `LOCKOUT_DURATION_MILLIS`); an Admin can always clear a lock via "reset PIN" in the Admin Panel.
- Cost price and profit are role-gated in the UI (never rendered to a Stock Keeper), but they still live in plaintext in the local database and in any backup file exported from the Admin Panel — same caveat as the backup-file note above, just worth restating since this is the specific figure that was asked to stay hidden from staff. CSV export intentionally does *not* include cost price or profit, since CSV export is available to both roles.
- Item/receipt photos live in app-private storage (`filesDir/images/`), *not* inside the backup file — `BackupManager` only copies the SQLite database. Restoring an old backup can leave `photoPath`/`receiptPath` values pointing at images that no longer exist on this device (the UI just shows a blank thumbnail, it won't crash), and photos added after a backup was taken aren't in it. Worth fixing — bundle the images directory into the backup, e.g. as a zip — before this is relied on as the only backup strategy.
- `BackupManager.importFrom` closes Room's database connection immediately before swapping the live SQLite file for the restored one, so an in-flight write from another coroutine can't race the file copy. The window between that close and the user tapping "Restart now" on the follow-up dialog is small but not zero — nothing in the app initiates a database query during it today, but this hasn't been exercised against a real device, only reasoned through; worth keeping in mind if restore behavior ever looks flaky.
- No release signing key and no crash reporting wired up yet — both need something from outside this codebase (a real signing keystore you generate and keep safe; a real Firebase project for Crashlytics) rather than something that can be fabricated here. See the PR/chat history for what's needed to set each one up.
- Barcode scan (`com.google.android.gms:play-services-code-scanner:16.1.0`) depends on Google Play services being installed and up to date. On a device without it (rare in practice, but real on some non-Google-certified Android devices), the scan buttons just show a "couldn't start the barcode scanner" toast — the SKU field and search box are still plain text fields either way, so nothing is blocked by it. Confirmed building clean in CI after two rounds of fixing a wrong package name and an unverifiable `close()` call — see the commit history on this file for what didn't hold up on the first try.
- Low-stock notifications need the POST_NOTIFICATIONS runtime permission on Android 13+, requested once right after sign-in. If denied, the app doesn't re-prompt (by design — Android's own guidance against nagging) and low-stock alerts just silently don't show; the in-app low-stock banner and dashboard count still work regardless, so this is a convenience layer, not the only way to see low stock. There's no in-app way yet to re-request it after an initial denial short of the device's own app notification settings — worth adding if that turns out to matter in practice.
- The undo window on a stock take is a single in-memory slot (last stock take only) that's cleared if the screen recomposes away from it (e.g. the process is killed) — there's no "undo history," and once the Snackbar times out or a second stock take is saved, the previous one can't be recovered short of manually fixing the numbers.
- The quick sell tap (`−` on an item card) has no undo, unlike Stock Take — deliberate, not an oversight: it's meant to be tapped dozens of times a shift, and a Snackbar per tap would be noise, not a safety net. A wrong tap is fixed the same way a miscount is: run a Stock Take (or edit the item directly, Admin-only) to correct the quantity; the mistaken "Sale" audit entry itself stays in the log as a record of what happened, same as any other audit entry.

