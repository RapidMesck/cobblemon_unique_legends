# Storage, Scan, Release, and Trade

## Storage file

Unique Legends stores lock data per world.

The storage file is under the world folder:

```text
world/data/unique_legends/unique_legends.json
```

The storage contains active and inactive lock entries.

## Scan system

The scan system looks for known player UUIDs from:

- online players
- vanilla `playerdata`
- Cobblemon storage under `pokemon`

For each known player, it loads:

- Cobblemon party store
- Cobblemon PC store

The scan uses Cobblemon APIs instead of editing Pokemon files manually.

## Scan fix behavior

```text
/uniquelegends scan fix
```

Can:

- register missing locks
- release stale locks
- detect duplicates
- update owner data when a tracked Pokemon moved to another player

## Release handling

When a player releases a tracked unique Pokemon, the mod listens to Cobblemon's release event.

If the released Pokemon UUID matches the active lock, the species is unlocked and storage is saved.

## Trade handling

When a trade finishes, the mod listens to Cobblemon's trade event.

If a traded Pokemon is already registered as unique, the lock owner is updated to the new owner.

The mod updates:

- owner UUID
- owner name
- last seen timestamp

If a trade happened before the mod was updated or if another mod moved Pokemon without firing Cobblemon's trade event, run:

```text
/uniquelegends scan fix
```
