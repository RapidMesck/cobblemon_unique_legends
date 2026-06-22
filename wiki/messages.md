# Messages and Formatting

Unique Legends is server-side only, so messages are formatted on the server and sent as Minecraft components.

Players do not need a client-side language file.

## Message config

Messages are configured under:

```json
"messages": {}
```

Example:

```json
"messages": {
  "prefix": "&6[Unique Legends]&r ",
  "blockCapture": "&cThis unique Pokemon already belongs to &e{owner}&c.",
  "broadcastCapture": "&6{player}&e captured the unique Pokemon &b{species}&e.",
  "captureTitle": "&6Unique Pokemon Captured",
  "captureSubtitle": "&e{player}&7 claimed &b{species}"
}
```

## Colors and styles

Use Minecraft legacy formatting codes:

```text
&0 black
&1 dark blue
&2 dark green
&3 dark aqua
&4 dark red
&5 dark purple
&6 gold
&7 gray
&8 dark gray
&9 blue
&a green
&b aqua
&c red
&d light purple
&e yellow
&f white
&l bold
&o italic
&n underline
&m strikethrough
&r reset
```

## Placeholders

Common placeholders:

```text
{species}      Friendly species name, such as Articuno
{species_name} Friendly species name, such as Articuno
{species_id}   Full id, such as cobblemon:articuno
{player}       Player name
{owner}        Lock owner name
{owner_uuid}   Lock owner UUID
{pokemon_uuid} Pokemon UUID
```

Scan placeholders:

```text
{players}
{scanned}
{registered}
{stale_locks}
{ownership_updates}
{duplicates}
{duplicate}
```

Inactivity placeholder:

```text
{released}
```

## Title broadcasts

Enable or disable capture titles:

```json
"titleBroadcastCapture": true
```

Configure the title text:

```json
"captureTitle": "&6Unique Pokemon Captured",
"captureSubtitle": "&e{player}&7 claimed &b{species}"
```

The title broadcast is sent to every online player when a unique Pokemon is captured.
