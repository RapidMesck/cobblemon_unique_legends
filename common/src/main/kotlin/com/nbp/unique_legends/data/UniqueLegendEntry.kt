package com.nbp.unique_legends.data

import java.util.UUID

data class UniqueLegendEntry(
    val speciesId: String,
    var ownerUuid: UUID,
    var ownerName: String,
    val pokemonUuid: UUID,
    val capturedAt: Long,
    var lastSeenAt: Long,
    var active: Boolean = true
)
