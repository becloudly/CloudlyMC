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
 * Beautiful GUI for displaying whitelisted players.
 * Shows player skulls with detailed information and allows pagination.
 */
class WhitelistGui(private val plugin: CloudlyPaper, private val viewer: Player) : Listener {
    
    private var inventory: Inventory? = null
    private var currentPage = 0
    private val playersPerPage = 28 // 4 rows of 7 slots for players
    private var whitelistedPlayers: List<WhitelistPlayer> = emptyList()
    
    companion object {
        private const val INVENTORY_SIZE = 54 // 6 rows
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val INFO_MATERIAL = Material.BOOK
        private val REFRESH_MATERIAL = Material.EMERALD
        
        // Flexible layout system for slot positioning
        private val layout = GuiLayout(6).apply {
            setSlot("prev_page", 5, 0)   // Bottom row, left (slot 45)
            setSlot("refresh", 5, 2)     // Bottom row, left-center (slot 47)
            setSlot("info", 5, 4)        // Bottom row, center (slot 49)
            setSlot("next_page", 5, 8)   // Bottom row, right (slot 53)
        }
    }
    
    init {
        // Register this as an event listener
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    /**
     * Opens the whitelist GUI for the viewer.
     */
    fun open() {
        loadWhitelistedPlayers()
        createInventory()
        updateInventory()
        viewer.openInventory(inventory!!)
    }
    
    /**
     * Loads whitelisted players from the service.
     */
    private fun loadWhitelistedPlayers() {
        whitelistedPlayers = plugin.getWhitelistService().getAllPlayers()
    }
    
    /**
     * Creates the inventory with proper title and size.
     */
    private fun createInventory() {
        val title = Messages.Gui.Whitelist.title(whitelistedPlayers.size)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
    }
    
    /**
     * Updates the inventory with current page content.
     */
    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        
        // Add border decoration
        addBorderDecoration(inv)
        
        // Add player items
        addPlayerItems(inv)
        
        // Add navigation items
        addNavigationItems(inv)
    }
    
    /**
     * Adds decorative border to the inventory.
     */
    private fun addBorderDecoration(inv: Inventory) {
        val borderItem = ItemStack(BORDER_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§7")
            }
        }
        
        // Top border
        for (i in 0..8) {
            inv.setItem(i, borderItem)
        }
        
        // Bottom border
        for (i in 45..53) {
            inv.setItem(i, borderItem)
        }
        
        // Side borders
        for (row in 1..4) {
            inv.setItem(row * 9, borderItem)
            inv.setItem(row * 9 + 8, borderItem)
        }
    }
    
    /**
     * Adds whitelisted player items to the inventory.
     */
    private fun addPlayerItems(inv: Inventory) {
        val startIndex = currentPage * playersPerPage
        val endIndex = minOf(startIndex + playersPerPage, whitelistedPlayers.size)
        
        for (i in startIndex until endIndex) {
            val player = whitelistedPlayers[i]
            val slotIndex = getPlayerSlotIndex(i - startIndex)
            
            val playerItem = createPlayerItem(player)
            inv.setItem(slotIndex, playerItem)
        }
    }
    
    /**
     * Gets the inventory slot index for a player item.
     */
    private fun getPlayerSlotIndex(relativeIndex: Int): Int {
        val row = relativeIndex / 7
        val col = relativeIndex % 7
        return (row + 1) * 9 + col + 1 // Start from row 1, column 1
    }
    
    /**
     * Creates a player item with skull and detailed information.
     */
    private fun createPlayerItem(player: WhitelistPlayer): ItemStack {
        val playerItem = ItemStack(Material.PLAYER_HEAD)
        
        // Check for special status (OP) before setting item meta
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        
        // Check if player is OP
        val isOp = offlinePlayer.isOp
        val hasSpecialStatus = isOp
        
        playerItem.itemMeta = (playerItem.itemMeta as SkullMeta).apply {
            // Set player skull
            owningPlayer = offlinePlayer
            
            // Set display name
            setDisplayName("§a§l${player.username}")
            
            // Create detailed lore
            val lore = mutableListOf<String>()
            lore.add("§7")
            lore.add("§7UUID: §f${player.uuid}")
            
            // Add special status indicators
            if (isOp) {
                lore.add(Messages.Gui.Whitelist.PLAYER_OP_STATUS)
            }
            
            // Add separator if special status was shown
            if (hasSpecialStatus) {
                lore.add("§7")
            }
            
            // Added by information
            val addedByName = if (player.addedBy != null) {
                val addedByUuid = player.addedBy
                if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                    Messages.Gui.Whitelist.CONSOLE
                } else {
                    Bukkit.getOfflinePlayer(addedByUuid).name ?: Messages.Gui.Whitelist.UNKNOWN
                }
            } else {
                Messages.Gui.Whitelist.UNKNOWN
            }
            lore.add(Messages.Gui.Whitelist.playerAddedBy(addedByName))
            
            // Format date
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            val dateStr = player.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
            lore.add(Messages.Gui.Whitelist.playerAddedOn(dateStr))
            
            // Add reason if available
            if (!player.reason.isNullOrBlank()) {
                lore.add("§7Grund: §f${player.reason}")
            }
            
            // Add Discord connection info
            if (player.discordConnection != null) {
                val discord = player.discordConnection
                if (discord.verified) {
                    lore.add(Messages.Gui.Whitelist.playerDiscordVerified(discord.discordUsername))
                } else {
                    lore.add(Messages.Gui.Whitelist.playerDiscordConnected(discord.discordUsername))
                }
            } else {
                lore.add(Messages.Gui.Whitelist.PLAYER_DISCORD_NOT_CONNECTED)
            }
            
            // Add action hints
            lore.add("§7")
            lore.add(Messages.Gui.Whitelist.ACTIONS_TITLE)
            lore.add(Messages.Gui.Whitelist.ACTION_LEFT_CLICK)
            lore.add(Messages.Gui.Whitelist.ACTION_RIGHT_CLICK)
            
            setLore(lore)
            
            // Add item flags for special status (hide enchants)
            if (hasSpecialStatus) {
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        
        // Add enchantment glow for players with special status
        if (hasSpecialStatus) {
            playerItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
        }
        
        return playerItem
    }
    
    /**
     * Adds navigation items to the inventory.
     */
    private fun addNavigationItems(inv: Inventory) {
        val totalPages = getTotalPages()
        
        // Previous page button
        if (currentPage > 0) {
            val prevItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Whitelist.PREVIOUS_PAGE)
                    setLore(listOf(Messages.Gui.Whitelist.previousPageLore(currentPage)))
                }
            }
            inv.setItem(layout.getSlot("prev_page"), prevItem)
        }
        
        // Info item
        val infoItem = ItemStack(INFO_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.INFO_TITLE)
                setLore(listOf(
                    "§7",
                    Messages.Gui.Whitelist.infoTotalPlayers(whitelistedPlayers.size),
                    Messages.Gui.Whitelist.infoCurrentPage(currentPage + 1, totalPages),
                    Messages.Gui.Whitelist.infoPlayersPerPage(playersPerPage),
                    "§7",
                    Messages.Gui.Whitelist.INFO_ADD_COMMAND,
                    Messages.Gui.Whitelist.INFO_REMOVE_COMMAND
                ))
            }
        }
        inv.setItem(layout.getSlot("info"), infoItem)
        
        // Refresh button
        val refreshItem = ItemStack(REFRESH_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Whitelist.REFRESH_BUTTON)
                setLore(listOf(Messages.Gui.Whitelist.REFRESH_LORE))
            }
        }
        inv.setItem(layout.getSlot("refresh"), refreshItem)
        
        // Next page button
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Whitelist.NEXT_PAGE)
                    setLore(listOf(Messages.Gui.Whitelist.nextPageLore(currentPage + 2)))
                }
            }
            inv.setItem(layout.getSlot("next_page"), nextItem)
        }
    }
    
    /**
     * Gets the total number of pages.
     */
    private fun getTotalPages(): Int {
        return if (whitelistedPlayers.isEmpty()) 1 else (whitelistedPlayers.size + playersPerPage - 1) / playersPerPage
    }
    
    /**
     * Handles inventory click events.
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        
        event.isCancelled = true
        val slot = event.slot
        val clickedItem = event.currentItem ?: return
        
        when (slot) {
            layout.getSlot("prev_page") -> {
                if (currentPage > 0) {
                    currentPage--
                    updateInventory()
                }
            }
            layout.getSlot("next_page") -> {
                if (currentPage < getTotalPages() - 1) {
                    currentPage++
                    updateInventory()
                }
            }
            layout.getSlot("info") -> {
                // Show information (no action, just informational)
            }
            layout.getSlot("refresh") -> {
                // Refresh the GUI
                loadWhitelistedPlayers()
                if (whitelistedPlayers.isEmpty()) {
                    viewer.closeInventory()
                    viewer.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
                } else {
                    // Adjust page if necessary
                    val totalPages = getTotalPages()
                    if (currentPage >= totalPages) {
                        currentPage = maxOf(0, totalPages - 1)
                    }
                    updateInventory()
                    viewer.sendMessage(Messages.Gui.Whitelist.REFRESHED)
                }
            }
            else -> {
                // Check if it's a player item
                if (clickedItem.type == Material.PLAYER_HEAD) {
                    handlePlayerItemClick(event, clickedItem)
                }
            }
        }
    }
    
    /**
     * Handles clicks on player items.
     */
    private fun handlePlayerItemClick(event: InventoryClickEvent, item: ItemStack) {
        val meta = item.itemMeta as? SkullMeta ?: return
        val owningPlayer = meta.owningPlayer ?: return
        val playerUuid = owningPlayer.uniqueId
        val playerName = owningPlayer.name ?: Messages.Gui.Whitelist.UNKNOWN
        
        when (event.click) {
            org.bukkit.event.inventory.ClickType.LEFT -> {
                // Show detailed player information
                viewer.closeInventory()
                viewer.sendMessage(Messages.Gui.Whitelist.playerDetails(playerName))
                
                val whitelistPlayer = plugin.getWhitelistService().getPlayer(playerUuid)
                if (whitelistPlayer != null) {
                    val addedByUuid = whitelistPlayer.addedBy ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
                    val addedByName = if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                        Messages.Gui.Whitelist.CONSOLE
                    } else {
                        Bukkit.getOfflinePlayer(addedByUuid).name ?: Messages.Gui.Whitelist.UNKNOWN
                    }
                    
                    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                    val dateStr = whitelistPlayer.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
                    
                    viewer.sendMessage(Messages.Commands.Whitelist.infoHeader(whitelistPlayer.username))
                    viewer.sendMessage(Messages.Commands.Whitelist.infoAddedBy(addedByName))
                    viewer.sendMessage(Messages.Commands.Whitelist.infoAddedOn(dateStr))
                    if (!whitelistPlayer.reason.isNullOrBlank()) {
                        viewer.sendMessage("§e▪ §fGrund§8: §7${whitelistPlayer.reason}")
                    }
                }
            }
            
            org.bukkit.event.inventory.ClickType.RIGHT -> {
                // Remove player from whitelist (with permission check)
                if (viewer.hasPermission("cloudly.whitelist.remove")) {
                    if (plugin.getWhitelistService().removePlayer(playerUuid)) {
                        viewer.sendMessage(Messages.Gui.Whitelist.playerRemoved(playerName))
                        
                        // Check if the player is online and kick them
                        val onlinePlayer = Bukkit.getPlayer(playerUuid)
                        if (onlinePlayer != null && onlinePlayer.isOnline) {
                            onlinePlayer.kickPlayer(Messages.Commands.Whitelist.PLAYER_REMOVED_KICK_MESSAGE)
                        }
                        
                        // Refresh the GUI
                        loadWhitelistedPlayers()
                        if (whitelistedPlayers.isEmpty()) {
                            viewer.closeInventory()
                            viewer.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
                        } else {
                            // Adjust page if necessary
                            val totalPages = getTotalPages()
                            if (currentPage >= totalPages) {
                                currentPage = maxOf(0, totalPages - 1)
                            }
                            updateInventory()
                        }
                    } else {
                        viewer.sendMessage(Messages.Gui.Whitelist.removeFailed(playerName))
                    }
                } else {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                }
            }
            
            else -> {
                // Do nothing for other click types
            }
        }
    }
    
    /**
     * Handles inventory close events.
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory == inventory && event.player == viewer) {
            cleanup()
        }
    }
    
    /**
     * Cleans up resources when the GUI is closed.
     */
    private fun cleanup() {
        // Unregister this listener
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
    }
}
