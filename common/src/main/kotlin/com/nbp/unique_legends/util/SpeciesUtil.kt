package com.nbp.unique_legends.util

import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.pokemon.Species
import net.minecraft.resources.ResourceLocation

object SpeciesUtil {
    fun getSpeciesId(pokemon: Pokemon): String {
        return pokemon.species.resourceIdentifier.toString().lowercase()
    }

    fun getSpeciesName(pokemon: Pokemon): String {
        return pokemon.species.name
    }

    fun getSpeciesName(speciesId: String): String {
        return speciesId
            .substringAfter(':')
            .split('_', '-', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
            .ifBlank { speciesId }
    }

    fun getNationalPokedexNumber(speciesId: String): Int? {
        val species = getSpecies(speciesId) ?: return null
        return species.nationalPokedexNumber.takeIf { it > 0 }
    }

    fun getSpecies(speciesId: String): Species? {
        val resourceLocation = ResourceLocation.tryParse(speciesId) ?: return null
        return PokemonSpecies.getByIdentifier(resourceLocation)
    }

    fun placeholders(speciesId: String): Map<String, Any?> {
        return mapOf(
            "species" to getSpeciesName(speciesId),
            "species_name" to getSpeciesName(speciesId),
            "species_id" to speciesId
        )
    }

    fun placeholders(pokemon: Pokemon): Map<String, Any?> {
        val speciesId = getSpeciesId(pokemon)
        return mapOf(
            "species" to getSpeciesName(pokemon),
            "species_name" to getSpeciesName(pokemon),
            "species_id" to speciesId
        )
    }
}
