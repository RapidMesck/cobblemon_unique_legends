# Unique Legends

Unique Legends is a server-side Cobblemon addon that makes selected Pokemon unique per server.

By default, the mod tracks Cobblemon Pokemon with these labels:

- `legendary`
- `mythical`
- `ultra_beast`

When a tracked Pokemon is captured, that species becomes locked globally. Other players can still find, battle, and defeat future spawns of that species, but they cannot capture another one while the lock is active.

## Features

- Server-side only behavior
- Fabric and NeoForge support
- Unique species locks for legendary, mythical, and Ultra Beast Pokemon by default
- Configurable tracked species and labels
- Optional shiny bypass with `ignoreLockIfShiny`
- Capture blocking outside battle
- Battle-safe capture failure inside Cobblemon battles
- Pokeball refund for blocked battle captures
- Persistent world-specific lock storage
- Startup scan for existing servers
- Manual scan and repair command
- Release handling
- Trade ownership handling
- Inactivity-based release
- Configurable chat messages and capture titles
- Cobblemon species autocomplete in commands

## Requirements

- Minecraft `1.21.1`
- Cobblemon `1.7.0+1.21.1`
- Java `21`
- Fabric or NeoForge

## Installation

1. Install Cobblemon on the server.
2. Install Unique Legends on the server.
3. Start the server once.
4. Edit the generated config if needed:

```text
config/unique_legends/server.json
```

5. Reload the mod config:

```text
/uniquelegends reload
```

Players do not need the mod installed on their clients.

## Commands

Base command:

```text
/uniquelegends
```

Aliases:

```text
/ul
/legendarylock
```

Common commands:

```text
/uniquelegends list
/uniquelegends info <species>
/uniquelegends unlock <species>
/uniquelegends scan
/uniquelegends scan fix
/uniquelegends check-inactive
/uniquelegends reload
/uniquelegends debug
```

The `<species>` argument uses Cobblemon species autocomplete, like `/pokespawn`.

## Documentation

Full documentation is available in the [wiki](wiki/README.md).

Useful pages:

- [Configuration Reference](wiki/configuration.md)
- [Commands](wiki/commands.md)
- [Capture, Battle, and Pokeballs](wiki/capture-and-battle.md)
- [Storage, Scan, Release, and Trade](wiki/storage-scan-release-trade.md)
- [Messages and Formatting](wiki/messages.md)
- [Troubleshooting](wiki/troubleshooting.md)

## Default Config Behavior

The default tracked labels are:

```json
"trackedLabels": [
  "legendary",
  "mythical",
  "ultra_beast"
]
```

The default shiny behavior is:

```json
"ignoreLockIfShiny": false
```

Capture broadcasts are enabled by default:

```json
"broadcastCapture": true,
"titleBroadcastCapture": true
```

Existing server scan is enabled by default when the registry is empty:

```json
"scanExistingPokemonOnServerStart": true,
"scanExistingPokemonOnlyWhenRegistryEmpty": true
```

## Technical Information

## Project Stack

- Language: Kotlin
- Build: Gradle
- Loader architecture: Architectury Loom multi-loader
- Modules:
  - `common`
  - `fabric`
  - `neoforge`

## Module Layout

```text
common/src/main/kotlin/com/nbp/unique_legends/
  UniqueLegends.kt
  commands/
  config/
  data/
  events/
  service/
  util/

fabric/src/main/kotlin/com/nbp/unique_legends/fabric/
  UniqueLegendsFabric.kt

neoforge/src/main/kotlin/com/nbp/unique_legends/neoforge/
  UniqueLegendsNeoForge.kt
```

## Common Module Responsibilities

The `common` module contains the server-side business logic:

- config loading and saving
- lock registry
- world storage
- Cobblemon event handlers
- commands
- inactivity checks
- scan and repair logic
- Pokemon removal service
- message formatting

## Loader Modules

The loader modules are intentionally thin.

Fabric:

- initializes `UniqueLegends`
- registers commands through Fabric command registration

NeoForge:

- initializes `UniqueLegends`
- registers commands through NeoForge command registration

Business logic should stay in `common` whenever possible.

## Main Components

### Config

```text
config/UniqueLegendsConfig.kt
config/UniqueLegendsConfigManager.kt
```

Defines and loads:

- tracked species
- tracked labels
- shiny bypass
- scan behavior
- inactivity behavior
- message templates

### Registry and Storage

```text
data/UniqueLegendEntry.kt
data/UniqueLegendRegistry.kt
data/UniqueLegendStorage.kt
```

The registry is the in-memory state for locks.

The storage persists lock data per world under:

```text
world/data/unique_legends/unique_legends.json
```

### Events

```text
events/CaptureEventHandler.kt
events/PlayerActivityHandler.kt
events/ServerLifecycleHandler.kt
events/StorageEventHandler.kt
```

These handlers subscribe to Cobblemon and platform events:

- Pokeball hit
- capture calculation
- Pokemon captured
- Pokemon released
- trade completed
- player login
- server lifecycle
- server tick

### Services

```text
service/LegendaryLockService.kt
service/InactivityService.kt
service/PokemonRemovalService.kt
service/ScanService.kt
```

Services contain reusable server logic:

- capture blocking checks
- inactivity release
- party/PC removal
- scanning known players
- repairing stale locks and ownership

### Utilities

```text
util/MessageUtil.kt
util/SpeciesUtil.kt
util/TimeUtil.kt
```

Utilities handle:

- formatted server-side messages
- title broadcasts
- species display names and ids
- time conversion

## Capture Flow

Outside battle:

```text
THROWN_POKEBALL_HIT
  -> check lock
  -> cancel if locked
```

Inside battle:

```text
THROWN_POKEBALL_HIT
  -> do not cancel, to keep Cobblemon battle state healthy

POKE_BALL_CAPTURE_CALCULATED
  -> force failed capture if locked
  -> refund Pokeball
```

After successful capture:

```text
POKEMON_CAPTURED
  -> check tracking rules
  -> register lock
  -> save storage
  -> send chat/title broadcasts
```

## Scan Flow

The scan system collects known player UUIDs from:

- online players
- vanilla `playerdata`
- Cobblemon `pokemon` storage

Then it loads each known player's Cobblemon party and PC through Cobblemon APIs.

`/uniquelegends scan fix` can:

- register missing locks
- release stale locks
- update ownership after trades
- report duplicates

## Release and Trade Flow

Release:

```text
POKEMON_RELEASED_EVENT_POST
  -> if released Pokemon UUID matches active lock
  -> unlock species
  -> save storage
```

Trade:

```text
TRADE_EVENT_POST
  -> if traded Pokemon UUID matches active lock
  -> update owner UUID/name/lastSeenAt
  -> save storage
```

## Build

Run:

```text
./gradlew build
```

On Windows:

```text
.\gradlew.bat build
```

Build outputs are generated for both Fabric and NeoForge.
