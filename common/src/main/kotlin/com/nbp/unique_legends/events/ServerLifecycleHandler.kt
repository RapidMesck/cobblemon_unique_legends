package com.nbp.unique_legends.events

import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.service.InactivityService
import com.nbp.unique_legends.service.ScanService
import com.nbp.unique_legends.util.TimeUtil

object ServerLifecycleHandler {
    private var ticksUntilInactiveCheck: Long = 0L

    fun register() {
        PlatformEvents.SERVER_STARTING.subscribe { event ->
            UniqueLegendsConfigManager.load()
            UniqueLegendStorage.load(event.server)
        }

        PlatformEvents.SERVER_STARTED.subscribe { event ->
            val config = UniqueLegendsConfigManager.config
            if (config.scanExistingPokemonOnServerStart) {
                val shouldScan = !config.scanExistingPokemonOnlyWhenRegistryEmpty || UniqueLegendRegistry.all().isEmpty()
                if (shouldScan) {
                    ScanService.scanKnownPlayers(event.server, fix = true)
                }
            }
            if (config.checkInactiveOnServerStart) {
                InactivityService.checkInactiveOwners(event.server)
            }
            ticksUntilInactiveCheck = TimeUtil.minutesToTicks(config.checkInactiveIntervalMinutes)
        }

        PlatformEvents.SERVER_TICK_POST.subscribe { event ->
            val config = UniqueLegendsConfigManager.config
            if (!config.releaseOnInactive) {
                return@subscribe
            }

            ticksUntilInactiveCheck--
            if (ticksUntilInactiveCheck <= 0L) {
                InactivityService.checkInactiveOwners(event.server)
                ticksUntilInactiveCheck = TimeUtil.minutesToTicks(config.checkInactiveIntervalMinutes)
            }
        }
    }
}
