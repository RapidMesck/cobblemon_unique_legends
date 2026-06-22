# How Unique Locks Work

Unique Legends stores one active lock per tracked species.

Example:

```text
Articuno has no active lock
Player A captures Articuno
The server stores a lock for cobblemon:articuno
Player B finds another Articuno
Player B can battle it
Player B cannot capture it
```

## Lock data

Each lock stores:

- species id, such as `cobblemon:articuno`
- owner UUID
- owner name
- Pokemon UUID
- capture time
- last seen time
- active state

## What counts as the same Pokemon

The lock is per species, not per individual spawn.

If `cobblemon:articuno` is locked, another Articuno cannot be captured unless the lock is released.

## What releases a lock

A lock can be released by:

- the owner releasing the tracked Pokemon
- inactivity cleanup
- admin command
- `/uniquelegends scan fix` detecting a stale lock

## Shiny exception

If enabled, shiny Pokemon ignore the unique lock system:

```json
"ignoreLockIfShiny": true
```

When enabled, shiny tracked Pokemon:

- can be captured even if the species is locked
- do not create a new lock
- are ignored by scan
- are ignored by release and trade lock handling
