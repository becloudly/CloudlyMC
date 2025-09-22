package de.cloudly.gui

import de.cloudly.CloudlyPaper
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
        private const val PREV_PAGE_SLOT = 45
        private const val REFRESH_SLOT = 47
        private const val INFO_SLOT = 49
        private const val NEXT_PAGE_SLOT = 53
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val INFO_MATERIAL = Material.BOOK
        private val REFRESH_MATERIAL = Material.EMERALD
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
        
        // Check for special status (OP) before setting item meta
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        val onlinePlayer = Bukkit.getPlayer(player.uuid)
        
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
                lore.add(languageManager.getMessage("gui.whitelist.player_op_status"))
            }
            
            // Add separator if special status was shown
            if (hasSpecialStatus) {
                lore.add("§7")
            }
            
            // Added by information
            val addedByName = if (player.addedBy != null) {
                val addedByUuid = player.addedBy
                if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                    languageManager.getMessage("gui.whitelist.console")
                } else {
                    Bukkit.getOfflinePlayer(addedByUuid).name ?: languageManager.getMessage("gui.whitelist.unknown")
                }
            } else {
                languageManager.getMessage("gui.whitelist.unknown")
            }
            lore.add(languageManager.getMessage("gui.whitelist.player_added_by", "name" to addedByName))
            
            // Format date
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            val dateStr = player.addedAt.atZone(ZoneId.systemDefault()).format(formatter)
            lore.add(languageManager.getMessage("gui.whitelist.player_added_on", "date" to dateStr))
            
            // Add reason if available
            if (!player.reason.isNullOrBlank()) {
                lore.add(languageManager.getMessage("gui.whitelist.player_reason", "reason" to player.reason))
            }
            
            // Add Discord connection info
            if (player.discordConnection != null) {
                val discord = player.discordConnection
                if (discord.verified) {
                    lore.add(languageManager.getMessage("gui.whitelist.player_discord_verified", "username" to discord.discordUsername))
                } else {
                    lore.add(languageManager.getMessage("gui.whitelist.player_discord_connected", "username" to discord.discordUsername))
                }
            } else {
                lore.add(languageManager.getMessage("gui.whitelist.player_discord_not_connected"))
            }
            
            // Add action hints
            lore.add("§7")
            lore.add(languageManager.getMessage("gui.whitelist.actions_title"))
            lore.add(languageManager.getMessage("gui.whitelist.action_left_click"))
            lore.add(languageManager.getMessage("gui.whitelist.action_right_click"))
            
            setLore(lore)
            
            // Add item flags for special status (hide enchants)
            if (hasSpecialStatus) {
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        
        // Add enchantment glow for players with special status (must be done on ItemStack)
        if (hasSpecialStatus) {
            playerItem.addUnsafeEnchantment(Enchantment.DURABILITY, 1)
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
                    setDisplayName(languageManager.getMessage("gui.whitelist.previous_page"))
                    setLore(listOf(languageManager.getMessage("gui.whitelist.previous_page_lore", "page" to currentPage.toString())))
                }
            }
            inv.setItem(PREV_PAGE_SLOT, prevItem)
        }
        
        // Info item
        val infoItem = ItemStack(INFO_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(languageManager.getMessage("gui.whitelist.info_title"))
                setLore(listOf(
                    "§7",
                    languageManager.getMessage("gui.whitelist.info_total_players", "count" to whitelistedPlayers.size.toString()),
                    languageManager.getMessage("gui.whitelist.info_current_page", 
                        "current" to (currentPage + 1).toString(), 
                        "total" to totalPages.toString()),
                    languageManager.getMessage("gui.whitelist.info_players_per_page", "count" to playersPerPage.toString()),
                    "§7",
                    languageManager.getMessage("gui.whitelist.info_add_command"),
                    languageManager.getMessage("gui.whitelist.info_remove_command")
                ))
            }
        }
        inv.setItem(INFO_SLOT, infoItem)
        
        // Refresh button
        val refreshItem = ItemStack(REFRESH_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(languageManager.getMessage("gui.whitelist.refresh_button"))
                setLore(listOf(languageManager.getMessage("gui.whitelist.refresh_lore")))
            }
        }
        inv.setItem(REFRESH_SLOT, refreshItem)
        
        // Next page button
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(languageManager.getMessage("gui.whitelist.next_page"))
                    setLore(listOf(languageManager.getMessage("gui.whitelist.next_page_lore", "page" to (currentPage + 2).toString())))
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
                // Show information (no refresh here, just informational)
            }
            REFRESH_SLOT -> {
                // Refresh the GUI
                loadWhitelistedPlayers()
                if (whitelistedPlayers.isEmpty()) {
                    viewer.closeInventory()
                    viewer.sendMessage(plugin.getLanguageManager().getMessage("commands.whitelist.list_empty"))
                } else {
                    // Adjust page if necessary
                    val totalPages = getTotalPages()
                    if (currentPage >= totalPages) {
                        currentPage = maxOf(0, totalPages - 1)
                    }
                    updateInventory()
                    viewer.sendMessage(plugin.getLanguageManager().getMessage("gui.whitelist.refreshed"))
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
        val languageManager = plugin.getLanguageManager()
        val playerName = owningPlayer.name ?: languageManager.getMessage("gui.whitelist.unknown")
        
        when (event.click) {
            org.bukkit.event.inventory.ClickType.LEFT -> {
                // Show detailed player information
                viewer.closeInventory()
                viewer.sendMessage(languageManager.getMessage("gui.whitelist.player_details", "player" to playerName))
                
                val whitelistPlayer = plugin.getWhitelistService().getPlayer(playerUuid)
                if (whitelistPlayer != null) {
                    val addedByUuid = whitelistPlayer.addedBy ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
                    val addedByName = if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                        languageManager.getMessage("gui.whitelist.console")
                    } else {
                        Bukkit.getOfflinePlayer(addedByUuid).name ?: languageManager.getMessage("gui.whitelist.unknown")
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