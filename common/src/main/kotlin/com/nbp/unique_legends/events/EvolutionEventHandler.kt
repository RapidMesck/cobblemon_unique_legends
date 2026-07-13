package com.nbp.unique_legends.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.util.SpeciesUtil

object EvolutionEventHandler {
    fun register() {
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe { event ->
            val evolvedPokemon = event.pokemon
            val sourcePokemon = event.sourcePokemon

            val newSpeciesId = SpeciesUtil.getSpeciesId(evolvedPokemon)
            val oldSpeciesId = SpeciesUtil.getSpeciesId(sourcePokemon)

            val newSpeciesTracked = UniqueLegendsConfigManager.shouldTrackPokemon(evolvedPokemon)
            val oldSpeciesTracked = UniqueLegendsConfigManager.shouldTrackPokemon(sourcePokemon)

            if (!newSpeciesTracked && !oldSpeciesTracked) {
                return@subscribe
            }

            val pokemonUuid = evolvedPokemon.uuid

            if (oldSpeciesTracked) {
                val oldEntry = UniqueLegendRegistry.getEntry(oldSpeciesId)
                if (oldEntry != null && oldEntry.pokemonUuid == pokemonUuid) {
                    if (newSpeciesTracked) {
                        val newEntry = UniqueLegendRegistry.registerEvolution(
                            speciesId = newSpeciesId,
                            ownerUuid = oldEntry.ownerUuid,
                            ownerName = oldEntry.ownerName,
                            pokemonUuid = pokemonUuid,
                            capturedAt = oldEntry.capturedAt
                        )

                        if (newEntry != null) {
                            UniqueLegends.logger.info(
                                "Evolution lock migrated: {} ({}) evolved to {} — registered new lock for {}.",
                                oldSpeciesId,
                                pokemonUuid,
                                newSpeciesId,
                                newEntry.ownerName
                            )
                        }
                    } else {
                        UniqueLegends.logger.info(
                            "Evolution: {} ({}) evolved to {} but new species is not tracked, keeping old lock.",
                            oldSpeciesId,
                            pokemonUuid,
                            newSpeciesId
                        )
                    }

                    handlePreEvolutionUnlock(pokemonUuid, evolvedPokemon)
                }
            } else if (newSpeciesTracked) {
                val existingEntry = UniqueLegendRegistry.getEntry(newSpeciesId)
                if (existingEntry == null || !existingEntry.active) {
                    val ownerUuid = evolvedPokemon.getOwnerUUID() ?: return@subscribe
                    val ownerName = evolvedPokemon.getOwnerPlayer()?.gameProfile?.name ?: "Unknown"
                    val newEntry = UniqueLegendRegistry.registerEvolution(
                        speciesId = newSpeciesId,
                        ownerUuid = ownerUuid,
                        ownerName = ownerName,
                        pokemonUuid = pokemonUuid,
                        capturedAt = System.currentTimeMillis()
                    )

                    if (newEntry != null) {
                        UniqueLegendRegistry.unlockChainByPokemonUuid(pokemonUuid)
                            .filter { it.speciesId != newSpeciesId }
                            .forEach { cleared ->
                                UniqueLegends.logger.info(
                                    "Cleared stale lock for {} (same pokemonUuid {}) after registering {}.",
                                    cleared.speciesId,
                                    pokemonUuid,
                                    newSpeciesId
                                )
                            }

                        UniqueLegendStorage.save()
                        UniqueLegends.logger.info(
                            "Registered new evolution lock for {} ({}) — {}.",
                            newSpeciesId,
                            pokemonUuid,
                            newEntry.ownerName
                        )
                    }
                }
            }
        }
    }

    /**
     * If configured to [EvolutionUnlockMode.TERMINAL], unlocks pre-evolutions when the Pokémon
     * reaches a species that has no further evolutions.
     */
    private fun handlePreEvolutionUnlock(
        pokemonUuid: java.util.UUID,
        evolvedPokemon: com.cobblemon.mod.common.pokemon.Pokemon
    ) {
        val mode = UniqueLegendsConfigManager.config.evolutionUnlockMode.uppercase()
        if (mode != "TERMINAL") {
            return
        }

        val hasFurtherEvolutions = evolvedPokemon.evolutions.any()
        if (hasFurtherEvolutions) {
            return
        }

        val chainEntries = UniqueLegendRegistry.getEntriesByPokemonUuid(pokemonUuid)
        val currentSpeciesId = SpeciesUtil.getSpeciesId(evolvedPokemon)

        chainEntries
            .filter { it.speciesId != currentSpeciesId }
            .forEach { entry ->
                UniqueLegendRegistry.unlock(entry.speciesId)
                UniqueLegends.logger.info(
                    "Terminal evolution reached: unlocked pre-evolution {} (chain uuid {}).",
                    entry.speciesId,
                    pokemonUuid
                )
            }

        UniqueLegendStorage.save()
    }
}
