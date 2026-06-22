package com.nbp.unique_legends.events

import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage

object PlayerActivityHandler {
    fun register() {
        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe { event ->
            if (UniqueLegendRegistry.updateLastSeen(event.player.uuid, event.player.gameProfile.name)) {
                UniqueLegendStorage.save()
            }
        }
    }
}
