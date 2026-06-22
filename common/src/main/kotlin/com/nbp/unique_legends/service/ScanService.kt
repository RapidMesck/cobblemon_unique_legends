package com.nbp.unique_legends.service

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.util.SpeciesUtil
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.name

object ScanService {
    fun scanOnlinePlayers(server: MinecraftServer, fix: Boolean = false): ScanResult {
        return scanKnownPlayers(server, fix)
    }

    fun scanKnownPlayers(server: MinecraftServer, fix: Boolean = false): ScanResult {
        var scanned = 0
        var registered = 0
        var staleLocks = 0
        var ownershipUpdates = 0
        val duplicates = mutableListOf<String>()
        val candidates = mutableListOf<ScanCandidate>()
        val seenPokemonUuids = mutableSetOf<UUID>()
        val registryAccess = server.registryAccess()
        val players = collectKnownPlayers(server)

        players.forEach { knownPlayer ->
            val party = runCatching { Cobblemon.storage.getParty(knownPlayer.uuid, registryAccess) }
                .onFailure { UniqueLegends.logger.warn("Could not load party store for {}.", knownPlayer.uuid, it) }
                .getOrNull()
            val pc = runCatching { Cobblemon.storage.getPC(knownPlayer.uuid, registryAccess) }
                .onFailure { UniqueLegends.logger.warn("Could not load PC store for {}.", knownPlayer.uuid, it) }
                .getOrNull()

            ((party?.asIterable() ?: emptyList()) + (pc?.asIterable() ?: emptyList())).forEach { pokemon: Pokemon ->
                scanned++
                seenPokemonUuids.add(pokemon.uuid)
                val speciesId = SpeciesUtil.getSpeciesId(pokemon)
                if (!UniqueLegendsConfigManager.shouldTrackPokemon(pokemon)) {
                    return@forEach
                }

                candidates.add(
                    ScanCandidate(
                        speciesId = speciesId,
                        ownerUuid = knownPlayer.uuid,
                        ownerName = knownPlayer.name,
                        pokemonUuid = pokemon.uuid
                    )
                )
            }
        }

        if (fix) {
            UniqueLegendRegistry.all().forEach { entry ->
                if (entry.pokemonUuid !in seenPokemonUuids) {
                    UniqueLegendRegistry.unlock(entry.speciesId)
                    staleLocks++
                }
            }
        }

        candidates.forEach { candidate ->
            val existing = UniqueLegendRegistry.getEntry(candidate.speciesId)
            if (existing == null || !existing.active) {
                if (fix) {
                    val entry = UniqueLegendRegistry.registerCapture(
                        speciesId = candidate.speciesId,
                        ownerUuid = candidate.ownerUuid,
                        ownerName = candidate.ownerName,
                        pokemonUuid = candidate.pokemonUuid
                    )
                    if (entry != null) {
                        registered++
                    }
                }
            } else if (existing.pokemonUuid != candidate.pokemonUuid) {
                duplicates.add(
                    "${candidate.speciesId}: ${existing.ownerName} (${existing.pokemonUuid}) e ${candidate.ownerName} (${candidate.pokemonUuid})"
                )
            } else if (existing.pokemonUuid == candidate.pokemonUuid && existing.ownerUuid != candidate.ownerUuid) {
                if (fix) {
                    val updated = UniqueLegendRegistry.updateOwnerByPokemonUuid(
                        pokemonUuid = candidate.pokemonUuid,
                        ownerUuid = candidate.ownerUuid,
                        ownerName = candidate.ownerName
                    )
                    if (updated != null) {
                        ownershipUpdates++
                    }
                }
            }
        }

        if (fix && (registered > 0 || staleLocks > 0 || ownershipUpdates > 0)) {
            UniqueLegendStorage.save()
        }

        UniqueLegends.logger.info(
            "Scan completed. Players: {}, Pokemon: {}, registered: {}, stale locks: {}, ownership updates: {}, duplicates: {}.",
            players.size,
            scanned,
            registered,
            staleLocks,
            ownershipUpdates,
            duplicates.size
        )

        return ScanResult(
            players = players.size,
            scanned = scanned,
            registered = registered,
            staleLocks = staleLocks,
            ownershipUpdates = ownershipUpdates,
            duplicates = duplicates
        )
    }

    private fun collectKnownPlayers(server: MinecraftServer): Set<KnownPlayer> {
        val players = linkedMapOf<UUID, String>()

        server.playerList.players.forEach { player ->
            players[player.uuid] = player.gameProfile.name
        }

        collectPlayerDataUuids(server).forEach { uuid ->
            players.putIfAbsent(uuid, resolvePlayerName(server, uuid))
        }

        collectCobblemonStorageUuids(server).forEach { uuid ->
            players.putIfAbsent(uuid, resolvePlayerName(server, uuid))
        }

        return players.map { (uuid, name) -> KnownPlayer(uuid, name) }.toSet()
    }

    private fun collectPlayerDataUuids(server: MinecraftServer): Set<UUID> {
        val playerDataPath = server.getWorldPath(LevelResource.ROOT).resolve("playerdata")
        return collectUuidsFromFiles(playerDataPath)
    }

    private fun collectCobblemonStorageUuids(server: MinecraftServer): Set<UUID> {
        val pokemonPath = server.getWorldPath(LevelResource.ROOT).resolve("pokemon")
        return collectUuidsFromFiles(pokemonPath)
    }

    private fun collectUuidsFromFiles(root: Path): Set<UUID> {
        if (!Files.exists(root)) {
            return emptySet()
        }

        return Files.walk(root).use { stream ->
            stream
                .iterator()
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "dat" || it.extension == "json" }
                .mapNotNull { path -> parseUuidFromFileName(path.name) }
                .toSet()
        }
    }

    private fun parseUuidFromFileName(fileName: String): UUID? {
        val stem = fileName.substringBeforeLast(".")
        val uuidText = if (stem.length >= UUID_STRING_LENGTH) {
            stem.take(UUID_STRING_LENGTH)
        } else {
            stem
        }
        return runCatching { UUID.fromString(uuidText) }.getOrNull()
    }

    private fun resolvePlayerName(server: MinecraftServer, uuid: UUID): String {
        val online = server.playerList.getPlayer(uuid)
        if (online != null) {
            return online.gameProfile.name
        }

        return runCatching {
            server.profileCache?.get(uuid)?.orElse(null)?.name
        }.getOrNull() ?: "Offline-${uuid.toString().take(8)}"
    }

    private const val UUID_STRING_LENGTH = 36
}

data class ScanResult(
    val players: Int,
    val scanned: Int,
    val registered: Int,
    val staleLocks: Int,
    val ownershipUpdates: Int,
    val duplicates: List<String>
)

private data class ScanCandidate(
    val speciesId: String,
    val ownerUuid: UUID,
    val ownerName: String,
    val pokemonUuid: UUID
)

private data class KnownPlayer(
    val uuid: UUID,
    val name: String
)
