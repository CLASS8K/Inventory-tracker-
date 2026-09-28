# Firestore rule addition for `stockEvents`

Add this alongside the existing `license/status/devices` rules (same project,
`nkhokwe-e683b`) — publish it the same way those were published (Firebase
Console → Firestore Database → Rules).

```
match /stockEvents/{eventId} {
  // The app creates one event per Stock Take. No auth exists, so this can't
  // check *who* is writing — only *what* they're allowed to write: exactly
  // these fields, and a server-set timestamp (not a client-supplied one).
  allow create: if request.resource.data.keys().hasOnly([
                     'businessName', 'businessNameNormalized', 'deviceId',
                     'itemName', 'detail', 'actorName', 'timestamp'
                   ])
                && request.resource.data.timestamp == request.time;

  // The dashboard reads this collection with no login — same open-read
  // posture as license/status/devices today. Revisit if this needs to be
  // private per business later.
  allow read: if true;

  // Events are an append-only log: nothing should ever edit or remove one.
  allow update, delete: if false;
}
```

This only needs to be added once; it doesn't touch or replace the existing
`license/status/devices` rules.
