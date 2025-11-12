package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
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
 * General administration dashboard showing tracked players with quick actions.
 */
class AdminGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val initialPage: Int = 0
) : Listener {

    private var inventory: Inventory? = null
    private var currentPage = initialPage
    private val playersPerPage = 28
    private var trackedPlayers: List<WhitelistPlayer> = emptyList()
    private var cleanedUp = false

    companion object {
        private const val INVENTORY_SIZE = 54
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val INFO_MATERIAL = Material.BOOK
        private val REFRESH_MATERIAL = Material.EMERALD

        private val layout = GuiLayout(6).apply {
            setSlot("prev_page", 5, 0)
            setSlot("refresh", 5, 2)
            setSlot("info", 5, 4)
            setSlot("next_page", 5, 8)
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
        trackedPlayers = plugin.getWhitelistService().getAllPlayers()
    }

    private fun createInventory() {
        val title = Messages.Gui.Admin.title(trackedPlayers.size)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        addBorderDecoration(inv)
        addPlayerItems(inv)
        addNavigationItems(inv)
    }

    private fun addBorderDecoration(inv: Inventory) {
        val borderItem = ItemStack(BORDER_MATERIAL).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§7") }
        }

        for (i in 0..8) {
            inv.setItem(i, borderItem)
        }
        for (i in 45..53) {
            inv.setItem(i, borderItem)
        }
        for (row in 1..4) {
            inv.setItem(row * 9, borderItem)
            inv.setItem(row * 9 + 8, borderItem)
        }
    }

    private fun addPlayerItems(inv: Inventory) {
        val startIndex = currentPage * playersPerPage
        val endIndex = minOf(startIndex + playersPerPage, trackedPlayers.size)

        for (i in startIndex until endIndex) {
            val player = trackedPlayers[i]
            val slotIndex = getPlayerSlotIndex(i - startIndex)
            inv.setItem(slotIndex, createPlayerItem(player))
        }
    }

    private fun getPlayerSlotIndex(relativeIndex: Int): Int {
        val row = relativeIndex / 7
        val col = relativeIndex % 7
        return (row + 1) * 9 + col + 1
    }

    private fun createPlayerItem(player: WhitelistPlayer): ItemStack {
        val playerItem = ItemStack(Material.PLAYER_HEAD)
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        val isOp = offlinePlayer.isOp
        val hasHighlight = isOp

        playerItem.itemMeta = (playerItem.itemMeta as SkullMeta).apply {
            owningPlayer = offlinePlayer
            setDisplayName(Messages.Gui.Admin.playerLabel(player.username))

            val lore = mutableListOf<String>()
            lore.add("§7")
            lore.add(Messages.Gui.Admin.infoUuid(player.uuid.toString()))

            if (isOp) {
                lore.add(Messages.Gui.Admin.PLAYER_OP_STATUS)
                lore.add("§7")
            }

            val addedByUuid = player.addedBy
            val addedByName = when {
                addedByUuid == null -> Messages.Gui.Admin.UNKNOWN
                addedByUuid == UUID(0, 0) -> Messages.Gui.Admin.CONSOLE
                else -> Bukkit.getOfflinePlayer(addedByUuid).name ?: Messages.Gui.Admin.UNKNOWN
            }
            lore.add(Messages.Gui.Admin.playerAddedBy(addedByName))

            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            val dateStr = player.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
            lore.add(Messages.Gui.Admin.playerAddedOn(dateStr))

            if (!player.reason.isNullOrBlank()) {
                lore.add(Messages.Gui.Admin.playerReason(player.reason!!))
            }

            val discord = player.discordConnection
            when {
                discord == null -> lore.add(Messages.Gui.Admin.PLAYER_DISCORD_NOT_CONNECTED)
                discord.verified -> lore.add(Messages.Gui.Admin.playerDiscordVerified(discord.discordUsername))
                else -> lore.add(Messages.Gui.Admin.playerDiscordConnected(discord.discordUsername))
            }

            lore.add("§7")
            lore.add(Messages.Gui.Admin.ACTIONS_TITLE)
            lore.add(Messages.Gui.Admin.ACTION_LEFT_CLICK)
            lore.add(Messages.Gui.Admin.ACTION_RIGHT_CLICK)

            setLore(lore)
            if (hasHighlight) {
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }

        if (hasHighlight) {
            playerItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
        }
        return playerItem
    }

    private fun addNavigationItems(inv: Inventory) {
        val totalPages = getTotalPages()

        if (currentPage > 0) {
            val prevItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Admin.PREVIOUS_PAGE)
                    setLore(listOf(Messages.Gui.Admin.previousPageLore(currentPage)))
                }
            }
            inv.setItem(layout.getSlot("prev_page"), prevItem)
        }

        val infoItem = ItemStack(INFO_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.INFO_TITLE)
                setLore(listOf(
                    "§7",
                    Messages.Gui.Admin.infoTotalPlayers(trackedPlayers.size),
                    Messages.Gui.Admin.infoCurrentPage(currentPage + 1, totalPages),
                    Messages.Gui.Admin.infoPlayersPerPage(playersPerPage),
                    "§7",
                    Messages.Gui.Admin.INFO_ADD_COMMAND,
                    Messages.Gui.Admin.INFO_REMOVE_COMMAND
                ))
            }
        }
        inv.setItem(layout.getSlot("info"), infoItem)

        val refreshItem = ItemStack(REFRESH_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.REFRESH_BUTTON)
                setLore(listOf(Messages.Gui.Admin.REFRESH_LORE))
            }
        }
        inv.setItem(layout.getSlot("refresh"), refreshItem)

        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Admin.NEXT_PAGE)
                    setLore(listOf(Messages.Gui.Admin.nextPageLore(currentPage + 2)))
                }
            }
            inv.setItem(layout.getSlot("next_page"), nextItem)
        }
    }

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
            layout.getSlot("refresh") -> {
                loadTrackedPlayers()
                if (trackedPlayers.isEmpty()) {
                    viewer.closeInventory()
                    viewer.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
                } else {
                    val totalPages = getTotalPages()
                    if (currentPage >= totalPages) {
                        currentPage = maxOf(0, totalPages - 1)
                    }
                    updateInventory()
                    viewer.sendMessage(Messages.Gui.Admin.REFRESHED)
                }
            }
            layout.getSlot("info") -> {
                // purely informational
            }
            else -> if (clickedItem.type == Material.PLAYER_HEAD) {
                handlePlayerItemClick(event, clickedItem)
            }
        }
    }

    private fun handlePlayerItemClick(event: InventoryClickEvent, item: ItemStack) {
        val meta = item.itemMeta as? SkullMeta ?: return
        val owningPlayer = meta.owningPlayer ?: return
        val playerUuid = owningPlayer.uniqueId
        val playerName = owningPlayer.name ?: Messages.Gui.Admin.UNKNOWN

        when (event.click) {
            org.bukkit.event.inventory.ClickType.LEFT -> {
                if (!viewer.hasPermission("cloudly.moderation")) {
                    viewer.sendMessage(Messages.Gui.Admin.NO_PERMISSION_ADMIN)
                    return
                }
                PlayerAdminGui(plugin, viewer, playerUuid, playerName, currentPage).open()
            }
            org.bukkit.event.inventory.ClickType.RIGHT -> {
                if (viewer.hasPermission("cloudly.whitelist.remove")) {
                    if (plugin.getWhitelistService().removePlayer(playerUuid)) {
                        viewer.sendMessage(Messages.Gui.Admin.playerRemoved(playerName))
                        Bukkit.getPlayer(playerUuid)?.takeIf { it.isOnline }?.kickPlayer(Messages.Commands.Whitelist.PLAYER_REMOVED_KICK_MESSAGE)
                        loadTrackedPlayers()
                        if (trackedPlayers.isEmpty()) {
                            viewer.closeInventory()
                            viewer.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
                        } else {
                            val totalPages = getTotalPages()
                            if (currentPage >= totalPages) {
                                currentPage = maxOf(0, totalPages - 1)
                            }
                            updateInventory()
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

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory == inventory && event.player == viewer) {
            cleanup()
        }
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        plugin.getAdminGuiManager().unregisterGui(viewer.uniqueId)
    }
}
