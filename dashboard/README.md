# Nkhokwe — Live Activity Dashboard

A single static page (`index.html`) showing a live feed of Stock Take and
Sale events as they happen, plus running Today's Revenue / Today's Profit
totals — no login to the *app* required, just a browser and a dashboard
account (see step 3 below).

## What it reads

Every Stock Take and every Sale in the app writes one event (item, detail,
staff name, business, device, revenue/profit where applicable, server
timestamp) to the Firestore collection `businessEvents` (see
`ActivityReporter.kt` in the app). This page listens to that collection in
real time.

## One-time setup (you do this in Firebase Console, not in code)

1. **Register a Web app** on the same Firebase project (`nkhokwe-e683b`) used
   for licensing: Project Settings → General → "Your apps" → Add app → Web
   (`</>`). Give it any nickname, e.g. "Nkhokwe Dashboard". You do **not** need
   Firebase Hosting for this step — just registering the app gets you a config
   object.
2. ~~Copy the `firebaseConfig` values~~ Already done — `index.html` has the
   real `apiKey`/`messagingSenderId`/`appId` for the "Nkhokwe Dashboard" web
   app. These values are meant to ship in client-side code (every Firebase
   web app works this way) — they are not secrets. What actually protects
   the data is sign-in + the Firestore rules below.
3. **Turn on Firebase Authentication** (Console → Authentication → Sign-in
   method → enable "Email/Password"), then create at least one user
   (Authentication → Users → Add user) — that's the login you'll use on the
   dashboard. Because this page now shows real revenue/profit, it's gated
   behind sign-in, unlike the licensing device list.
4. **Add the Firestore rule** in `FIRESTORE_RULES.md` — this replaces the
   earlier open-read rule with one that requires sign-in to read.
5. Host `index.html` however you like:
   - **Firebase Hosting** (`firebase deploy --only hosting`, free tier) is the
     easiest way to get a stable link to bookmark.
   - Or just open the file locally / drop it on any static host (GitHub Pages,
     Netlify) — it has no server-side code, no build step.

## Known limitations

- One shared login for now, not per-staff-member accounts. Fine for an
  owner-only view; if multiple people need their own logins later, add more
  users in the same Authentication panel — no code change needed for that.
- The app itself still has no Firebase Auth — only this dashboard does. The
  app can always write an event; only a signed-in dashboard viewer can read
  the feed back.
