package com.nbp.unique_legends

import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.events.CaptureEventHandler
import com.nbp.unique_legends.events.PlayerActivityHandler
import com.nbp.unique_legends.events.ServerLifecycleHandler
import com.nbp.unique_legends.events.StorageEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object UniqueLegends {
    const val MOD_ID = "unique_legends"

    val logger: Logger = LoggerFactory.getLogger("UniqueLegends")

    private var initialized = false

    fun init() {
        if (initialized) {
            return
        }
        initialized = true

        UniqueLegendsConfigManager.load()
        ServerLifecycleHandler.register()
        CaptureEventHandler.register()
        StorageEventHandler.register()
        PlayerActivityHandler.register()

        logger.info("Unique Legends loaded.")
    }
}
