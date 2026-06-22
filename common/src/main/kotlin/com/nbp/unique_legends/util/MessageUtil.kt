package com.nbp.unique_legends.util

import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object MessageUtil {
    fun text(message: String): Component = component(message, includePrefix = false)

    fun component(
        template: String,
        placeholders: Map<String, Any?> = emptyMap(),
        includePrefix: Boolean = true
    ): Component {
        val rendered = applyPlaceholders(template, placeholders)
        val prefixed = if (includePrefix) {
            UniqueLegendsConfigManager.config.messages.prefix + rendered
        } else {
            rendered
        }
        return parseLegacyColors(prefixed)
    }

    fun send(player: ServerPlayer, message: String) {
        player.sendSystemMessage(component(message))
    }

    fun send(player: ServerPlayer, message: String, placeholders: Map<String, Any?>) {
        player.sendSystemMessage(component(message, placeholders))
    }

    fun broadcast(server: MinecraftServer, message: String) {
        server.playerList.players.forEach { player ->
            send(player, message)
        }
    }

    fun broadcast(server: MinecraftServer, message: String, placeholders: Map<String, Any?>) {
        server.playerList.players.forEach { player ->
            send(player, message, placeholders)
        }
    }

    fun broadcastTitle(
        server: MinecraftServer,
        title: String,
        subtitle: String,
        placeholders: Map<String, Any?>,
        fadeInTicks: Int = 10,
        stayTicks: Int = 70,
        fadeOutTicks: Int = 20
    ) {
        val titleComponent = component(title, placeholders, includePrefix = false)
        val subtitleComponent = component(subtitle, placeholders, includePrefix = false)
        server.playerList.players.forEach { player ->
            player.connection.send(ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks))
            player.connection.send(ClientboundSetTitleTextPacket(titleComponent))
            player.connection.send(ClientboundSetSubtitleTextPacket(subtitleComponent))
        }
    }

    private fun applyPlaceholders(template: String, placeholders: Map<String, Any?>): String {
        var result = template
        placeholders.forEach { (key, value) ->
            result = result.replace("{$key}", value?.toString() ?: "")
        }
        return result
    }

    private fun parseLegacyColors(message: String): MutableComponent {
        val root = Component.empty()
        val activeFormats = mutableListOf<ChatFormatting>()
        val segment = StringBuilder()
        var index = 0

        fun flushSegment() {
            if (segment.isEmpty()) {
                return
            }
            val text = Component.literal(segment.toString())
            if (activeFormats.isNotEmpty()) {
                text.withStyle(*activeFormats.toTypedArray())
            }
            root.append(text)
            segment.clear()
        }

        while (index < message.length) {
            val char = message[index]
            if (char == '&' && index + 1 < message.length) {
                val formatting = ChatFormatting.getByCode(message[index + 1].lowercaseChar())
                if (formatting != null) {
                    flushSegment()
                    when {
                        formatting == ChatFormatting.RESET -> activeFormats.clear()
                        formatting.isColor -> {
                            activeFormats.removeIf { it.isColor || it == ChatFormatting.RESET }
                            activeFormats.add(formatting)
                        }
                        else -> {
                            activeFormats.remove(formatting)
                            activeFormats.add(formatting)
                        }
                    }
                    index += 2
                    continue
                }
            }
            segment.append(char)
            index++
        }

        flushSegment()
        return root
    }
}
