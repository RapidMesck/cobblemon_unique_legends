package com.nbp.unique_legends.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.pokemon.Pokemon
import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.util.MessageUtil
import com.nbp.unique_legends.util.SpeciesUtil
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.server.MinecraftServer
import java.util.UUID

object StorageEventHandler {
    fun register() {
        CobblemonEvents.POKEMON_RELEASED_EVENT_POST.subscribe { event ->
            val speciesId = SpeciesUtil.getSpeciesId(event.pokemon)
            if (!UniqueLegendsConfigManager.shouldTrackPokemon(event.pokemon)) {
                return@subscribe
            }

            val entry = UniqueLegendRegistry.getEntry(speciesId)
            if (entry?.pokemonUuid != event.pokemon.uuid) {
                return@subscribe
            }

            UniqueLegendRegistry.unlock(speciesId)
            UniqueLegendStorage.save()
            MessageUtil.send(
                event.player,
                UniqueLegendsConfigManager.config.messages.releaseUnlocked,
                SpeciesUtil.placeholders(event.pokemon) + mapOf(
                    "player" to event.player.gameProfile.name,
                    "owner" to entry.ownerName,
                    "owner_uuid" to entry.ownerUuid,
                    "pokemon_uuid" to entry.pokemonUuid
                )
            )
            UniqueLegends.logger.info(
                "Released lock for {} because {} released pokemon {}.",
                speciesId,
                event.player.gameProfile.name,
                event.pokemon.uuid
            )
        }

        CobblemonEvents.TRADE_EVENT_POST.subscribe { event ->
            var changed = false
            val server = event.tradeParticipant1.uuid.getPlayerServer() ?: event.tradeParticipant2.uuid.getPlayerServer()

            changed = updateOwnerAfterTrade(
                pokemon = event.tradeParticipant1Pokemon,
                newOwnerUuid = event.tradeParticipant1.uuid,
                newOwnerName = resolvePlayerName(server, event.tradeParticipant1.uuid)
            ) || changed

            changed = updateOwnerAfterTrade(
                pokemon = event.tradeParticipant2Pokemon,
                newOwnerUuid = event.tradeParticipant2.uuid,
                newOwnerName = resolvePlayerName(server, event.tradeParticipant2.uuid)
            ) || changed

            if (changed) {
                UniqueLegendStorage.save()
            }
        }
    }

    private fun updateOwnerAfterTrade(pokemon: Pokemon, newOwnerUuid: UUID, newOwnerName: String): Boolean {
        if (!UniqueLegendsConfigManager.shouldTrackPokemon(pokemon)) {
            return false
        }

        val entry = UniqueLegendRegistry.updateOwnerByPokemonUuid(
            pokemonUuid = pokemon.uuid,
            ownerUuid = newOwnerUuid,
            ownerName = newOwnerName
        ) ?: return false

        UniqueLegends.logger.info(
            "Updated owner for {} ({}) to {} after trade.",
            entry.speciesId,
            pokemon.uuid,
            newOwnerName
        )
        return true
    }

    private fun UUID.getPlayerServer(): MinecraftServer? {
        return getPlayer()?.server
    }

    private fun resolvePlayerName(server: MinecraftServer?, uuid: UUID): String {
        val online = server?.playerList?.getPlayer(uuid)
        if (online != null) {
            return online.gameProfile.name
        }

        return runCatching {
            server?.profileCache?.get(uuid)?.orElse(null)?.name
        }.getOrNull() ?: "Offline-${uuid.toString().take(8)}"
    }
}
