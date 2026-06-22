package com.nbp.unique_legends.service

import com.cobblemon.mod.common.pokemon.Pokemon
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry

object LegendaryLockService {
    fun shouldBlockCapture(pokemon: Pokemon): Boolean {
        if (!UniqueLegendsConfigManager.shouldTrackPokemon(pokemon)) {
            return false
        }
        return UniqueLegendRegistry.isLockedOrReserved(pokemon.species.resourceIdentifier.toString())
    }

    fun shouldBlockCapture(speciesId: String): Boolean {
        if (!UniqueLegendsConfigManager.config.enabled) {
            return false
        }
        if (!UniqueLegendsConfigManager.shouldTrackSpecies(speciesId)) {
            return false
        }
        return UniqueLegendRegistry.isLockedOrReserved(speciesId)
    }
}
