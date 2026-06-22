package com.nbp.unique_legends.data

import java.util.UUID

data class UniqueLegendReservation(
    val speciesId: String,
    val ownerUuid: UUID,
    val ownerName: String,
    val pokemonUuid: UUID,
    val reservedAt: Long,
    val expiresAt: Long
)
