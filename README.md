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
- **Admin Panel** — create/remove users and see a restocker leaderboard ranked by XP
- **Dashboard** — Total Value (MWK), total units, and low-stock count, with animated entrance
- **Inventory list** — search by name, SKU, or category; category filter chips; sort by name, quantity, price, or recently updated; colour-coded In Stock / Low Stock / Out of Stock badges
- **Quick +/- stock adjustments** — one tap to restock or draw down an item directly from its card
- **Add / edit / delete items** (admin only) — name, SKU, category, quantity, low-stock threshold, unit price in MWK
- **Low-stock banner** — animated banner when any item is at or under its threshold
- **Share / export report** — share a full or low-stock-only inventory report through the Android share sheet
- **Audit log** — every create/update/delete is recorded with actor, action badge, and a relative timestamp

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
      InventoryDao.kt / AuditLogDao.kt / UserDao.kt
      InventoryDatabase.kt         Room @Database (v2 — fallbackToDestructiveMigration pre-release)
      InventoryRepository.kt        Combines DAOs, writes audit log entries on every mutation
      UserRepository.kt             User CRUD, PIN hashing (salted SHA-256), XP awards
      SessionManager.kt             In-memory signed-in user for the process
      PinHasher.kt                  Salted SHA-256 PIN hashing
    di/
      DatabaseModule.kt            Hilt module providing the Room database + DAOs
    ui/
      InventoryViewModel.kt        Exposes InventoryUiState (items, search, sort, filter, dashboard totals)
      AuthViewModel.kt              Exposes AuthUiState (users, current user, leaderboard)
      screens/
        SignInScreen.kt             Profile picker, PIN pad, user creation
        AdminPanelScreen.kt          User management + restocker leaderboard
        InventoryMainScreen.kt       Dashboard + search + filters + sort + list + FAB
        ItemEditorDialog.kt          Add/edit form, fields locked down for Stock Keepers
        AuditLogSheet.kt              Audit log bottom sheet
    util/
      CurrencyFormatter.kt          MWK formatting
      RelativeTime.kt                "5m ago" style timestamps
  ui/theme/                        MyApplicationTheme (Material3, Fraunces typography, dynamic color on Android 12+)
```

## Known gaps

- No pre-Android-12 (API < 26) launcher icon fallback — only the adaptive icon variant exists, since generating binary PNGs wasn't practical here. Fine for essentially all real devices at this point, but worth knowing.
- No Gradle wrapper binaries (`gradlew`/`gradlew.bat`/`gradle-wrapper.jar`) — couldn't generate them in the sandbox this was built in (no network access to Google's Maven repo). Android Studio will offer to regenerate the wrapper automatically on first open; the CI workflow provisions Gradle directly instead of relying on the wrapper.
- Retrofit/Moshi/Firebase AI dependencies are present but unused — they were already declared before this change, presumably for future AI-assisted features.
- The CI workflow and this whole rebuild were assembled without ever being able to run a real Gradle build (same network restriction) — if `compileSdk 36` isn't available on GitHub's hosted runners yet, the first CI run may need an explicit SDK platform install step added to `build-apk.yml`. Check the Actions log if the build fails there.
- Fraunces is loaded as a downloadable Google Font at runtime (via Google Play services), not bundled into the APK. On a device without Play services, or offline on first launch, text falls back to the system font until it downloads. This is the same rebuild-without-a-real-build caveat above — the `font_certs.xml` cert hashes are Google's well-known, unchanging downloadable-fonts certs (copied from `android/compose-samples`), not something specific to this app.
- The Room schema bumped to v2 to add the `user_profiles` table and an `actorName` column on `audit_log`. There's no migration path — `fallbackToDestructiveMigration` wipes local data on upgrade. Fine pre-release; revisit before a real release with user data on device.
- PINs are 4 digits, hashed with a per-user salted SHA-256 (not bcrypt/Argon2) and stored locally — appropriate for a shared-device staff gate, not a substitute for real authentication if this app ever handles more sensitive data.

