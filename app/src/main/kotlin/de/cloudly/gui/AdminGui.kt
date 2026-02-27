package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * General administration dashboard showing tracked players with quick actions.
 */
class AdminGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val initialPage: Int = 0
) : Listener {

    private var inventory: Inventory? = null
    private var currentPage = initialPage
    private val playersPerPage = PLAYER_CARD_SLOTS.size
    private var trackedPlayers: List<WhitelistPlayer> = emptyList()
    private var allPlayers: List<WhitelistPlayer> = emptyList()
    private var searchQuery: String? = null
    private var cleanedUp = false
    private var awaitingSearchInput = false
    private var searchClosingForInput = false

    companion object {
        private const val INVENTORY_SIZE = 54
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val REFRESH_MATERIAL = Material.CLOCK
        private val PENDING_MATERIAL = Material.WRITABLE_BOOK
        private val SEARCH_MATERIAL = Material.SPYGLASS
        private val CARD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        private val PLAYER_CARD_SLOTS = listOf(
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42
        )

        private val layout = GuiLayout(6).apply {
            setSlot("search", 0, 0)
            setSlot("refresh", 0, 8)

            setSlot("prev_page", 5, 2)
            setSlot("pending", 5, 4)
            setSlot("next_page", 5, 6)
        }
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        loadTrackedPlayers()
        createInventory()
        updateInventory()
        inventory?.let(viewer::openInventory)
    }

    private fun loadTrackedPlayers() {
        allPlayers = plugin.getWhitelistService().getAllPlayers()
        trackedPlayers = filterPlayers(allPlayers)
    }

    private fun createInventory() {
        val title = Messages.Gui.Admin.title(trackedPlayers.size)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        decorateInventory(inv)

        if (trackedPlayers.isEmpty()) {
            currentPage = 0
        }

        val totalPages = getTotalPages()
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }

        val pendingCount = plugin.getWhitelistAttemptService().getAttempts().size
        renderHeader(inv)
        renderPlayerGrid(inv)
        renderFooter(inv, pendingCount, totalPages)
    }

    private fun decorateInventory(inv: Inventory) {
        GuiTheme.applyFrame(inv)

        val headerSkip = columnsForHeader()
        GuiTheme.applyRow(inv, 0, GuiTheme.pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7"), headerSkip)

        val borderFiller = GuiTheme.pane(Material.BLACK_STAINED_GLASS_PANE, "§0")
        val innerColumns = (2..6).toSet()
        for (row in 1..4) {
            GuiTheme.applyRow(inv, row, borderFiller, innerColumns)
        }

        val footerSkip = columnsForFooter()
        GuiTheme.applyRow(inv, 5, GuiTheme.standardFiller(), footerSkip)
    }

    private fun columnsForHeader(): Set<Int> {
        val columns = mutableSetOf(0, 8, 4)
        listOf("search", "refresh")
            .filter { layout.hasSlot(it) }
            .mapTo(columns) { layout.getSlot(it) % 9 }
        return columns
    }

    private fun columnsForFooter(): Set<Int> {
        val columns = mutableSetOf(0, 8)
        listOf("prev_page", "pending", "next_page")
            .filter { layout.hasSlot(it) }
            .mapTo(columns) { layout.getSlot(it) % 9 }
        return columns
    }

    private fun renderHeader(inv: Inventory) {
        inv.setItem(layout.getSlot("search"), createSearchItem())
        inv.setItem(layout.getSlot("refresh"), createRefreshItem())
    }

    private fun renderPlayerGrid(inv: Inventory) {
        if (trackedPlayers.isEmpty()) {
            val placeholderSlot = PLAYER_CARD_SLOTS[PLAYER_CARD_SLOTS.size / 2]
            inv.setItem(placeholderSlot, createEmptyStateItem())
            return
        }

        val startIndex = currentPage * playersPerPage
        val endIndex = minOf(startIndex + playersPerPage, trackedPlayers.size)

        var slotCursor = 0
        for (index in startIndex until endIndex) {
            if (slotCursor >= PLAYER_CARD_SLOTS.size) break
            val slotIndex = PLAYER_CARD_SLOTS[slotCursor]
            inv.setItem(slotIndex, createPlayerItem(trackedPlayers[index]))
            slotCursor++
        }
    }

    private fun renderFooter(inv: Inventory, pendingCount: Int, totalPages: Int) {
        if (currentPage > 0) {
            inv.setItem(layout.getSlot("prev_page"), createPrevPageItem())
        }

        inv.setItem(layout.getSlot("pending"), createPendingItem(pendingCount))

        if (currentPage < totalPages - 1) {
            inv.setItem(layout.getSlot("next_page"), createNextPageItem())
        }

        val footerRow = 5
        val base = footerRow * 9
        for (col in 0..8) {
            val slot = base + col
            if (slot >= inv.size) break
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, GuiTheme.standardFiller())
            }
        }
    }

    private fun createPlayerItem(player: WhitelistPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        val isOp = offlinePlayer.isOp
        val isOnline = Bukkit.getPlayer(player.uuid)?.isOnline == true
        val addedByName = resolveAddedByName(player)
        val addedAt = CARD_DATE_FORMAT.format(player.addedAt.atZone(ZoneId.systemDefault()))

        item.itemMeta = (item.itemMeta as SkullMeta).apply {
            owningPlayer = offlinePlayer
            setDisplayName(Messages.Gui.Admin.playerLabel(player.username))

            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.Admin.playerOnlineStatus(isOnline))
            if (isOp) {
                lore.add(Messages.Gui.Admin.PLAYER_OP_STATUS)
            }
            lore.add("§8§m────────────")
            lore.add(Messages.Gui.Admin.infoUuid(player.uuid.toString()))
            lore.add(Messages.Gui.Admin.playerAddedBy(addedByName))
            lore.add(Messages.Gui.Admin.playerAddedOn(addedAt))
            player.reason?.takeIf { it.isNotBlank() }?.let { lore.add(Messages.Gui.Admin.playerReason(it)) }

            val discord = player.discordConnection
            when {
                discord == null -> lore.add(Messages.Gui.Admin.PLAYER_DISCORD_NOT_CONNECTED)
                discord.verified -> lore.add(Messages.Gui.Admin.playerDiscordVerified(discord.discordUsername))
                else -> lore.add(Messages.Gui.Admin.playerDiscordConnected(discord.discordUsername))
            }

            lore.add("§8§m────────────")
            lore.add(Messages.Gui.Admin.ACTIONS_TITLE)
            lore.add(Messages.Gui.Admin.ACTION_LEFT_CLICK)
            lore.add(Messages.Gui.Admin.ACTION_RIGHT_CLICK)

            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }

        GuiTheme.applyGlow(item, isOp || isOnline)
        return item
    }

    private fun createPrevPageItem(): ItemStack = ItemStack(NAVIGATION_MATERIAL).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.PREVIOUS_PAGE)
            setLore(listOf(Messages.Gui.Admin.previousPageLore(currentPage)))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
    }

    private fun createNextPageItem(): ItemStack = ItemStack(NAVIGATION_MATERIAL).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.NEXT_PAGE)
            setLore(listOf(Messages.Gui.Admin.nextPageLore(currentPage + 2)))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
    }

    private fun createRefreshItem(): ItemStack = ItemStack(REFRESH_MATERIAL).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.REFRESH_BUTTON)
            setLore(listOf(Messages.Gui.Admin.REFRESH_LORE))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
    }

    private fun createPendingItem(pendingCount: Int): ItemStack {
        val item = ItemStack(PENDING_MATERIAL)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.PENDING_ATTEMPTS_BUTTON)
            setLore(listOf(Messages.Gui.Admin.pendingAttemptsLore(pendingCount)))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        GuiTheme.applyGlow(item, pendingCount > 0)
        return item
    }

    private fun createEmptyStateItem(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val activeQuery = searchQuery?.takeIf { it.isNotBlank() }
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.EMPTY_STATE_TITLE)
            val lore = mutableListOf<String>()
            when {
                activeQuery != null -> lore.add(Messages.Gui.Admin.emptyStateNoResult(activeQuery))
                allPlayers.isEmpty() -> lore.add(Messages.Gui.Admin.EMPTY_STATE_DEFAULT)
                else -> lore.add(Messages.Gui.Admin.EMPTY_STATE_DEFAULT)
            }
            lore.add(Messages.Gui.Admin.EMPTY_STATE_HINT)
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun hasActiveSearch(): Boolean = !searchQuery.isNullOrBlank()

    private fun getTotalPages(): Int {
        return if (trackedPlayers.isEmpty()) 1 else (trackedPlayers.size + playersPerPage - 1) / playersPerPage
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return

        event.isCancelled = true
        val slot = event.slot
        val clickedItem = event.currentItem ?: return

        when (slot) {
            layout.getSlot("prev_page") -> if (currentPage > 0) {
                currentPage--
                updateInventory()
            }
            layout.getSlot("next_page") -> if (currentPage < getTotalPages() - 1) {
                currentPage++
                updateInventory()
            }
            layout.getSlot("refresh") -> handleRefresh()
            layout.getSlot("search") -> when (event.click) {
                ClickType.LEFT, ClickType.SHIFT_LEFT -> startSearchInput()
                ClickType.RIGHT, ClickType.SHIFT_RIGHT -> clearSearchFilter(true)
                else -> {}
            }
            layout.getSlot("pending") -> {
                if (!viewer.hasPermission("cloudly.whitelist")) {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                    return
                }
                plugin.getAdminGuiManager().openPendingWhitelistGui(viewer)
            }
            else -> if (clickedItem.type == Material.PLAYER_HEAD) {
                handlePlayerItemClick(event, clickedItem)
            }
        }
    }

    private fun handleRefresh() {
        loadTrackedPlayers()
        if (allPlayers.isEmpty()) {
            viewer.closeInventory()
            viewer.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
            return
        }
        if (trackedPlayers.isEmpty()) {
            currentPage = 0
            updateInventory()
            if (hasActiveSearch()) {
                searchQuery?.let { viewer.sendMessage(Messages.Gui.Admin.searchNoResults(it)) }
            }
            return
        }
        val totalPages = getTotalPages()
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }
        updateInventory()
        viewer.sendMessage(Messages.Gui.Admin.REFRESHED)
    }

    private fun handlePlayerItemClick(event: InventoryClickEvent, item: ItemStack) {
        val meta = item.itemMeta as? SkullMeta ?: return
        val owningPlayer = meta.owningPlayer ?: return
        val playerUuid = owningPlayer.uniqueId
        val playerName = owningPlayer.name ?: Messages.Gui.Admin.UNKNOWN

        when (event.click) {
            ClickType.LEFT -> {
                if (!viewer.hasPermission("cloudly.moderation")) {
                    viewer.sendMessage(Messages.Gui.Admin.NO_PERMISSION_ADMIN)
                    return
                }
                PlayerAdminGui(plugin, viewer, playerUuid, playerName, currentPage).open()
            }
            ClickType.RIGHT -> {
                if (viewer.hasPermission("cloudly.whitelist.remove")) {
                    if (plugin.getWhitelistService().removePlayer(playerUuid)) {
                        viewer.sendMessage(Messages.Gui.Admin.playerRemoved(playerName))
                        Bukkit.getPlayer(playerUuid)?.takeIf { it.isOnline }?.kickPlayer(Messages.Commands.Whitelist.PLAYER_REMOVED_KICK_MESSAGE)
                        loadTrackedPlayers()
                        if (allPlayers.isEmpty()) {
                            viewer.closeInventory()
                            viewer.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
                        } else {
                            if (trackedPlayers.isEmpty()) {
                                currentPage = 0
                                updateInventory()
                                if (hasActiveSearch()) {
                                    searchQuery?.let { viewer.sendMessage(Messages.Gui.Admin.searchNoResults(it)) }
                                }
                            } else {
                                val totalPages = getTotalPages()
                                if (currentPage >= totalPages) {
                                    currentPage = maxOf(0, totalPages - 1)
                                }
                                updateInventory()
                            }
                        }
                    } else {
                        viewer.sendMessage(Messages.Gui.Admin.removeFailed(playerName))
                    }
                } else {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                }
            }
            else -> {}
        }
    }

    private fun resolveAddedByName(player: WhitelistPlayer): String {
        val addedByUuid = player.addedBy ?: return Messages.Gui.Admin.UNKNOWN
        if (addedByUuid == UUID(0, 0)) {
            return Messages.Gui.Admin.CONSOLE
        }
        return Bukkit.getOfflinePlayer(addedByUuid).name ?: Messages.Gui.Admin.UNKNOWN
    }

    private fun createSearchItem(): ItemStack {
        val item = ItemStack(SEARCH_MATERIAL)
        val activeQuery = searchQuery?.takeIf { it.isNotBlank() }
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.Admin.SEARCH_BUTTON)
            val lore = mutableListOf<String>()
            activeQuery?.let { lore.add(Messages.Gui.Admin.searchActive(it)) }
            lore.add(Messages.Gui.Admin.SEARCH_HINT_LEFT)
            lore.add(Messages.Gui.Admin.SEARCH_HINT_RIGHT)
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        GuiTheme.applyGlow(item, activeQuery != null)
        return item
    }

    private fun startSearchInput() {
        if (cleanedUp) return
        if (awaitingSearchInput) {
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_PROMPT)
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_PROMPT_CANCEL)
            return
        }
        awaitingSearchInput = true
        searchClosingForInput = true
        viewer.closeInventory()
        plugin.server.scheduler.runTask(plugin, Runnable {
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_PROMPT)
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_PROMPT_CANCEL)
        })
    }

    private fun clearSearchFilter(notify: Boolean) {
        val hadFilter = !searchQuery.isNullOrBlank()
        searchQuery = null
        loadTrackedPlayers()
        currentPage = 0
        updateInventory()
        if (notify) {
            if (hadFilter) {
                viewer.sendMessage(Messages.Gui.Admin.SEARCH_CLEARED)
            } else {
                viewer.sendMessage(Messages.Gui.Admin.SEARCH_CANCELLED)
            }
        }
    }

    private fun filterPlayers(source: List<WhitelistPlayer>): List<WhitelistPlayer> {
        val query = searchQuery?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val filtered = if (query == null) {
            source
        } else {
            source.filter { matchesSearch(it, query) }
        }

        return filtered.sortedByDescending { it.addedAt }
    }

    private fun matchesSearch(player: WhitelistPlayer, query: String): Boolean {
        val addedByName = resolveAddedByName(player)
        return player.username.lowercase().contains(query) ||
            (player.reason?.lowercase()?.contains(query) == true) ||
            (player.discordConnection?.discordUsername?.lowercase()?.contains(query) == true) ||
            addedByName.lowercase().contains(query)
    }

    private fun handleSearchInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.equals("cancel", true) || trimmed.equals("abbrechen", true)) {
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_CANCELLED)
            reopenInventoryWithCurrentFilter(false)
            return
        }

        if (trimmed.equals("clear", true) || trimmed.equals("reset", true)) {
            val hadFilter = !searchQuery.isNullOrBlank()
            searchQuery = null
            viewer.sendMessage(if (hadFilter) Messages.Gui.Admin.SEARCH_CLEARED else Messages.Gui.Admin.SEARCH_CANCELLED)
            reopenInventoryWithCurrentFilter(true)
            return
        }

        val sanitized = trimmed.take(32)
        if (sanitized.isEmpty()) {
            viewer.sendMessage(Messages.Gui.Admin.SEARCH_CANCELLED)
            reopenInventoryWithCurrentFilter(false)
            return
        }

        searchQuery = sanitized
        viewer.sendMessage(Messages.Gui.Admin.searchApplied(sanitized))
        reopenInventoryWithCurrentFilter(true)
    }

    private fun reopenInventoryWithCurrentFilter(showNoResultNotice: Boolean) {
        currentPage = 0
        loadTrackedPlayers()
        if (allPlayers.isEmpty()) {
            viewer.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
            cleanup()
            return
        }
        createInventory()
        updateInventory()
        inventory?.let(viewer::openInventory)
        if (trackedPlayers.isEmpty() && showNoResultNotice && hasActiveSearch()) {
            searchQuery?.let { viewer.sendMessage(Messages.Gui.Admin.searchNoResults(it)) }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.player != viewer) return
        val currentInventory = inventory
        if (currentInventory != null && event.inventory != currentInventory) return
        if (searchClosingForInput) {
            searchClosingForInput = false
            inventory = null
            return
        }
        cleanup()
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (!awaitingSearchInput || event.player != viewer) return
        event.isCancelled = true
        awaitingSearchInput = false
        val message = event.message
        plugin.server.scheduler.runTask(plugin, Runnable {
            handleSearchInput(message)
        })
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        awaitingSearchInput = false
        searchClosingForInput = false
        inventory = null
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        AsyncPlayerChatEvent.getHandlerList().unregister(this)
        plugin.getAdminGuiManager().unregisterGui(viewer.uniqueId)
    }
}
