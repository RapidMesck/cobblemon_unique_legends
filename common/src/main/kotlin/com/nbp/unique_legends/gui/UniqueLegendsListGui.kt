package com.nbp.unique_legends.gui

import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import com.cobblemon.mod.common.item.PokemonItem
import com.nbp.unique_legends.config.UniqueLegendsConfigManager
import com.nbp.unique_legends.data.UniqueLegendEntry
import com.nbp.unique_legends.data.UniqueLegendRegistry
import com.nbp.unique_legends.util.MessageUtil
import com.nbp.unique_legends.util.SpeciesUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Optional
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max

object UniqueLegendsListGui {
    private const val SIZE = 54
    private const val ENTRIES_PER_PAGE = 28
    private const val MEWTWO_HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWFjMmJlZjYyNjc2OTQ1MDllMzM5ZGFiNDVjZGFkYTBmNWZhMDk3ZTcxNDE3ZDY3ZmU2ZDExNmIxN2UyZjliNSJ9fX0="
    private const val MODEL_SEARCH_PLAYER = 900001
    private const val MODEL_SEARCH_POKEMON = 900002
    private const val MODEL_CLEAR_FILTER = 900003
    private const val MODEL_PREVIOUS_PAGE = 900004
    private const val MODEL_NEXT_PAGE = 900005
    private const val MODEL_PAGE_INFO = 900006
    private const val MODEL_NO_LOCKS = 900007
    private const val MODEL_FILLER = 900008
    private const val MODEL_LOCKED_FALLBACK = 900009
    private val entrySlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )
    private val pendingSearches = ConcurrentHashMap<UUID, SearchMode>()

    fun open(player: ServerPlayer) {
        openFiltered(player, ListFilter.All)
    }

    private fun openFiltered(player: ServerPlayer, filter: ListFilter = ListFilter.All, page: Int = 0) {
        val state = createState(filter, page)
        player.openMenu(
            SimpleMenuProvider(
                { syncId, inventory, _ -> UniqueLegendsListMenu(syncId, inventory, state) },
                MessageUtil.component(
                    UniqueLegendsConfigManager.config.messages.guiTitle,
                    state.titlePlaceholders(),
                    includePrefix = false
                )
            )
        )
    }

    fun handleChat(player: ServerPlayer, rawMessage: String): Boolean {
        val mode = pendingSearches.remove(player.uuid) ?: return false
        val query = rawMessage.trim()
        if (query.equals("cancel", ignoreCase = true)) {
            MessageUtil.send(player, UniqueLegendsConfigManager.config.messages.guiSearchCancelled)
            return true
        }

        if (query.isBlank()) {
            open(player)
            return true
        }

        openFiltered(
            player,
            when (mode) {
                SearchMode.PLAYER -> ListFilter.Player(query)
                SearchMode.POKEMON -> ListFilter.Pokemon(query)
            }
        )
        return true
    }

    private fun beginSearch(player: ServerPlayer, mode: SearchMode) {
        pendingSearches[player.uuid] = mode
        player.closeContainer()
        val message = when (mode) {
            SearchMode.PLAYER -> UniqueLegendsConfigManager.config.messages.guiSearchPromptPlayer
            SearchMode.POKEMON -> UniqueLegendsConfigManager.config.messages.guiSearchPromptPokemon
        }
        MessageUtil.send(player, message)
    }

    private fun createState(filter: ListFilter, requestedPage: Int): ListState {
        val entries = UniqueLegendRegistry.all()
            .filter { filter.matches(it) }
            .sortedBy { SpeciesUtil.getSpeciesName(it.speciesId) }
        val pages = max(1, ceil(entries.size / ENTRIES_PER_PAGE.toDouble()).toInt())
        val page = requestedPage.coerceIn(0, pages - 1)
        return ListState(filter, page, pages, entries)
    }

    private fun render(container: Container, state: ListState) {
        container.clearContent()
        repeat(SIZE) { slot ->
            container.setItem(slot, filler())
        }

        container.setItem(2, button(Items.PLAYER_HEAD.defaultInstance, UniqueLegendsConfigManager.config.messages.guiSearchPlayerButtonName, UniqueLegendsConfigManager.config.messages.guiSearchPlayerButtonLore, customModelData = MODEL_SEARCH_PLAYER))
        container.setItem(4, button(Items.COMPASS.defaultInstance, UniqueLegendsConfigManager.config.messages.guiClearFilterButtonName, UniqueLegendsConfigManager.config.messages.guiClearFilterButtonLore, customModelData = MODEL_CLEAR_FILTER))
        container.setItem(6, button(mewtwoHead(), UniqueLegendsConfigManager.config.messages.guiSearchPokemonButtonName, UniqueLegendsConfigManager.config.messages.guiSearchPokemonButtonLore, customModelData = MODEL_SEARCH_POKEMON))

        val start = state.page * ENTRIES_PER_PAGE
        val pageEntries = state.entries.drop(start).take(ENTRIES_PER_PAGE)
        if (pageEntries.isEmpty()) {
            container.setItem(22, button(Items.BARRIER.defaultInstance, UniqueLegendsConfigManager.config.messages.guiNoLocksName, UniqueLegendsConfigManager.config.messages.guiNoLocksLore, state.commonPlaceholders(), MODEL_NO_LOCKS))
        } else {
            pageEntries.forEachIndexed { index, entry ->
                container.setItem(entrySlots[index], lockedPokemonItem(entry))
            }
        }

        if (state.page > 0) {
            container.setItem(45, button(Items.ARROW.defaultInstance, UniqueLegendsConfigManager.config.messages.guiPreviousPageButtonName, UniqueLegendsConfigManager.config.messages.guiPreviousPageButtonLore, state.commonPlaceholders() + mapOf("target_page" to state.page), MODEL_PREVIOUS_PAGE))
        }
        container.setItem(49, button(Items.BOOK.defaultInstance, UniqueLegendsConfigManager.config.messages.guiPageInfoName, UniqueLegendsConfigManager.config.messages.guiPageInfoLore, state.commonPlaceholders(), MODEL_PAGE_INFO))
        if (state.page < state.pages - 1) {
            container.setItem(53, button(Items.ARROW.defaultInstance, UniqueLegendsConfigManager.config.messages.guiNextPageButtonName, UniqueLegendsConfigManager.config.messages.guiNextPageButtonLore, state.commonPlaceholders() + mapOf("target_page" to state.page + 2), MODEL_NEXT_PAGE))
        }
    }

    private fun lockedPokemonItem(entry: UniqueLegendEntry): ItemStack {
        val placeholders = SpeciesUtil.placeholders(entry.speciesId) + mapOf(
            "owner" to entry.ownerName,
            "owner_uuid" to entry.ownerUuid,
            "pokemon_uuid" to entry.pokemonUuid,
            "captured_at" to formatTime(entry.capturedAt),
            "last_seen_at" to formatTime(entry.lastSeenAt)
        )
        return button(
            pokemonModelItem(entry) ?: Items.NETHER_STAR.defaultInstance,
            UniqueLegendsConfigManager.config.messages.guiLockedPokemonName,
            UniqueLegendsConfigManager.config.messages.guiLockedPokemonLore,
            placeholders,
            if (SpeciesUtil.getSpecies(entry.speciesId) == null) MODEL_LOCKED_FALLBACK else null
        )
    }

    private fun pokemonModelItem(entry: UniqueLegendEntry): ItemStack? {
        val species = SpeciesUtil.getSpecies(entry.speciesId) ?: return null
        return PokemonItem.from(species, emptySet(), 1, null)
    }

    private fun button(
        stack: ItemStack,
        name: String,
        lore: List<String>,
        placeholders: Map<String, Any?> = emptyMap(),
        customModelData: Int? = null
    ): ItemStack {
        stack.set(DataComponents.CUSTOM_NAME, MessageUtil.component(name, placeholders, includePrefix = false))
        stack.set(
            DataComponents.LORE,
            ItemLore(lore.map { MessageUtil.component(it, placeholders, includePrefix = false) })
        )
        if (customModelData != null) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(customModelData))
        }
        return stack
    }

    private fun mewtwoHead(): ItemStack {
        val stack = Items.PLAYER_HEAD.defaultInstance
        val properties = PropertyMap()
        properties.put("textures", Property("textures", MEWTWO_HEAD_TEXTURE))
        stack.set(
            DataComponents.PROFILE,
            ResolvableProfile(Optional.of("Mewtwo"), Optional.empty(), properties)
        )
        return stack
    }

    private fun filler(): ItemStack {
        val stack = Items.GRAY_STAINED_GLASS_PANE.defaultInstance
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "))
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(MODEL_FILLER))
        return stack
    }

    private fun formatTime(time: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US)
        format.timeZone = TimeZone.getDefault()
        return format.format(Date(time))
    }

    private class UniqueLegendsListMenu(
        syncId: Int,
        playerInventory: Inventory,
        private var state: ListState
    ) : AbstractContainerMenu(MenuType.GENERIC_9x6, syncId) {
        private val container = SimpleContainer(SIZE)

        init {
            render(container, state)
            repeat(6) { row ->
                repeat(9) { column ->
                    addSlot(LockedSlot(container, column + row * 9, 8 + column * 18, 18 + row * 18))
                }
            }
            repeat(3) { row ->
                repeat(9) { column ->
                    addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18))
                }
            }
            repeat(9) { column ->
                addSlot(Slot(playerInventory, column, 8 + column * 18, 198))
            }
        }

        override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
            if (slotId in 0 until SIZE) {
                handleGuiClick(slotId, player)
                return
            }
            super.clicked(slotId, button, clickType, player)
        }

        override fun quickMoveStack(player: Player, index: Int): ItemStack {
            return ItemStack.EMPTY
        }

        override fun stillValid(player: Player): Boolean = true

        private fun handleGuiClick(slotId: Int, player: Player) {
            val serverPlayer = player as? ServerPlayer ?: return
            when (slotId) {
                2 -> beginSearch(serverPlayer, SearchMode.PLAYER)
                4 -> open(serverPlayer)
                6 -> beginSearch(serverPlayer, SearchMode.POKEMON)
                45 -> if (state.page > 0) openFiltered(serverPlayer, state.filter, state.page - 1)
                53 -> if (state.page < state.pages - 1) openFiltered(serverPlayer, state.filter, state.page + 1)
            }
        }
    }

    private class LockedSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
        override fun mayPickup(player: Player): Boolean = false
    }

    private data class ListState(
        val filter: ListFilter,
        val page: Int,
        val pages: Int,
        val entries: List<UniqueLegendEntry>
    ) {
        fun titlePlaceholders(): Map<String, Any?> = commonPlaceholders()

        fun commonPlaceholders(): Map<String, Any?> {
            val shown = if (entries.isEmpty()) 0 else entries.drop(page * ENTRIES_PER_PAGE).take(ENTRIES_PER_PAGE).size
            return mapOf(
                "page" to page + 1,
                "pages" to pages,
                "shown" to shown,
                "total" to entries.size,
                "filter" to filter.displayName()
            )
        }
    }

    private enum class SearchMode {
        PLAYER,
        POKEMON
    }

    private sealed interface ListFilter {
        fun matches(entry: UniqueLegendEntry): Boolean
        fun displayName(): String

        data object All : ListFilter {
            override fun matches(entry: UniqueLegendEntry): Boolean = true
            override fun displayName(): String = UniqueLegendsConfigManager.config.messages.guiFilterAll
        }

        data class Player(private val query: String) : ListFilter {
            override fun matches(entry: UniqueLegendEntry): Boolean {
                return entry.ownerName.contains(query, ignoreCase = true) ||
                    entry.ownerUuid.toString().contains(query, ignoreCase = true)
            }

            override fun displayName(): String {
                return UniqueLegendsConfigManager.config.messages.guiFilterPlayer.replace("{query}", query)
            }
        }

        data class Pokemon(private val query: String) : ListFilter {
            override fun matches(entry: UniqueLegendEntry): Boolean {
                return entry.speciesId.contains(query, ignoreCase = true) ||
                    SpeciesUtil.getSpeciesName(entry.speciesId).contains(query, ignoreCase = true)
            }

            override fun displayName(): String {
                return UniqueLegendsConfigManager.config.messages.guiFilterPokemon.replace("{query}", query)
            }
        }
    }
}
