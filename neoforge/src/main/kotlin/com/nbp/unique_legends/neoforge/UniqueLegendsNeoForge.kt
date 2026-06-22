package com.nbp.unique_legends.neoforge

import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.commands.UniqueLegendsCommand
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

@Mod(UniqueLegends.MOD_ID)
class UniqueLegendsNeoForge {
    init {
        UniqueLegends.init()
        NeoForge.EVENT_BUS.addListener(::registerCommands)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        UniqueLegendsCommand.register(event.dispatcher)
    }
}
