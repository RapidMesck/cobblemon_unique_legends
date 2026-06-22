package com.nbp.unique_legends.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.cobblemon.mod.common.pokemon.Pokemon
import com.nbp.unique_legends.UniqueLegends
import net.minecraft.resources.ResourceLocation
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object UniqueLegendsConfigManager {
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val configPath: Path = Path.of("config", UniqueLegends.MOD_ID, "server.json")

    var config: UniqueLegendsConfig = UniqueLegendsConfig()
        private set

    fun load(): UniqueLegendsConfig {
        Files.createDirectories(configPath.parent)

        if (!Files.exists(configPath)) {
            config = UniqueLegendsConfig()
            save()
            return config
        }

        config = try {
            Files.newBufferedReader(configPath).use { reader: Reader ->
                gson.fromJson(reader, UniqueLegendsConfig::class.java) ?: UniqueLegendsConfig()
            }
        } catch (exception: Exception) {
            backupCorruptConfig()
            UniqueLegends.logger.error("Failed to load Unique Legends config, using defaults.", exception)
            UniqueLegendsConfig()
        }

        save()
        return config
    }

    fun save() {
        Files.createDirectories(configPath.parent)
        Files.newBufferedWriter(configPath).use { writer: Writer ->
            gson.toJson(config, writer)
        }
    }

    fun shouldTrackSpecies(speciesId: ResourceLocation): Boolean {
        return shouldTrackSpecies(speciesId.toString())
    }

    fun shouldTrackSpecies(speciesId: String): Boolean {
        if (!config.enabled) {
            return false
        }

        val normalizedSpecies = speciesId.lowercase()
        val tracked = config.trackedPokemons.mapTo(mutableSetOf()) { it.lowercase() }
        val untracked = config.untrackedPokemons.mapTo(mutableSetOf()) { it.lowercase() }

        return when {
            tracked.isEmpty() && untracked.isEmpty() -> true
            tracked.isNotEmpty() -> normalizedSpecies in tracked && normalizedSpecies !in untracked
            else -> normalizedSpecies !in untracked
        }
    }

    fun shouldTrackPokemon(pokemon: Pokemon): Boolean {
        if (!config.enabled) {
            return false
        }
        if (config.ignoreLockIfShiny && pokemon.shiny) {
            return false
        }

        val speciesId = pokemon.species.resourceIdentifier.toString().lowercase()
        val tracked = config.trackedPokemons.mapTo(mutableSetOf()) { it.lowercase() }
        val untracked = config.untrackedPokemons.mapTo(mutableSetOf()) { it.lowercase() }
        val trackedLabels = config.trackedLabels.mapTo(mutableSetOf()) { it.lowercase() }
        val untrackedLabels = config.untrackedLabels.mapTo(mutableSetOf()) { it.lowercase() }

        if (speciesId in untracked || pokemon.hasAnyLabel(untrackedLabels)) {
            return false
        }

        return when {
            tracked.isNotEmpty() -> speciesId in tracked
            trackedLabels.isNotEmpty() -> pokemon.hasAnyLabel(trackedLabels)
            untracked.isEmpty() && untrackedLabels.isEmpty() -> true
            else -> true
        }
    }

    private fun Pokemon.hasAnyLabel(labels: Set<String>): Boolean {
        return labels.any { label -> hasLabels(label) }
    }

    private fun backupCorruptConfig() {
        if (!Files.exists(configPath)) {
            return
        }

        runCatching {
            val backupPath = configPath.resolveSibling("server.json.corrupt-${System.currentTimeMillis()}")
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
