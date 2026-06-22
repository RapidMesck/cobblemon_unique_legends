package com.nbp.unique_legends.service

import com.cobblemon.mod.common.Cobblemon
import com.nbp.unique_legends.UniqueLegends
import net.minecraft.server.MinecraftServer
import java.util.UUID

object PokemonRemovalService {
    fun removePokemonFromPlayerStorage(
        server: MinecraftServer,
        playerUuid: UUID,
        pokemonUuid: UUID
    ): Boolean {
        return try {
            val registryAccess = server.registryAccess()
            val party = Cobblemon.storage.getParty(playerUuid, registryAccess)
            party[pokemonUuid]?.let { pokemon ->
                return party.remove(pokemon)
            }

            val pc = Cobblemon.storage.getPC(playerUuid, registryAccess)
            pc[pokemonUuid]?.let { pokemon ->
                return pc.remove(pokemon)
            }

            false
        } catch (exception: Exception) {
            UniqueLegends.logger.warn("Could not remove pokemon $pokemonUuid from player $playerUuid storage.", exception)
            false
        }
    }
}
