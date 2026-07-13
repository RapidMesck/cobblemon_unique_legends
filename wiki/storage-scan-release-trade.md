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
- detect duplicates (same species, different Pokemon UUID)
- recognize evolution chains (same Pokemon UUID, different species)
- update owner data when a tracked Pokemon moved to another player

## Release handling

When a player releases a tracked unique Pokemon, the mod listens to Cobblemon's release event.

If the released Pokemon UUID matches an active lock, the **entire evolution chain**
is unlocked — all species sharing the same Pokemon UUID. Storage is saved.

## Evolution handling

When a tracked Pokemon evolves, the mod listens to Cobblemon's
`EVOLUTION_COMPLETE` event. A new lock is registered for the evolved species
using the same Pokemon UUID (which is preserved across evolutions).

The behavior for pre-evolution locks is controlled by `evolutionUnlockMode`
in the config:

- **`TERMINAL`** (default): Pre-evolution locks are cleared once the Pokemon
  reaches a species with no further evolutions (final form).
- **`ACCUMULATE`**: All pre-evolution locks are kept permanently.

### Scans and evolution chains

During scans, Pokemon with the same UUID but different species IDs are
recognized as evolution chains, not duplicates. Stale lock detection
releases the entire chain when the Pokemon UUID is no longer found.

Fused Pokemon (stored in another Pokemon's persistent NBT via mods like
CobblemonMegaShowdown) are also recognized. The scan loads them from the
host Pokemon's `fusion_pokemon` NBT compound to prevent false stale lock
detection during fusion.

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
