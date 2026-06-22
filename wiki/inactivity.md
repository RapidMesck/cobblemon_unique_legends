# Inactivity System

The inactivity system can release unique Pokemon from players who have not been active for a configured number of days.

## Config

```json
"releaseOnInactive": true,
"inactiveDaysToRelease": 30,
"checkInactiveOnServerStart": true,
"checkInactiveIntervalMinutes": 60
```

## Last seen tracking

When a player joins the server, the mod updates `lastSeenAt` for all active locks owned by that player.

## Cleanup behavior

When a lock owner is inactive for too long, the mod:

1. Looks for the exact Pokemon UUID in the owner's party.
2. Looks for the exact Pokemon UUID in the owner's PC.
3. Removes the Pokemon if found.
4. Unlocks the species.
5. Saves storage.
6. Sends configured messages and broadcasts.

If the Pokemon is not found, the species is still unlocked. This prevents stale locks from blocking the server forever.

## Trade interaction

Trade ownership updates are important for inactivity.

If a unique Pokemon is traded, the new owner should become responsible for the lock. If you suspect ownership is stale, run:

```text
/uniquelegends scan fix
```
