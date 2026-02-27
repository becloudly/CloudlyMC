package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.GuiTheme.applyFrame
import de.cloudly.gui.GuiTheme.applyGlow
import de.cloudly.gui.GuiTheme.applyRow
import de.cloudly.gui.GuiTheme.pane
import de.cloudly.gui.GuiTheme.standardFiller
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Styled overview of all whitelisted players with quick moderation access.
 */
class WhitelistGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val initialPage: Int = 0
) : Listener {

    private val whitelistService = plugin.getWhitelistService()

    private var inventory: Inventory? = null
    private var currentPage = initialPage
    private var whitelistedPlayers: List<WhitelistPlayer> = emptyList()
    private var cleanedUp = false
    private val displayedPlayers = mutableMapOf<Int, WhitelistPlayer>()

    companion object {
        private const val INVENTORY_SIZE = 54
        private val PLAYER_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )

        private const val SLOT_INFO = 4
        private const val SLOT_PREV = 45
        private const val SLOT_BACK = 48
        private const val SLOT_REFRESH = 49
        private const val SLOT_NEXT = 53

        private val CONSOLE_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        loadWhitelistedPlayers()
        if (whitelistedPlayers.isEmpty()) {
            viewer.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
            cleanup()
            plugin.getWhitelistGuiManager().unregisterGui(viewer.uniqueId)
            return
        }

        createInventory()
        updateInventory()
        viewer.openInventory(requireNotNull(inventory))
    }

    private fun loadWhitelistedPlayers() {
        whitelistedPlayers = whitelistService.getAllPlayers()
    }

    private fun createInventory() {
        val title = Messages.Gui.Whitelist.title(whitelistedPlayers.size)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        displayedPlayers.clear()

        decorateInventory(inv)
        renderPlayers(inv)
        renderFooter(inv)
    }

    private fun decorateInventory(inv: Inventory) {
        applyFrame(inv)

        val topSkip = setOf(0, 8, SLOT_INFO % 9)
        applyRow(inv, 0, pane(Material.BLACK_STAINED_GLASS_PANE, "§0"), topSkip)

        val middleSkip = buildSet {
            add(0)
            add(8)
            PLAYER_SLOTS.forEach { add(it % 9) }
        }
        applyRow(inv, 1, pane(Material.GRAY_STAINED_GLASS_PANE), middleSkip)
        applyRow(inv, 2, pane(Material.GRAY_STAINED_GLASS_PANE), middleSkip)
        applyRow(inv, 3, pane(Material.GRAY_STAINED_GLASS_PANE), middleSkip)

        inv.setItem(SLOT_INFO, createInfoItem())
    }

    private fun renderPlayers(inv: Inventory) {
        val pageSize = PLAYER_SLOTS.size
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, whitelistedPlayers.size)

        PLAYER_SLOTS.forEach { inv.setItem(it, null) }

        var slotIndex = 0
        for (index in startIndex until endIndex) {
            val slot = PLAYER_SLOTS.getOrNull(slotIndex) ?: break
            val player = whitelistedPlayers[index]
            inv.setItem(slot, createPlayerCard(player))
            displayedPlayers[slot] = player
            slotIndex++
        }
    }

    private fun renderFooter(inv: Inventory) {
        inv.setItem(SLOT_PREV, createPrevItem())
        inv.setItem(SLOT_BACK, createBackItem())
        inv.setItem(SLOT_REFRESH, createRefreshItem())
        inv.setItem(SLOT_NEXT, createNextItem())

        for (slot in 45..53) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, standardFiller())
            }
        }
    }

    private fun createInfoItem(): ItemStack {
        val totalPages = getTotalPages()

        val lore = mutableListOf<String>()
        lore.add(Messages.Gui.Whitelist.infoTotalPlayers(whitelistedPlayers.size))
        lore.add(Messages.Gui.Whitelist.infoCurrentPage(currentPage + 1, totalPages))
        lore.add(Messages.Gui.Whitelist.infoPlayersPerPage(PLAYER_SLOTS.size))
        lore.add("§7")
        lore.add(Messages.Gui.Whitelist.INFO_ADD_COMMAND)
        lore.add(Messages.Gui.Whitelist.INFO_REMOVE_COMMAND)

        return ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.INFO_TITLE)
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createPrevItem(): ItemStack? {
        if (currentPage == 0) return null
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.PREVIOUS_PAGE)
                setLore(listOf(Messages.Gui.Whitelist.previousPageLore(currentPage)))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createNextItem(): ItemStack? {
        if (currentPage >= getTotalPages() - 1) return null
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.NEXT_PAGE)
                setLore(listOf(Messages.Gui.Whitelist.nextPageLore(currentPage + 2)))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createBackItem(): ItemStack {
        return ItemStack(Material.BARRIER).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.BUTTON_BACK)
                setLore(listOf(Messages.Gui.Whitelist.BUTTON_BACK_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createRefreshItem(): ItemStack {
        return ItemStack(Material.EMERALD).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.REFRESH_BUTTON)
                setLore(listOf(Messages.Gui.Whitelist.REFRESH_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createPlayerCard(player: WhitelistPlayer): ItemStack {
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        val isOp = offlinePlayer.isOp
        val isOnline = Bukkit.getPlayer(player.uuid)?.isOnline == true

        return ItemStack(Material.PLAYER_HEAD).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                owningPlayer = offlinePlayer
                setDisplayName("§a§l${player.username}")

                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.Whitelist.infoUuid(player.uuid.toString()))
                lore.add(Messages.Gui.Whitelist.playerAddedBy(resolveAddedByName(player)))
                val addedOn = DATE_FORMAT.format(player.addedAt)
                lore.add(Messages.Gui.Whitelist.playerAddedOn(addedOn))
                player.reason?.takeIf { it.isNotBlank() }?.let {
                    lore.add(Messages.Gui.Whitelist.playerReason(it))
                }
                lore.add(Messages.Gui.Whitelist.playerOnlineStatus(isOnline))
                if (isOp) {
                    lore.add(Messages.Gui.Whitelist.PLAYER_OP_STATUS)
                }
                lore.add("§7")

                val discord = player.discordConnection
                if (discord != null) {
                    if (discord.verified) {
                        lore.add(Messages.Gui.Whitelist.playerDiscordVerified(discord.discordUsername))
                    } else {
                        lore.add(Messages.Gui.Whitelist.playerDiscordConnected(discord.discordUsername))
                    }
                } else {
                    lore.add(Messages.Gui.Whitelist.PLAYER_DISCORD_NOT_CONNECTED)
                }
                lore.add("§7")
                lore.add(Messages.Gui.Whitelist.ACTIONS_TITLE)
                lore.add(Messages.Gui.Whitelist.ACTION_LEFT_CLICK)
                lore.add(Messages.Gui.Whitelist.ACTION_RIGHT_CLICK)

                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, isOp)
        }
    }

    private fun resolveAddedByName(player: WhitelistPlayer): String {
        val addedBy = player.addedBy ?: return Messages.Gui.Whitelist.UNKNOWN
        if (addedBy == CONSOLE_UUID) return Messages.Gui.Whitelist.CONSOLE
        return Bukkit.getOfflinePlayer(addedBy).name ?: Messages.Gui.Whitelist.UNKNOWN
    }

    private fun getTotalPages(): Int {
        val pageSize = PLAYER_SLOTS.size
        return if (whitelistedPlayers.isEmpty()) 1 else (whitelistedPlayers.size + pageSize - 1) / pageSize
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        when (event.slot) {
            SLOT_PREV -> if (currentPage > 0) {
                currentPage--
                updateInventory()
            }
            SLOT_NEXT -> if (currentPage < getTotalPages() - 1) {
                currentPage++
                updateInventory()
            }
            SLOT_REFRESH -> handleRefresh()
            SLOT_BACK -> handleBack()
            SLOT_INFO -> Unit
            else -> handlePlayerSlotClick(event.slot, event.click)
        }
    }

    private fun handlePlayerSlotClick(slot: Int, click: ClickType) {
        val target = displayedPlayers[slot] ?: return
        val targetUuid = target.uuid
        val targetName = target.username

        when (click) {
            ClickType.LEFT, ClickType.SHIFT_LEFT -> {
                if (!viewer.hasPermission("cloudly.moderation")) {
                    viewer.sendMessage(Messages.Gui.Whitelist.NO_PERMISSION_ADMIN)
                    return
                }
                WhitelistPlayerAdminGui(plugin, viewer, targetUuid, targetName, currentPage).open()
            }
            ClickType.RIGHT, ClickType.SHIFT_RIGHT -> {
                if (!viewer.hasPermission("cloudly.whitelist.remove")) {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                    return
                }

                if (whitelistService.removePlayer(targetUuid)) {
                    viewer.sendMessage(Messages.Gui.Whitelist.playerRemoved(targetName))
                    Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }
                        ?.kickPlayer(Messages.Commands.Whitelist.PLAYER_REMOVED_KICK_MESSAGE)
                    reloadOrClose()
                } else {
                    viewer.sendMessage(Messages.Gui.Whitelist.removeFailed(targetName))
                }
            }
            else -> Unit
        }
    }

    private fun handleBack() {
        viewer.closeInventory()
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.getAdminGuiManager().openAdminGui(viewer)
        })
    }

    private fun handleRefresh() {
        loadWhitelistedPlayers()
        if (whitelistedPlayers.isEmpty()) {
            viewer.closeInventory()
            viewer.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
            return
        }

        val totalPages = getTotalPages()
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }
        updateInventory()
        viewer.sendMessage(Messages.Gui.Whitelist.REFRESHED)
    }

    private fun reloadOrClose() {
        loadWhitelistedPlayers()
        if (whitelistedPlayers.isEmpty()) {
            viewer.closeInventory()
            viewer.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
        } else {
            val totalPages = getTotalPages()
            if (currentPage >= totalPages) {
                currentPage = maxOf(0, totalPages - 1)
            }
            updateInventory()
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        plugin.getWhitelistGuiManager().unregisterGui(viewer.uniqueId)
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        displayedPlayers.clear()
        inventory = null
    }
}
