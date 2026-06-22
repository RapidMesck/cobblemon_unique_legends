package com.nbp.unique_legends.util

import com.cobblemon.mod.common.pokemon.Pokemon

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
