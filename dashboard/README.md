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
   method → enable "Email/Password").
4. **Add the Firestore rule** in `FIRESTORE_RULES.md`. Each login only sees
   the one business its `dashboardUsers` doc names — see "Adding a client"
   below.
5. **Go live** — the repo root already has `firebase.json`/`.firebaserc`
   pointing at this folder, so it's one command (needs
   [Node.js](https://nodejs.org) installed, one time):
   ```
   npm install -g firebase-tools
   firebase login
   firebase deploy --only hosting
   ```
   `firebase login` opens a browser for you to sign in with the Google
   account that owns the `nkhokwe-e683b` project — this step can't be done
   for you, since it's your login, not a code change. The deploy prints a
   URL like `https://nkhokwe-e683b.web.app` — that's the one link both
   client logins share (each still only sees their own business's data
   after signing in). Re-run just `firebase deploy --only hosting` any time
   `index.html` changes.

## Adding a client (one dashboard login per business)

Each business gets its own login that can only ever see its own events —
never another client's. Two things, both by hand in Firebase Console, no
code change:

1. **Authentication → Users → Add user.** Email + password for that owner.
   Copy the new user's **UID**.
2. **Firestore Database → Data → `dashboardUsers` collection → Add document.**
   Document ID = that UID. Fields:
   - `businessName` (string) — display name shown on the dashboard, e.g.
     `Ventha`. Can be a placeholder (`Client 1`) until you know the real one.
   - `businessNameNormalized` (string) — must exactly match what the app
     computes: the business name **trimmed and lowercased** (`.trim().lowercase()`
     in `ActivityReporter.kt`). This is what events are actually matched on,
     so it must match whatever name gets entered in-app at first admin
     sign-in — update it once you know the real name.

Right now there are 2 such logins/documents set up with placeholder names,
ready to be pointed at real clients once they're onboarded.

## Known limitations

- Per-business isolation depends on `businessNameNormalized` matching
  exactly what the app has on file — if a client's in-app business name
  changes or was mistyped, update the `dashboardUsers` doc to match, or
  their dashboard will show nothing.
- The app itself still has no Firebase Auth — only this dashboard does. The
  app can always write an event; only a signed-in dashboard viewer can read
  the feed back, and only their own business's events.
- First load after adding this rule may show a "failed-precondition" console
  error with a link to create a composite index (`businessNameNormalized` +
  `timestamp`) — click it once, it takes a minute to build, then reload.
