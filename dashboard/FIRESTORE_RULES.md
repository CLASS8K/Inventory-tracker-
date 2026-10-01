# Firestore rules for `businessEvents` + `dashboardUsers`

Add these alongside the existing `license/status/devices` rules (same
project, `nkhokwe-e683b`) — publish them the same way those were published
(Firebase Console → Firestore Database → Rules). This replaces any earlier
`businessEvents` rule.

Two kinds of dashboard login:
- **Admin** — a fixed, short allowlist of UIDs (currently Frank's own
  logins) that can read every business's events. Listed directly in the
  rule below, not via a `dashboardUsers` doc.
- **Client** — scoped to the one business named in their `dashboardUsers/{uid}`
  doc (see `dashboard/README.md` → "Adding a client").

```
match /dashboardUsers/{uid} {
  // Maps a dashboard login (Firebase Auth uid) to the one business it may
  // see. You create these by hand in Firestore, one per client owner — see
  // dashboard/README.md. A signed-in user may read only their own mapping;
  // nobody can write one from the client, only you, from the console.
  allow read: if request.auth != null && request.auth.uid == uid;
  allow write: if false;
}

match /businessEvents/{eventId} {
  // The app writes one event per Stock Take or Sale. No auth exists on the
  // app side, so this can't check *who* is writing — only *what* they're
  // allowed to write: exactly these fields, a valid type, and a server-set
  // timestamp (not a client-supplied one).
  allow create: if request.resource.data.keys().hasOnly([
                     'type', 'businessName', 'businessNameNormalized',
                     'deviceId', 'itemName', 'detail', 'actorName',
                     'timestamp', 'revenue', 'profit'
                   ])
                && request.resource.data.type in ['stock_take', 'sale']
                && request.resource.data.timestamp == request.time;

  // Admins (the UID allowlist below) read every business. Everyone else
  // may only read events for the ONE business their dashboardUsers mapping
  // names — never another client's data, even if they know the collection
  // exists. The dashboard's query mirrors this: admins query unfiltered,
  // everyone else MUST filter on businessNameNormalized (see index.html) —
  // an unfiltered query from a non-admin login fails outright rather than
  // silently omitting other businesses' rows.
  //
  // Keep this UID list identical to ADMIN_UIDS in dashboard/index.html.
  allow read: if request.auth != null
              && (request.auth.uid in [
                    'vP1LliGDPTRnYkBWzrCtvpI1fUD2',
                    'eqvCMik8SeSygloQtBYnbCZrx5n2'
                  ]
                  || resource.data.businessNameNormalized ==
                     get(/databases/$(database)/documents/dashboardUsers/$(request.auth.uid)).data.businessNameNormalized);

  // Events are an append-only log: nothing should ever edit or remove one.
  allow update, delete: if false;
}
```

If you already published an earlier version of this rule, replace it with
the one above.
