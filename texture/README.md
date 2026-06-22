# Unique Legends Optional Texture Pack

This folder is a starter resource pack for custom GUI icons.

The mod already sends `CustomModelData` on GUI items, so this pack can replace the visuals without changing server config.

## CustomModelData Map

Pokemon lock entries use `minecraft:nether_star`:

| Item | CustomModelData |
| --- | ---: |
| Locked Pokemon | National Pokedex number |
| Locked Pokemon fallback | 900009 |

GUI controls:

| Control | Item | CustomModelData |
| --- | --- | ---: |
| Search by player | `minecraft:player_head` | 900001 |
| Search by Pokemon | `minecraft:player_head` | 900002 |
| Show all / clear filter | `minecraft:compass` | 900003 |
| Previous page | `minecraft:arrow` | 900004 |
| Next page | `minecraft:arrow` | 900005 |
| Page info | `minecraft:book` | 900006 |
| No locks found | `minecraft:barrier` | 900007 |
| Filler / border | `minecraft:gray_stained_glass_pane` | 900008 |

## Test Textures

This pack currently includes active test textures with fixed colors. They are intentionally simple 16x16 PNGs so you can quickly confirm that each GUI item is matching the expected `CustomModelData`.

Current Pokemon examples:

| Pokemon | CustomModelData | Test color |
| --- | ---: | --- |
| Articuno | 144 | Light blue |
| Zapdos | 145 | Yellow |
| Moltres | 146 | Orange |
| Mewtwo | 150 | Purple |
| Mew | 151 | Pink |
| Lugia | 249 | Silver |
| Ho-Oh | 250 | Orange |
| Rayquaza | 384 | Green |

## Download Pokemon Icons

Use the project script to download numeric Pokemon icons from `PokeAPI/sprites`, resize them into square item textures, create one model JSON per Pokemon, and regenerate `assets/minecraft/models/item/nether_star.json`.

From the repository root:

```powershell
.\scripts\download-pokeapi-sprites.ps1
```

Useful options:

```powershell
.\scripts\download-pokeapi-sprites.ps1 -StartDex 1 -EndDex 1025
.\scripts\download-pokeapi-sprites.ps1 -StartDex 144 -EndDex 151 -Overwrite
.\scripts\download-pokeapi-sprites.ps1 -TextureSize 32
.\scripts\download-pokeapi-sprites.ps1 -TextureSize 32 -Padding 1 -Overwrite
```

Sprites are cropped to their non-transparent content before resizing, so they fill the item slot better.

The script tries these PokeAPI sprite paths in order:

```text
sprites/pokemon/versions/generation-viii/icons/{dex}.png
sprites/pokemon/versions/generation-vii/icons/{dex}.png
sprites/pokemon/{dex}.png
```

After running it, check:

```text
downloaded-pokeapi-sprites.txt
missing-pokeapi-sprites.txt
```

## Folder Layout

Put final PNG textures here:

```text
assets/minecraft/textures/item/unique_legends/gui/
assets/minecraft/textures/item/unique_legends/pokemon/
```

Put custom item models here:

```text
assets/minecraft/models/item/unique_legends/gui/
assets/minecraft/models/item/unique_legends/pokemon/
```

The active override files are already in `assets/minecraft/models/item/`.

The `_templates` folder is kept as a reference copy.
