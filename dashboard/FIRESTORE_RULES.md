# Firestore rules for `businessEvents` + `dashboardUsers`

Add these alongside the existing `license/status/devices` rules (same
project, `nkhokwe-e683b`) — publish them the same way those were published
(Firebase Console → Firestore Database → Rules). This replaces any earlier
`businessEvents`-only rule (the open-signed-in-read version): read is now
scoped per business, not just gated behind any sign-in.

```
match /dashboardUsers/{uid} {
  // Maps a dashboard login (Firebase Auth uid) to the one business it may
  // see. You create these by hand in Firestore, one per client owner — see
  // dashboard/README.md. Nobody can read or write their own mapping from
  // the client; only you, from the console.
  allow read, write: if false;
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

  // Sales carry real revenue/profit figures, so a signed-in reader may only
  // read events for the ONE business their dashboardUsers mapping names —
  // never another client's data, even if they know the collection exists.
  // This means the dashboard's query MUST filter on businessNameNormalized
  // (see index.html): an unfiltered query fails outright rather than
  // silently omitting other businesses' rows.
  allow read: if request.auth != null
              && resource.data.businessNameNormalized ==
                 get(/databases/$(database)/documents/dashboardUsers/$(request.auth.uid)).data.businessNameNormalized;

  // Events are an append-only log: nothing should ever edit or remove one.
  allow update, delete: if false;
}
```

If you already published the earlier open-signed-in-read `businessEvents`
rule, replace it with the one above.
