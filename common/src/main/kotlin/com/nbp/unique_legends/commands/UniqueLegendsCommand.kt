package com.nbp.unique_legends.commands

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.data.UniqueLegendStorage
import com.nbp.unique_legends.service.InactivityService
import com.nbp.unique_legends.service.ScanService
import com.nbp.unique_legends.util.MessageUtil
import com.nbp.unique_legends.util.SpeciesUtil
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object UniqueLegendsCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = Commands.literal("uniquelegends")
            .requires { source -> source.hasPermission(2) }
            .then(Commands.literal("reload").executes { context ->
                UniqueLegendsConfigManager.load()
                UniqueLegendStorage.load(context.source.server)
                context.source.sendSuccess({ MessageUtil.component(UniqueLegendsConfigManager.config.messages.commandReload) }, false)
                1
            })
            .then(Commands.literal("list").executes { context ->
                val entries = UniqueLegendRegistry.all()
                if (entries.isEmpty()) {
                    context.source.sendSuccess({ MessageUtil.component(UniqueLegendsConfigManager.config.messages.commandListEmpty) }, false)
                } else {
                    entries.forEach { entry ->
                        context.source.sendSuccess({
                            MessageUtil.component(
                                UniqueLegendsConfigManager.config.messages.commandListEntry,
                                SpeciesUtil.placeholders(entry.speciesId) + mapOf(
                                    "owner" to entry.ownerName,
                                    "owner_uuid" to entry.ownerUuid,
                                    "pokemon_uuid" to entry.pokemonUuid
                                )
                            )
                        }, false)
                    }
                }
                entries.size
            })
            .then(Commands.literal("info")
                .then(Commands.argument("species", SpeciesArgumentType.species()).executes { context ->
                    val species = getSpeciesId(context, "species")
                    val entry = UniqueLegendRegistry.getEntry(species)
                    if (entry == null || !entry.active) {
                        context.source.sendSuccess({
                            MessageUtil.component(
                                UniqueLegendsConfigManager.config.messages.commandInfoUnlocked,
                                SpeciesUtil.placeholders(species)
                            )
                        }, false)
                        0
                    } else {
                        context.source.sendSuccess({
                            MessageUtil.component(
                                UniqueLegendsConfigManager.config.messages.commandInfoEntry,
                                SpeciesUtil.placeholders(entry.speciesId) + mapOf(
                                    "owner" to entry.ownerName,
                                    "owner_uuid" to entry.ownerUuid,
                                    "pokemon_uuid" to entry.pokemonUuid
                                )
                            )
                        }, false)
                        1
                    }
                })
            )
            .then(Commands.literal("unlock")
                .then(Commands.argument("species", SpeciesArgumentType.species()).executes { context ->
                    val species = getSpeciesId(context, "species")
                    val entry = UniqueLegendRegistry.unlock(species)
                    if (entry == null) {
                        context.source.sendSuccess({
                            MessageUtil.component(
                                UniqueLegendsConfigManager.config.messages.commandUnlockMissing,
                                SpeciesUtil.placeholders(species)
                            )
                        }, false)
                        0
                    } else {
                        UniqueLegendStorage.save()
                        context.source.sendSuccess({
                            MessageUtil.component(
                                UniqueLegendsConfigManager.config.messages.commandUnlockSuccess,
                                SpeciesUtil.placeholders(entry.speciesId) + mapOf(
                                    "owner" to entry.ownerName,
                                    "owner_uuid" to entry.ownerUuid,
                                    "pokemon_uuid" to entry.pokemonUuid
                                )
                            )
                        }, true)
                        1
                    }
                })
            )
            .then(Commands.literal("check-inactive").executes { context ->
                val released = InactivityService.checkInactiveOwners(context.source.server)
                context.source.sendSuccess({
                    MessageUtil.component(
                        UniqueLegendsConfigManager.config.messages.commandCheckInactive,
                        mapOf("released" to released)
                    )
                }, true)
                released
            })
            .then(Commands.literal("scan").executes { context ->
                val result = ScanService.scanKnownPlayers(context.source.server, fix = false)
                sendScanResult(context.source, result)
                result.scanned
            }.then(Commands.literal("fix").executes { context ->
                val result = ScanService.scanKnownPlayers(context.source.server, fix = true)
                sendScanResult(context.source, result)
                result.scanned
            }))
            .then(Commands.literal("debug").executes { context ->
                val config = UniqueLegendsConfigManager.config
                context.source.sendSuccess({
                    MessageUtil.component(
                        config.messages.commandDebugStatus,
                        mapOf(
                            "enabled" to config.enabled,
                            "tracked" to config.trackedPokemons.size,
                            "untracked" to config.untrackedPokemons.size,
                            "tracked_labels" to config.trackedLabels.size,
                            "untracked_labels" to config.untrackedLabels.size,
                            "locks" to UniqueLegendRegistry.all().size
                        )
                    )
                }, false)
                1
            })

        val node = dispatcher.register(root)
        dispatcher.register(alias(node, "ul"))
        dispatcher.register(alias(node, "legendarylock"))
    }

    private fun getSpeciesId(context: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, name: String): String {
        return SpeciesArgumentType.getPokemon(context, name).resourceIdentifier.toString()
    }

    private fun sendScanResult(source: CommandSourceStack, result: com.nbp.unique_legends.service.ScanResult) {
        source.sendSuccess({
            MessageUtil.component(
                UniqueLegendsConfigManager.config.messages.commandScanSummary,
                mapOf(
                    "players" to result.players,
                    "scanned" to result.scanned,
                    "registered" to result.registered,
                    "stale_locks" to result.staleLocks,
                    "ownership_updates" to result.ownershipUpdates,
                    "duplicates" to result.duplicates.size
                )
            )
        }, true)
        result.duplicates.take(10).forEach { duplicate ->
            source.sendSuccess({
                MessageUtil.component(
                    UniqueLegendsConfigManager.config.messages.commandScanDuplicate,
                    mapOf("duplicate" to duplicate)
                )
            }, false)
        }
    }

    private fun alias(
        node: com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack>,
        name: String
    ): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = Commands.literal(name)
            .requires(node.requirement)
            .forward(node.redirect, node.redirectModifier, node.isFork)
            .executes(node.command)
        node.children.forEach { child ->
            builder.then(child)
        }
        return builder
    }
}
