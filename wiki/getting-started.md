# Getting Started

## Requirements

- Minecraft `1.21.1`
- Cobblemon `1.7.0+1.21.1`
- Fabric or NeoForge
- Kotlin loader for the selected platform

## Installation

1. Install Cobblemon on the server.
2. Install Unique Legends on the server.
3. Start the server once.
4. Edit the generated config if needed:

```text
config/unique_legends/server.json
```

5. Reload the config with:

```text
/uniquelegends reload
```

## Default behavior

With the default config, the mod tracks Pokemon with these Cobblemon labels:

```json
"trackedLabels": [
  "legendary",
  "mythical",
  "ultra_beast"
]
```

This means Pokemon such as Articuno, Mewtwo, Mew, and Ultra Beasts are unique by default.

## Existing servers

If the mod is added to a server that already has Cobblemon player data, it can scan existing player storages.

By default, the scan runs automatically on server start when the Unique Legends registry is empty:

```json
"scanExistingPokemonOnServerStart": true,
"scanExistingPokemonOnlyWhenRegistryEmpty": true
```

You can also run a manual scan:

```text
/uniquelegends scan fix
```
