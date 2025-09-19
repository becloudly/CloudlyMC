package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
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
        private const val PREV_PAGE_SLOT = 45
        private const val INFO_SLOT = 49
        private const val NEXT_PAGE_SLOT = 53
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val INFO_MATERIAL = Material.BOOK
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
        val languageManager = plugin.getLanguageManager()
        val title = languageManager.getMessage("gui.whitelist.title", "count" to whitelistedPlayers.size.toString())
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
        val languageManager = plugin.getLanguageManager()
        val startIndex = currentPage * playersPerPage
        val endIndex = minOf(startIndex + playersPerPage, whitelistedPlayers.size)
        
        for (i in startIndex until endIndex) {
            val player = whitelistedPlayers[i]
            val slotIndex = getPlayerSlotIndex(i - startIndex)
            
            val playerItem = createPlayerItem(player, languageManager)
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
    private fun createPlayerItem(player: WhitelistPlayer, languageManager: de.cloudly.config.LanguageManager): ItemStack {
        val playerItem = ItemStack(Material.PLAYER_HEAD)
        
        playerItem.itemMeta = (playerItem.itemMeta as SkullMeta).apply {
            // Set player skull
            owningPlayer = Bukkit.getOfflinePlayer(player.uuid)
            
            // Set display name
            setDisplayName("§a§l${player.username}")
            
            // Create detailed lore
            val lore = mutableListOf<String>()
            lore.add("§7")
            lore.add("§7UUID: §f${player.uuid}")
            
            // Added by information
            val addedByName = if (player.addedBy != null) {
                val addedByUuid = player.addedBy
                if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                    "Console"
                } else {
                    Bukkit.getOfflinePlayer(addedByUuid).name ?: "Unknown"
                }
            } else {
                "Unknown"
            }
            lore.add("§7Added by: §f$addedByName")
            
            // Format date
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            val dateStr = player.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
            lore.add("§7Added on: §f$dateStr")
            
            // Add reason if available
            if (!player.reason.isNullOrBlank()) {
                lore.add("§7Reason: §f${player.reason}")
            }
            
            // Add action hints
            lore.add("§7")
            lore.add("§e§lActions:")
            lore.add("§7• §fLeft-click: View details")
            lore.add("§7• §cRight-click: Remove from whitelist")
            
            setLore(lore)
        }
        
        return playerItem
    }
    
    /**
     * Adds navigation items to the inventory.
     */
    private fun addNavigationItems(inv: Inventory) {
        val languageManager = plugin.getLanguageManager()
        val totalPages = getTotalPages()
        
        // Previous page button
        if (currentPage > 0) {
            val prevItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§a§lPrevious Page")
                    setLore(listOf("§7Click to go to page ${currentPage}"))
                }
            }
            inv.setItem(PREV_PAGE_SLOT, prevItem)
        }
        
        // Info item
        val infoItem = ItemStack(INFO_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§6§lWhitelist Information")
                setLore(listOf(
                    "§7",
                    "§7Total Players: §f${whitelistedPlayers.size}",
                    "§7Current Page: §f${currentPage + 1}§7/§f$totalPages",
                    "§7Players per Page: §f$playersPerPage",
                    "§7",
                    "§eUse §f/cloudly whitelist add <player>§e to add players",
                    "§eUse §f/cloudly whitelist remove <player>§e to remove players"
                ))
            }
        }
        inv.setItem(INFO_SLOT, infoItem)
        
        // Next page button
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§a§lNext Page")
                    setLore(listOf("§7Click to go to page ${currentPage + 2}"))
                }
            }
            inv.setItem(NEXT_PAGE_SLOT, nextItem)
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
            PREV_PAGE_SLOT -> {
                if (currentPage > 0) {
                    currentPage--
                    updateInventory()
                }
            }
            NEXT_PAGE_SLOT -> {
                if (currentPage < getTotalPages() - 1) {
                    currentPage++
                    updateInventory()
                }
            }
            INFO_SLOT -> {
                // Refresh the GUI
                loadWhitelistedPlayers()
                updateInventory()
                viewer.sendMessage(plugin.getLanguageManager().getMessage("gui.whitelist.refreshed"))
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
        val playerName = owningPlayer.name ?: "Unknown"
        
        val languageManager = plugin.getLanguageManager()
        
        when (event.click) {
            org.bukkit.event.inventory.ClickType.LEFT -> {
                // Show detailed player information
                viewer.closeInventory()
                viewer.sendMessage(languageManager.getMessage("gui.whitelist.player_details", "player" to playerName))
                
                val whitelistPlayer = plugin.getWhitelistService().getPlayer(playerUuid)
                if (whitelistPlayer != null) {
                    val addedByUuid = whitelistPlayer.addedBy ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
                    val addedByName = if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                        "Console"
                    } else {
                        Bukkit.getOfflinePlayer(addedByUuid).name ?: "Unknown"
                    }
                    
                    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                    val dateStr = whitelistPlayer.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
                    
                    viewer.sendMessage(languageManager.getMessage("commands.whitelist.info_header", "player" to whitelistPlayer.username))
                    viewer.sendMessage(languageManager.getMessage("commands.whitelist.info_added_by", "name" to addedByName))
                    viewer.sendMessage(languageManager.getMessage("commands.whitelist.info_added_on", "date" to dateStr))
                    if (!whitelistPlayer.reason.isNullOrBlank()) {
                        viewer.sendMessage(languageManager.getMessage("commands.whitelist.info_reason", "reason" to whitelistPlayer.reason))
                    }
                }
            }
            
            org.bukkit.event.inventory.ClickType.RIGHT -> {
                // Remove player from whitelist (with confirmation)
                if (viewer.hasPermission("cloudly.whitelist.remove")) {
                    if (plugin.getWhitelistService().removePlayer(playerUuid)) {
                        viewer.sendMessage(languageManager.getMessage("gui.whitelist.player_removed", "player" to playerName))
                        
                        // Check if the player is online and kick them
                        val onlinePlayer = Bukkit.getPlayer(playerUuid)
                        if (onlinePlayer != null && onlinePlayer.isOnline) {
                            onlinePlayer.kickPlayer(languageManager.getMessage("commands.whitelist.player_removed_kick_message"))
                        }
                        
                        // Refresh the GUI
                        loadWhitelistedPlayers()
                        if (whitelistedPlayers.isEmpty()) {
                            viewer.closeInventory()
                            viewer.sendMessage(languageManager.getMessage("commands.whitelist.list_empty"))
                        } else {
                            // Adjust page if necessary
                            val totalPages = getTotalPages()
                            if (currentPage >= totalPages) {
                                currentPage = maxOf(0, totalPages - 1)
                            }
                            updateInventory()
                        }
                    } else {
                        viewer.sendMessage(languageManager.getMessage("gui.whitelist.remove_failed", "player" to playerName))
                    }
                } else {
                    viewer.sendMessage(languageManager.getMessage("commands.no_permission"))
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