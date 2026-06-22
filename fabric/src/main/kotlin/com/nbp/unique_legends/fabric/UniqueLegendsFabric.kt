package com.nbp.unique_legends.fabric

import com.nbp.unique_legends.UniqueLegends
import com.nbp.unique_legends.commands.UniqueLegendsCommand
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.api.ModInitializer

class UniqueLegendsFabric : ModInitializer {
    override fun onInitialize() {
        UniqueLegends.init()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            UniqueLegendsCommand.register(dispatcher)
        }
    }
}
