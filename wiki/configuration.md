# Configuration Reference

The server config is generated at:

```text
config/unique_legends/server.json
```

Use this command after editing it:

```text
/uniquelegends reload
```

## Main options

```json
"enabled": true
```

Enables or disables the mod.

```json
"trackedPokemons": [],
"untrackedPokemons": []
```

Species allowlist and blocklist. Use resource ids such as:

```text
cobblemon:articuno
cobblemon:mewtwo
```

```json
"trackedLabels": ["legendary", "mythical", "ultra_beast"],
"untrackedLabels": []
```

Label allowlist and blocklist. Labels come from Cobblemon species/form data.

```json
"ignoreLockIfShiny": false
```

If true, shiny Pokemon ignore the unique lock system.

```json
"evolutionUnlockMode": "TERMINAL"
```

Controls pre-evolution lock behavior:

- `"TERMINAL"` (default): Unlocks pre-evolutions when the Pokemon reaches a
  species with no further evolutions (final form).
- `"ACCUMULATE"`: Keeps all pre-evolution locks permanently.

## Tracking rules

Rules are evaluated in this order:

1. If `enabled` is false, nothing is tracked.
2. If `ignoreLockIfShiny` is true and the Pokemon is shiny, it is not tracked.
3. If the species is in `untrackedPokemons`, it is not tracked.
4. If the Pokemon has a label in `untrackedLabels`, it is not tracked.
5. If `trackedPokemons` has entries, only those species are tracked.
6. Otherwise, if `trackedLabels` has entries, Pokemon with those labels are tracked.
7. If no tracked species or labels are configured, all non-untracked Pokemon are tracked.

## Capture options

```json
"captureLimitPerSpecies": 1
```

Reserved for the lock limit. The current implementation enforces one active lock per species.

```json
"broadcastCapture": true
```

Sends a chat broadcast when a unique Pokemon is captured.

```json
"titleBroadcastCapture": true
```

Sends a title/subtitle broadcast to all players when a unique Pokemon is captured.

## Inactivity options

```json
"releaseOnInactive": true,
"inactiveDaysToRelease": 30,
"checkInactiveOnServerStart": true,
"checkInactiveIntervalMinutes": 60
```

These control whether inactive owners lose their unique Pokemon, how long they can be inactive, and when checks run.

## Startup scan options

```json
"scanExistingPokemonOnServerStart": true,
"scanExistingPokemonOnlyWhenRegistryEmpty": true
```

These help existing servers build the first lock registry from player party and PC data.

## Debug

```json
"debug": false
```

Reserved for additional debug behavior.
