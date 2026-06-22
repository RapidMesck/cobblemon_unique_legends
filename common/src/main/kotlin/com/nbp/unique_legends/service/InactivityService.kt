package com.nbp.unique_legends.service

import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.util.MessageUtil
import com.nbp.unique_legends.util.SpeciesUtil
import com.nbp.unique_legends.util.TimeUtil
import net.minecraft.server.MinecraftServer

object InactivityService {
    fun checkInactiveOwners(server: MinecraftServer): Int {
        val config = UniqueLegendsConfigManager.config
        if (!config.enabled || !config.releaseOnInactive) {
            return 0
        }

        val maxInactiveMillis = TimeUtil.daysToMillis(config.inactiveDaysToRelease)
        val now = System.currentTimeMillis()
        var released = 0

        UniqueLegendRegistry.all().forEach { entry ->
            val inactiveFor = now - entry.lastSeenAt
            if (inactiveFor < maxInactiveMillis) {
                return@forEach
            }

            val removed = PokemonRemovalService.removePokemonFromPlayerStorage(
                server = server,
                playerUuid = entry.ownerUuid,
                pokemonUuid = entry.pokemonUuid
            )

            if (!removed) {
                UniqueLegends.logger.warn(
                    "Could not find pokemon {} in {} storage during inactivity release.",
                    entry.pokemonUuid,
                    entry.ownerName
                )
            }

            UniqueLegendRegistry.unlock(entry.speciesId)
            released++

            UniqueLegends.logger.info("Released {} due to inactivity from {}.", entry.speciesId, entry.ownerName)

            server.playerList.getPlayer(entry.ownerUuid)?.let { player ->
                MessageUtil.send(
                    player,
                    config.messages.releasedByInactivity,
                    SpeciesUtil.placeholders(entry.speciesId) + mapOf(
                        "owner" to entry.ownerName,
                        "owner_uuid" to entry.ownerUuid,
                        "pokemon_uuid" to entry.pokemonUuid
                    )
                )
            }

            if (config.broadcastReleaseByInactivity) {
                MessageUtil.broadcast(
                    server,
                    config.messages.broadcastReleaseByInactivity,
                    SpeciesUtil.placeholders(entry.speciesId) + mapOf(
                        "owner" to entry.ownerName,
                        "owner_uuid" to entry.ownerUuid,
                        "pokemon_uuid" to entry.pokemonUuid
                    )
                )
            }
        }

        if (released > 0) {
            UniqueLegendStorage.save()
        }

        return released
    }
}
