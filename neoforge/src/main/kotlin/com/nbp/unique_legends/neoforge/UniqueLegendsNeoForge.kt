package com.nbp.unique_legends.neoforge

import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.commands.UniqueLegendsCommand
import com.nbp.unique_legends.gui.UniqueLegendsListGui
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.ServerChatEvent

@Mod(UniqueLegends.MOD_ID)
class UniqueLegendsNeoForge {
    init {
        UniqueLegends.init()
        NeoForge.EVENT_BUS.addListener(::registerCommands)
        NeoForge.EVENT_BUS.addListener(::onServerChat)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        UniqueLegendsCommand.register(event.dispatcher)
    }

    private fun onServerChat(event: ServerChatEvent) {
        if (UniqueLegendsListGui.handleChat(event.player, event.rawText)) {
            event.isCanceled = true
        }
    }
}
