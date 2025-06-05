/*
 * Cloudly - Application Review GUI
 * 
 * Provides an intuitive GUI interface for admins to review whitelist applications.
 * Features player heads, application details, and easy approve/deny controls.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import cloudly.util.ApplicationManager.WhitelistApplication
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * ApplicationReviewGUI handles the admin interface for reviewing applications
 */
object ApplicationReviewGUI : Listener {
    
    private lateinit var plugin: CloudlyPlugin
    private val openGUIs = ConcurrentHashMap<UUID, ApplicationGUIData>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
    
    /**
     * Data class for GUI state
     */
    data class ApplicationGUIData(
        val adminUUID: UUID,
        val currentPage: Int,
        val applications: List<WhitelistApplication>,
        val inventory: Inventory
    )
    
    /**
     * Initialize the GUI manager
     */
    fun initialize(pluginInstance: CloudlyPlugin) {
        plugin = pluginInstance
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }
    
    /**
     * Open the application review GUI for an admin
     */
    fun openReviewGUI(admin: Player, page: Int = 0) {
        plugin.getPluginScope().launch {
            try {
                val applications = ApplicationManager.getPendingApplications()
                
                if (applications.isEmpty()) {
                    LanguageManager.sendMessage(admin, "whitelist.admin.no.applications")
                    return@launch
                }
                
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val inventory = createGUIInventory(applications, page)
                    val guiData = ApplicationGUIData(admin.uniqueId, page, applications, inventory)
                    
                    openGUIs[admin.uniqueId] = guiData
                    admin.openInventory(inventory)
                })
                
            } catch (e: Exception) {
                plugin.logger.severe("Error opening application review GUI: ${e.message}")
                LanguageManager.sendMessage(admin, "whitelist.admin.gui.error")
            }
        }
    }
    
    /**
     * Create the GUI inventory
     */
    private fun createGUIInventory(applications: List<WhitelistApplication>, page: Int): Inventory {
        val title = LanguageManager.getMessage("whitelist.admin.gui.title", applications.size)
        val inventory = Bukkit.createInventory(null, 54, title)
        
        val itemsPerPage = 21 // 3 rows of 7 items (leaving space for navigation)
        val startIndex = page * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, applications.size)
        
        // Add application items
        var slot = 10 // Start from second row
        for (i in startIndex until endIndex) {
            val application = applications[i]
            val applicationItem = createApplicationItem(application)
            inventory.setItem(slot, applicationItem)
            
            // Move to next slot (skip last 2 slots of each row)
            slot++
            if (slot % 9 >= 7) {
                slot += 3 // Skip to next row
            }
        }
        
        // Add navigation items
        addNavigationItems(inventory, page, applications.size, itemsPerPage)
        
        // Add utility items
        addUtilityItems(inventory)
        
        return inventory
    }
    
    /**
     * Create an item representing a whitelist application
     */
    private fun createApplicationItem(application: WhitelistApplication): ItemStack {
        val playerHead = ItemStack(Material.PLAYER_HEAD)
        val meta = playerHead.itemMeta as SkullMeta
        
        // Set player head
        val offlinePlayer = Bukkit.getOfflinePlayer(application.uuid)
        meta.owningPlayer = offlinePlayer
        
        // Set display name
        meta.setDisplayName("§e${application.username}")
        
        // Set lore with application details
        val lore = mutableListOf<String>()
        lore.add("§7Applied: §f${dateFormat.format(Date(application.appliedAt))}")
        lore.add("§7Status: §6${application.status}")
        lore.add("")
        lore.add("§7Reason:")
        
        // Split reason into multiple lines if too long
        val reasonLines = wrapText(application.reason, 30)
        reasonLines.forEach { line ->
            lore.add("§f  $line")
        }
        
        lore.add("")
        lore.add("§a§l[LEFT CLICK] §aApprove")
        lore.add("§c§l[RIGHT CLICK] §cDeny")
        lore.add("§e§l[SHIFT+CLICK] §eView Details")
        
        meta.lore = lore
        playerHead.itemMeta = meta
        
        return playerHead
    }
    
    /**
     * Add navigation items to the GUI
     */
    private fun addNavigationItems(inventory: Inventory, currentPage: Int, totalApplications: Int, itemsPerPage: Int) {
        val totalPages = (totalApplications + itemsPerPage - 1) / itemsPerPage
        
        // Previous page button
        if (currentPage > 0) {
            val prevItem = ItemStack(Material.ARROW)
            val prevMeta = prevItem.itemMeta!!
            prevMeta.setDisplayName("§e« Previous Page")
            prevMeta.lore = listOf("§7Click to go to page ${currentPage}")
            prevItem.itemMeta = prevMeta
            inventory.setItem(45, prevItem)
        }
        
        // Page info
        val pageInfoItem = ItemStack(Material.BOOK)
        val pageInfoMeta = pageInfoItem.itemMeta!!
        pageInfoMeta.setDisplayName("§ePage ${currentPage + 1} of $totalPages")
        pageInfoMeta.lore = listOf(
            "§7Total Applications: §f$totalApplications",
            "§7Showing: §f${currentPage * itemsPerPage + 1}-${minOf((currentPage + 1) * itemsPerPage, totalApplications)}"
        )
        pageInfoItem.itemMeta = pageInfoMeta
        inventory.setItem(49, pageInfoItem)
        
        // Next page button
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            val nextMeta = nextItem.itemMeta!!
            nextMeta.setDisplayName("§eNext Page »")
            nextMeta.lore = listOf("§7Click to go to page ${currentPage + 2}")
            nextItem.itemMeta = nextMeta
            inventory.setItem(53, nextItem)
        }
    }
    
    /**
     * Add utility items to the GUI
     */
    private fun addUtilityItems(inventory: Inventory) {
        // Refresh button
        val refreshItem = ItemStack(Material.LIME_DYE)
        val refreshMeta = refreshItem.itemMeta!!
        refreshMeta.setDisplayName("§a§lRefresh")
        refreshMeta.lore = listOf("§7Click to refresh the application list")
        refreshItem.itemMeta = refreshMeta
        inventory.setItem(48, refreshItem)
        
        // Close button
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta!!
        closeMeta.setDisplayName("§c§lClose")
        closeMeta.lore = listOf("§7Click to close this GUI")
        closeItem.itemMeta = closeMeta
        inventory.setItem(50, closeItem)
        
        // Statistics item
        val statsItem = ItemStack(Material.PAPER)
        val statsMeta = statsItem.itemMeta!!
        statsMeta.setDisplayName("§b§lStatistics")
        statsMeta.lore = listOf(
            "§7Click for application statistics",
            "§8(Feature coming soon)"
        )
        statsItem.itemMeta = statsMeta
        inventory.setItem(47, statsItem)
    }
    
    /**
     * Handle inventory click events
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val guiData = openGUIs[player.uniqueId] ?: return
        
        if (event.inventory != guiData.inventory) return
        
        event.isCancelled = true
        
        val clickedItem = event.currentItem ?: return
        val slot = event.rawSlot
        
        when {
            // Application item clicked
            clickedItem.type == Material.PLAYER_HEAD && slot in 10..43 -> {
                handleApplicationClick(player, event, guiData)
            }
            
            // Navigation buttons
            slot == 45 && clickedItem.type == Material.ARROW -> { // Previous page
                openReviewGUI(player, guiData.currentPage - 1)
            }
            
            slot == 53 && clickedItem.type == Material.ARROW -> { // Next page
                openReviewGUI(player, guiData.currentPage + 1)
            }
            
            // Utility buttons
            slot == 48 && clickedItem.type == Material.LIME_DYE -> { // Refresh
                openReviewGUI(player, guiData.currentPage)
            }
            
            slot == 50 && clickedItem.type == Material.BARRIER -> { // Close
                player.closeInventory()
            }
            
            slot == 47 && clickedItem.type == Material.PAPER -> { // Statistics
                LanguageManager.sendMessage(player, "whitelist.admin.stats.coming.soon")
            }
        }
    }
    
    /**
     * Handle application item clicks
     */
    private fun handleApplicationClick(player: Player, event: InventoryClickEvent, guiData: ApplicationGUIData) {
        val clickedItem = event.currentItem ?: return
        val playerName = clickedItem.itemMeta?.displayName?.replace("§e", "") ?: return
        
        val application = guiData.applications.find { it.username == playerName } ?: return
        
        when {
            event.isLeftClick && !event.isShiftClick -> {
                // Approve application
                approveApplicationDialog(player, application)
            }
            
            event.isRightClick && !event.isShiftClick -> {
                // Deny application
                denyApplicationDialog(player, application)
            }
            
            event.isShiftClick -> {
                // View details
                showApplicationDetails(player, application)
            }
        }
    }
    
    /**
     * Show approve dialog
     */
    private fun approveApplicationDialog(admin: Player, application: WhitelistApplication) {
        admin.closeInventory()
        LanguageManager.sendMessage(admin, "whitelist.admin.approve.prompt", application.username)
        LanguageManager.sendMessage(admin, "whitelist.admin.approve.instructions")
        
        // We'll implement a chat input system for the approval reason
        ChatInputManager.waitForInput(admin, "APPROVE_APPLICATION", application.id) { input ->
            plugin.getPluginScope().launch {
                val success = ApplicationManager.approveApplication(application.id, admin.name, input)
                
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        LanguageManager.sendMessage(admin, "whitelist.admin.approve.success", application.username)
                        openReviewGUI(admin, 0) // Refresh GUI
                    } else {
                        LanguageManager.sendMessage(admin, "whitelist.admin.approve.error")
                    }
                })
            }
        }
    }
    
    /**
     * Show deny dialog
     */
    private fun denyApplicationDialog(admin: Player, application: WhitelistApplication) {
        admin.closeInventory()
        LanguageManager.sendMessage(admin, "whitelist.admin.deny.prompt", application.username)
        LanguageManager.sendMessage(admin, "whitelist.admin.deny.instructions")
        
        ChatInputManager.waitForInput(admin, "DENY_APPLICATION", application.id) { reason ->
            plugin.getPluginScope().launch {
                val success = ApplicationManager.denyApplication(application.id, admin.name, reason)
                
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        LanguageManager.sendMessage(admin, "whitelist.admin.deny.success", application.username)
                        openReviewGUI(admin, 0) // Refresh GUI
                    } else {
                        LanguageManager.sendMessage(admin, "whitelist.admin.deny.error")
                    }
                })
            }
        }
    }
    
    /**
     * Show detailed application information
     */
    private fun showApplicationDetails(admin: Player, application: WhitelistApplication) {
        admin.closeInventory()
        
        LanguageManager.sendMessage(admin, "whitelist.admin.details.header")
        LanguageManager.sendMessage(admin, "whitelist.admin.details.player", application.username)
        LanguageManager.sendMessage(admin, "whitelist.admin.details.uuid", application.uuid.toString())
        LanguageManager.sendMessage(admin, "whitelist.admin.details.applied", dateFormat.format(Date(application.appliedAt)))
        LanguageManager.sendMessage(admin, "whitelist.admin.details.status", application.status.toString())
        LanguageManager.sendMessage(admin, "whitelist.admin.details.reason")
        
        // Show reason with proper formatting
        val reasonLines = wrapText(application.reason, 50)
        reasonLines.forEach { line ->
            admin.sendMessage("§f  $line")
        }
        
        LanguageManager.sendMessage(admin, "whitelist.admin.details.footer")
        
        // Offer quick actions
        admin.sendMessage("")
        admin.sendMessage("§7Quick Actions:")
        admin.sendMessage("§a/whitelist admin approve ${application.username} [reason] §7- Approve")
        admin.sendMessage("§c/whitelist admin deny ${application.username} <reason> §7- Deny")
        admin.sendMessage("§e/whitelist admin review §7- Return to GUI")
    }
    
    /**
     * Handle inventory close
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openGUIs.remove(player.uniqueId)
    }
    
    /**
     * Utility function to wrap text
     */
    private fun wrapText(text: String, maxLength: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if ((currentLine + " " + word).length <= maxLength) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}
