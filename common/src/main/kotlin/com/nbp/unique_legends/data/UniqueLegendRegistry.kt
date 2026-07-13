package com.nbp.unique_legends.data

import java.util.UUID

object UniqueLegendRegistry {
    private val entries = linkedMapOf<String, UniqueLegendEntry>()
    private val reservations = linkedMapOf<String, UniqueLegendReservation>()
    private const val RESERVATION_TTL_MS = 30_000L

    @Synchronized
    fun replaceAll(newEntries: Collection<UniqueLegendEntry>) {
        entries.clear()
        reservations.clear()
        newEntries.forEach { entry ->
            entries[normalize(entry.speciesId)] = entry.copy(speciesId = normalize(entry.speciesId))
        }
    }

    @Synchronized
    fun isLocked(speciesId: String): Boolean {
        clearExpiredReservations()
        return entries[normalize(speciesId)]?.active == true
    }

    @Synchronized
    fun isLockedOrReserved(speciesId: String): Boolean {
        clearExpiredReservations()
        val normalizedSpecies = normalize(speciesId)
        return entries[normalizedSpecies]?.active == true || reservations[normalizedSpecies] != null
    }

    @Synchronized
    fun getLockOrReservation(speciesId: String): UniqueLegendEntry? {
        clearExpiredReservations()
        val normalizedSpecies = normalize(speciesId)
        entries[normalizedSpecies]?.takeIf { it.active }?.let { return it.copy() }
        return reservations[normalizedSpecies]?.let { reservation ->
            UniqueLegendEntry(
                speciesId = reservation.speciesId,
                ownerUuid = reservation.ownerUuid,
                ownerName = reservation.ownerName,
                pokemonUuid = reservation.pokemonUuid,
                capturedAt = reservation.reservedAt,
                lastSeenAt = reservation.reservedAt,
                active = true
            )
        }
    }

    @Synchronized
    fun reserveCapture(
        speciesId: String,
        ownerUuid: UUID,
        ownerName: String,
        pokemonUuid: UUID,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        clearExpiredReservations(now)
        val normalizedSpecies = normalize(speciesId)
        if (entries[normalizedSpecies]?.active == true || reservations[normalizedSpecies] != null) {
            return false
        }

        reservations[normalizedSpecies] = UniqueLegendReservation(
            speciesId = normalizedSpecies,
            ownerUuid = ownerUuid,
            ownerName = ownerName,
            pokemonUuid = pokemonUuid,
            reservedAt = now,
            expiresAt = now + RESERVATION_TTL_MS
        )
        return true
    }

    @Synchronized
    fun getEntry(speciesId: String): UniqueLegendEntry? {
        clearExpiredReservations()
        return entries[normalize(speciesId)]?.copy()
    }

    @Synchronized
    fun getEntriesByPokemonUuid(pokemonUuid: UUID): List<UniqueLegendEntry> {
        clearExpiredReservations()
        return entries.values
            .filter { it.active && it.pokemonUuid == pokemonUuid }
            .map { it.copy() }
    }

    @Synchronized
    fun registerCapture(
        speciesId: String,
        ownerUuid: UUID,
        ownerName: String,
        pokemonUuid: UUID,
        now: Long = System.currentTimeMillis()
    ): UniqueLegendEntry? {
        clearExpiredReservations(now)
        val normalizedSpecies = normalize(speciesId)
        reservations.remove(normalizedSpecies)
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

    /**
     * Registers a lock for an evolved form. Unlike [registerCapture], this allows the same
     * [pokemonUuid] to exist under a different [speciesId] (evolution chain).
     */
    @Synchronized
    fun registerEvolution(
        speciesId: String,
        ownerUuid: UUID,
        ownerName: String,
        pokemonUuid: UUID,
        capturedAt: Long,
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
            capturedAt = capturedAt,
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
        val normalizedSpecies = normalize(speciesId)
        reservations.remove(normalizedSpecies)
        val entry = entries[normalizedSpecies] ?: return null
        entry.active = false
        return entry.copy()
    }

    /**
     * Unlocks a single entry by speciesId. Use [unlockChainByPokemonUuid] to unlock an entire evolution chain.
     */
    @Synchronized
    fun unlockByPokemonUuid(pokemonUuid: UUID): UniqueLegendEntry? {
        val entry = entries.values.firstOrNull { it.active && it.pokemonUuid == pokemonUuid } ?: return null
        entry.active = false
        return entry.copy()
    }

    /**
     * Unlocks all entries in the evolution chain sharing the same [pokemonUuid].
     * Returns the list of unlocked entries.
     */
    @Synchronized
    fun unlockChainByPokemonUuid(pokemonUuid: UUID): List<UniqueLegendEntry> {
        val chainEntries = entries.values.filter { it.active && it.pokemonUuid == pokemonUuid }
        chainEntries.forEach { it.active = false }
        return chainEntries.map { it.copy() }
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
        val normalizedSpecies = normalize(speciesId)
        reservations.remove(normalizedSpecies)
        return entries.remove(normalizedSpecies)?.copy()
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
        clearExpiredReservations()
        return entries.values
            .filter { includeInactive || it.active }
            .map { it.copy() }
    }

    private fun normalize(speciesId: String): String = speciesId.lowercase()

    private fun clearExpiredReservations(now: Long = System.currentTimeMillis()) {
        reservations.entries.removeIf { (_, reservation) -> reservation.expiresAt <= now }
    }
}
