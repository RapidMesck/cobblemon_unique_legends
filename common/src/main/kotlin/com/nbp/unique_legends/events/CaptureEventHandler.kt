package com.nbp.unique_legends.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokeball.catching.CaptureContext
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate
import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.service.LegendaryLockService
import com.nbp.unique_legends.util.MessageUtil
import com.nbp.unique_legends.util.SpeciesUtil
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object CaptureEventHandler {
    fun register() {
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe { event ->
            val speciesId = SpeciesUtil.getSpeciesId(event.pokemon.pokemon)
            if (!LegendaryLockService.shouldBlockCapture(event.pokemon.pokemon)) {
                return@subscribe
            }

            val player = event.pokeBall.owner as? ServerPlayer
            val entry = UniqueLegendRegistry.getLockOrReservation(speciesId)
            player?.let {
                MessageUtil.send(
                    it,
                    UniqueLegendsConfigManager.config.messages.blockCapture,
                    SpeciesUtil.placeholders(event.pokemon.pokemon) + mapOf(
                        "owner" to (entry?.ownerName ?: "unknown"),
                        "owner_uuid" to (entry?.ownerUuid ?: ""),
                        "pokemon_uuid" to (entry?.pokemonUuid ?: "")
                    )
                )
            }

            if ((event.pokemon.delegate as? PokemonServerDelegate)?.getBattle() == null) {
                event.cancel()
            }

            UniqueLegends.logger.info(
                "Blocked capture of {} because it is owned by {}.",
                speciesId,
                entry?.ownerName ?: "unknown"
            )
        }

        CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe { event ->
            val speciesId = SpeciesUtil.getSpeciesId(event.pokemonEntity.pokemon)
            if (!UniqueLegendsConfigManager.shouldTrackPokemon(event.pokemonEntity.pokemon)) {
                return@subscribe
            }

            event.ifPlayer { player ->
                if (LegendaryLockService.shouldBlockCapture(event.pokemonEntity.pokemon)) {
                    blockCalculatedCapture(event, player, speciesId)
                    return@ifPlayer
                }

                if (event.captureResult.isSuccessfulCapture) {
                    val reserved = UniqueLegendRegistry.reserveCapture(
                        speciesId = speciesId,
                        ownerUuid = player.uuid,
                        ownerName = player.gameProfile.name,
                        pokemonUuid = event.pokemonEntity.pokemon.uuid
                    )

                    if (!reserved) {
                        blockCalculatedCapture(event, player, speciesId)
                    }
                }
            }
        }

        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            val speciesId = SpeciesUtil.getSpeciesId(event.pokemon)
            if (!UniqueLegendsConfigManager.shouldTrackPokemon(event.pokemon)) {
                return@subscribe
            }

            val entry = UniqueLegendRegistry.registerCapture(
                speciesId = speciesId,
                ownerUuid = event.player.uuid,
                ownerName = event.player.gameProfile.name,
                pokemonUuid = event.pokemon.uuid
            ) ?: return@subscribe

            UniqueLegendStorage.save()
            UniqueLegends.logger.info("Registered {} for {}.", speciesId, entry.ownerName)

            val placeholders = SpeciesUtil.placeholders(event.pokemon) + mapOf(
                "player" to entry.ownerName,
                "owner" to entry.ownerName,
                "owner_uuid" to entry.ownerUuid,
                "pokemon_uuid" to entry.pokemonUuid
            )

            if (UniqueLegendsConfigManager.config.broadcastCapture) {
                MessageUtil.broadcast(
                    event.player.server,
                    UniqueLegendsConfigManager.config.messages.broadcastCapture,
                    placeholders
                )
            }

            if (UniqueLegendsConfigManager.config.titleBroadcastCapture) {
                MessageUtil.broadcastTitle(
                    event.player.server,
                    UniqueLegendsConfigManager.config.messages.captureTitle,
                    UniqueLegendsConfigManager.config.messages.captureSubtitle,
                    placeholders
                )
            }
        }
    }

    private fun refundPokeBall(player: ServerPlayer, itemStack: ItemStack) {
        if (player.isCreative || itemStack.isEmpty) {
            return
        }

        if (!player.inventory.add(itemStack)) {
            player.drop(itemStack, false)
        }
    }

    private fun blockCalculatedCapture(
        event: com.cobblemon.mod.common.api.events.pokeball.PokeBallCaptureCalculatedEvent,
        player: ServerPlayer,
        speciesId: String
    ) {
        event.captureResult = CaptureContext(
            numberOfShakes = 0,
            isSuccessfulCapture = false,
            isCriticalCapture = false
        )

        val entry = UniqueLegendRegistry.getLockOrReservation(speciesId)
        MessageUtil.send(
            player,
            UniqueLegendsConfigManager.config.messages.blockCapture,
            SpeciesUtil.placeholders(event.pokemonEntity.pokemon) + mapOf(
                "owner" to (entry?.ownerName ?: "unknown"),
                "owner_uuid" to (entry?.ownerUuid ?: ""),
                "pokemon_uuid" to (entry?.pokemonUuid ?: "")
            )
        )
        refundPokeBall(player, ItemStack(event.pokeBallEntity.pokeBall.item()))
    }
}
