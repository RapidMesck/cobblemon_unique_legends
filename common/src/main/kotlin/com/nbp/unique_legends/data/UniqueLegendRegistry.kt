package com.nbp.unique_legends.data

import java.util.UUID

object UniqueLegendRegistry {
    private val entries = linkedMapOf<String, UniqueLegendEntry>()

    @Synchronized
    fun replaceAll(newEntries: Collection<UniqueLegendEntry>) {
        entries.clear()
        newEntries.forEach { entry ->
            entries[normalize(entry.speciesId)] = entry.copy(speciesId = normalize(entry.speciesId))
        }
    }

    @Synchronized
    fun isLocked(speciesId: String): Boolean {
        return entries[normalize(speciesId)]?.active == true
    }

    @Synchronized
    fun getEntry(speciesId: String): UniqueLegendEntry? {
        return entries[normalize(speciesId)]?.copy()
    }

    @Synchronized
    fun registerCapture(
        speciesId: String,
        ownerUuid: UUID,
        ownerName: String,
        pokemonUuid: UUID,
        now: Long = System.currentTimeMillis()
    ): UniqueLegendEntry? {
        val normalizedSpecies = normalize(speciesId)
        if (entries[normalizedSpecies]?.active == true) {
            return null
        }

        val entry = UniqueLegendEntry(
            speciesId = normalizedSpecies,
            ownerUuid = ownerUuid,
            ownerName = ownerName,
            pokemonUuid = pokemonUuid,
            capturedAt = now,
            lastSeenAt = now,
            active = true
        )
        entries[normalizedSpecies] = entry
        return entry.copy()
    }

    @Synchronized
    fun putEntry(entry: UniqueLegendEntry): Boolean {
        val normalizedSpecies = normalize(entry.speciesId)
        if (entries[normalizedSpecies]?.active == true) {
            return false
        }

        entries[normalizedSpecies] = entry.copy(speciesId = normalizedSpecies)
        return true
    }

    @Synchronized
    fun unlock(speciesId: String): UniqueLegendEntry? {
        val entry = entries[normalize(speciesId)] ?: return null
        entry.active = false
        return entry.copy()
    }

    @Synchronized
    fun unlockByPokemonUuid(pokemonUuid: UUID): UniqueLegendEntry? {
        val entry = entries.values.firstOrNull { it.active && it.pokemonUuid == pokemonUuid } ?: return null
        entry.active = false
        return entry.copy()
    }

    @Synchronized
    fun updateOwnerByPokemonUuid(
        pokemonUuid: UUID,
        ownerUuid: UUID,
        ownerName: String,
        now: Long = System.currentTimeMillis()
    ): UniqueLegendEntry? {
        val entry = entries.values.firstOrNull { it.active && it.pokemonUuid == pokemonUuid } ?: return null
        entry.ownerUuid = ownerUuid
        entry.ownerName = ownerName
        entry.lastSeenAt = now
        return entry.copy()
    }

    @Synchronized
    fun remove(speciesId: String): UniqueLegendEntry? {
        return entries.remove(normalize(speciesId))?.copy()
    }

    @Synchronized
    fun updateLastSeen(playerUuid: UUID, playerName: String, now: Long = System.currentTimeMillis()): Boolean {
        var changed = false
        entries.values.forEach { entry ->
            if (entry.ownerUuid == playerUuid && entry.active) {
                entry.ownerName = playerName
                entry.lastSeenAt = now
                changed = true
            }
        }
        return changed
    }

    @Synchronized
    fun all(includeInactive: Boolean = false): Collection<UniqueLegendEntry> {
        return entries.values
            .filter { includeInactive || it.active }
            .map { it.copy() }
    }

    private fun normalize(speciesId: String): String = speciesId.lowercase()
}
