# Firestore rule addition for `businessEvents`

Add this alongside the existing `license/status/devices` rules (same project,
`nkhokwe-e683b`) — publish it the same way those were published (Firebase
Console → Firestore Database → Rules).

```
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

  // Sales carry real revenue/profit figures, so — unlike the open-read
  // license/status/devices collection — this dashboard requires the
  // reader to be signed in (see dashboard/README.md: create at least one
  // Firebase Authentication user for the owner).
  allow read: if request.auth != null;

  // Events are an append-only log: nothing should ever edit or remove one.
  allow update, delete: if false;
}
```

If you already published the earlier `stockEvents`-only rule from before this
change, replace it with this one — the collection was renamed to
`businessEvents` to cover sales too, and the read rule is now auth-gated
instead of open.
