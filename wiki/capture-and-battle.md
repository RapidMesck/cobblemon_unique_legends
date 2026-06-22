# Capture, Battle, and Pokeballs

## Normal wild capture

Outside battle, if a Pokeball hits a locked species, the capture is canceled immediately.

The player receives the configured blocked capture message.

## Battle capture

Inside a Cobblemon battle, the mod does not cancel the first Pokeball hit event.

This is intentional.

Cobblemon creates a battle capture action before the hit event. Canceling that event during battle can leave the battle HUD in a bad state.

Instead, Unique Legends lets the battle capture action continue and forces the capture result to fail during the capture calculation event.

This keeps the Cobblemon battle flow healthy.

## Pokeball refund

When a locked battle capture is forced to fail, the Pokeball is returned to the player.

If the inventory is full, the Pokeball is dropped near the player.

Creative players do not receive refunded Pokeballs.

## Client battle UI

The Cobblemon battle UI still shows the Capture option.

This mod is server-side only. Removing that button would require client-side UI changes and client-installed code or a custom client resource/integration.
