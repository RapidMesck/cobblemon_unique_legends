# Troubleshooting

## A locked Pokemon can still be battled

This is expected.

Unique Legends blocks capture, not battle. Players can still battle and defeat locked Pokemon.

## The Capture option still appears in the battle UI

This is expected for a server-side only mod.

The Cobblemon battle UI is client-side. Unique Legends blocks the capture on the server and keeps the battle flow working.

## A Pokeball was used on a locked Pokemon

In battle, the mod forces the capture to fail and refunds the Pokeball.

If the player inventory is full, the Pokeball drops near the player.

## A released Pokemon still appears locked

Run:

```text
/uniquelegends scan fix
```

This should release stale locks if the Pokemon UUID is no longer found.

## A traded Pokemon still shows the old owner

Run:

```text
/uniquelegends scan fix
```

The scan can repair owner data by finding the registered Pokemon UUID in the new owner's storage.

## Existing players are not detected

The scan can only find players with known files in:

```text
world/playerdata
world/pokemon
```

If a player has no playerdata and no Cobblemon storage file, there is no UUID to scan.

## Config changes did not apply

Run:

```text
/uniquelegends reload
```

If the issue continues, check the server log for config parse errors.
