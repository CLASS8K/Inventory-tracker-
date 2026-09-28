# Nkhokwe — Live Activity Dashboard

A single static page (`index.html`) that shows a live feed of Stock Take events
(who counted what, on which device, when) as they happen — no login to the app
required, just open this page in a browser.

## What it reads

Every time a bartender does a Stock Take in the app, it writes one event to the
Firestore collection `stockEvents` (see `StockEventReporter.kt` in the app).
This page listens to that collection in real time and lists the latest 100
events, newest first, with a business-name filter.

## One-time setup (you do this in Firebase Console, not in code)

1. **Register a Web app** on the same Firebase project (`nkhokwe-e683b`) used
   for licensing: Project Settings → General → "Your apps" → Add app → Web
   (`</>`). Give it any nickname, e.g. "Nkhokwe Dashboard". You do **not** need
   Firebase Hosting for this step — just registering the app gets you a config
   object.
2. Copy the `firebaseConfig` values it shows you (`apiKey`, `messagingSenderId`,
   `appId`) into `index.html`'s `firebaseConfig` block, replacing the
   `REPLACE_ME` placeholders. `authDomain`, `projectId`, `storageBucket` are
   already filled in to match the existing project.
   - These values are meant to ship in client-side code (every Firebase web app
     works this way) — they are not secrets. What actually protects the data is
     the Firestore rules below, the same way `license/status/devices` is
     already protected without Firebase Auth.
3. **Add the Firestore rule** in `FIRESTORE_RULES.md` (same place the existing
   `license/status/devices` rules live) so the app can write events and the
   dashboard can read them, but nothing else can be written or altered.
4. Host `index.html` however you like:
   - **Firebase Hosting** (`firebase deploy --only hosting`, free tier) is the
     easiest way to get a stable link to bookmark.
   - Or just open the file locally / drop it on any static host (GitHub Pages,
     Netlify) — it has no server-side code, no build step.

## Known limitation

There's no login on this page — anyone with the link can see the live feed for
every business using the app (filterable by business name, but not restricted
to one). That matches the app's existing no-auth design (the same is already
true of the `devices` collection used for duplicate-name checks). If this
dashboard is meant to be private per-client rather than an internal ops view
for you, it needs real access control (Firebase Auth + per-business rules) —
a bigger job, flag it if you want that instead of this MVP.
