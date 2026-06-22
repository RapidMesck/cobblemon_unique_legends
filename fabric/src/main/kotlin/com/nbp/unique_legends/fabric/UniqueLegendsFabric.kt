package com.nbp.unique_legends.fabric

import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.commands.UniqueLegendsCommand
import com.nbp.unique_legends.gui.UniqueLegendsListGui
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.api.ModInitializer

class UniqueLegendsFabric : ModInitializer {
    override fun onInitialize() {
        UniqueLegends.init()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            UniqueLegendsCommand.register(dispatcher)
        }
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, sender, _ ->
            !UniqueLegendsListGui.handleChat(sender, message.signedContent())
        }
    }
}
