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

## Features

- **Inventory list** — search by name, SKU, or category; low-stock items are highlighted
- **Add / edit / delete items** — name, SKU, category, quantity, low-stock threshold, unit price
- **Low-stock alerts** — a banner appears when any item is at or under its threshold, with a button that opens the device's email app pre-filled with the affected items (`Intent.ACTION_SENDTO`, no backend required)
- **Audit log** — every create/update/delete is recorded with a timestamp and viewable from the top bar

All data is persisted locally via Room; nothing here requires the Gemini API key yet (it's wired up for future AI-assisted features per the project's Firebase AI dependency, not used by the current UI).

## Architecture

```
app/src/main/java/com/example/
  MainActivity.kt                Hosts the Compose tree, injects InventoryViewModel via Hilt
  inventory/
    InventoryApplication.kt      @HiltAndroidApp entry point
    data/
      InventoryItem.kt            Room @Entity
      InventoryDao.kt / AuditLogDao.kt
      InventoryDatabase.kt         Room @Database
      InventoryRepository.kt        Combines DAOs, writes audit log entries on every mutation
    di/
      DatabaseModule.kt            Hilt module providing the Room database + DAOs
    ui/
      InventoryViewModel.kt        Exposes InventoryUiState (items, search, low-stock, audit log)
      screens/
        InventoryMainScreen.kt      List + search + low-stock banner + FAB
        ItemEditorDialog.kt          Add/edit form
        AuditLogSheet.kt              Audit log bottom sheet
  ui/theme/                        MyApplicationTheme (Material3, dynamic color on Android 12+)
```

## Known gaps

- No pre-Android-12 (API < 26) launcher icon fallback — only the adaptive icon variant exists, since generating binary PNGs wasn't practical here. Fine for essentially all real devices at this point, but worth knowing.
- No Gradle wrapper binaries (`gradlew`/`gradlew.bat`/`gradle-wrapper.jar`) — couldn't generate them in the sandbox this was built in (no network access to Google's Maven repo). Android Studio will offer to regenerate the wrapper automatically on first open.
- Retrofit/Moshi/Firebase AI dependencies are present but unused — they were already declared before this change, presumably for future AI-assisted features.

