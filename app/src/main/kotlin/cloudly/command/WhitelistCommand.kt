/*
 * Cloudly - Whitelist Command
 * 
 * Handles all whitelist-related commands including add, remove, enable, disable, list, and reload.
 * Fully integrated with the language system and provides comprehensive error handling.
 * All operations are async and null-safe.
 */
package cloudly.command

import cloudly.CloudlyPlugin
import cloudly.util.LanguageManager
import cloudly.util.WhitelistManager
import cloudly.util.ApplicationManager
import cloudly.util.ApplicationReviewGUI
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Date
import java.util.logging.Level

/**
 * WhitelistCommand handles all whitelist operations
 * Extends BaseCommand for consistent permission checking and error handling
 */
class WhitelistCommand : BaseCommand() {
    
    override val permission: String = "cloudly.whitelist"
    override val playerOnly: Boolean = false
    
    override fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "enable" -> handleEnable(sender)
            "disable" -> handleDisable(sender)
            "list" -> handleList(sender, args)
            "reload" -> handleReload(sender)
            "admin" -> handleAdmin(sender, args)
            "apply" -> handleApply(sender, args)
            else -> showUsage(sender)
        }
        
        return true
    }
    
    override fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        if (!hasPermissionSafely(sender)) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> {
                val subcommands = mutableListOf<String>()
                
                if (hasPermission(sender, "cloudly.whitelist.add")) {
                    subcommands.add("add")
                }
                if (hasPermission(sender, "cloudly.whitelist.remove")) {
                    subcommands.add("remove")
                }
                if (hasPermission(sender, "cloudly.whitelist.toggle")) {
                    subcommands.addAll(listOf("enable", "disable"))
                }
                if (hasPermission(sender, "cloudly.whitelist.list")) {
                    subcommands.add("list")
                }
                if (hasPermission(sender, "cloudly.whitelist.reload")) {
                    subcommands.add("reload")
                }
                if (hasPermission(sender, "cloudly.whitelist.admin")) {
                    subcommands.add("admin")
                }
                // Anyone can apply for whitelist
                subcommands.add("apply")
                
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                when (args[0].lowercase()) {
                    "add", "remove" -> {
                        // Suggest online player names
                        Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "admin" -> {
                        if (hasPermission(sender, "cloudly.whitelist.admin")) {
                            listOf("info", "purge", "logs", "clear", "export", "import", "review", "approve", "deny")
                                .filter { it.startsWith(args[1], ignoreCase = true) }
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when {
                    args[0].lowercase() == "admin" && args[1].lowercase() == "logs" -> {
                        if (hasPermission(sender, "cloudly.whitelist.admin")) {
                            listOf("1", "5", "10", "25", "50", "all")
                                .filter { it.startsWith(args[2], ignoreCase = true) }
                        } else {
                            emptyList()
                        }
                    }
                    args[0].lowercase() == "admin" && args[1].lowercase() == "clear" -> {
                        if (hasPermission(sender, "cloudly.whitelist.admin")) {
                            listOf("confirm")
                                .filter { it.startsWith(args[2], ignoreCase = true) }
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
    
    /**
     * Handle whitelist add command
     */
    private fun handleAdd(sender: CommandSender, args: Array<String>) {
        if (!hasPermission(sender, "cloudly.whitelist.add")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        if (args.size < 2) {
            sendMessage(sender, "commands.whitelist.usage-add")
            return
        }
        
        val playerName = args[1]
        val addedBy = if (sender is Player) sender.name else "CONSOLE"
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                
                // Check if player exists or has played before
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline) {
                    // Try to get player by name (this might work for never-joined players)
                    if (offlinePlayer.uniqueId.toString() == "00000000-0000-0000-0000-000000000000") {
                        sendMessage(sender, "commands.whitelist.player-not-found", playerName)
                        return@launch
                    }
                }
                
                val success = WhitelistManager.addPlayerToWhitelist(offlinePlayer, addedBy)
                
                if (success) {
                    sendMessage(sender, "commands.whitelist.player-added", offlinePlayer.name ?: playerName)
                } else {
                    sendMessage(sender, "commands.whitelist.player-already-whitelisted", offlinePlayer.name ?: playerName)
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error adding player to whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist remove command
     */
    private fun handleRemove(sender: CommandSender, args: Array<String>) {
        if (!hasPermission(sender, "cloudly.whitelist.remove")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        if (args.size < 2) {
            sendMessage(sender, "commands.whitelist.usage-remove")
            return
        }
        
        val playerName = args[1]
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                
                // Check if player is whitelisted
                if (!WhitelistManager.isPlayerWhitelisted(offlinePlayer.uniqueId)) {
                    sendMessage(sender, "commands.whitelist.player-not-whitelisted", offlinePlayer.name ?: playerName)
                    return@launch
                }
                
                val success = WhitelistManager.removePlayerFromWhitelist(offlinePlayer.uniqueId)
                
                if (success) {
                    sendMessage(sender, "commands.whitelist.player-removed", offlinePlayer.name ?: playerName)
                    
                    // Kick player if they're online and whitelist is enabled
                    if (WhitelistManager.isWhitelistEnabled() && offlinePlayer.isOnline) {
                        val player = offlinePlayer.player
                        player?.let {
                            val kickMessage = LanguageManager.getMessage("commands.whitelist.kick-message")
                            Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                                it.kickPlayer(kickMessage)
                            })
                        }
                    }
                } else {
                    sendMessage(sender, "commands.whitelist.database-error")
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error removing player from whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist enable command
     */
    private fun handleEnable(sender: CommandSender) {
        if (!hasPermission(sender, "cloudly.whitelist.toggle")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                if (WhitelistManager.isWhitelistEnabled()) {
                    sendMessage(sender, "commands.whitelist.already-enabled")
                    return@launch
                }
                
                val modifiedBy = if (sender is Player) sender.name else "CONSOLE"
                val success = WhitelistManager.setWhitelistEnabled(true, modifiedBy)
                
                if (success) {
                    sendMessage(sender, "commands.whitelist.enabled")
                    
                    // Kick non-whitelisted players
                    kickNonWhitelistedPlayers()
                } else {
                    sendMessage(sender, "commands.whitelist.database-error")
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error enabling whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist disable command
     */
    private fun handleDisable(sender: CommandSender) {
        if (!hasPermission(sender, "cloudly.whitelist.toggle")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                if (!WhitelistManager.isWhitelistEnabled()) {
                    sendMessage(sender, "commands.whitelist.already-disabled")
                    return@launch
                }
                
                val modifiedBy = if (sender is Player) sender.name else "CONSOLE"
                val success = WhitelistManager.setWhitelistEnabled(false, modifiedBy)
                
                if (success) {
                    sendMessage(sender, "commands.whitelist.disabled")
                } else {
                    sendMessage(sender, "commands.whitelist.database-error")
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error disabling whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist list command
     */
    private fun handleList(sender: CommandSender, args: Array<String>) {
        if (!hasPermission(sender, "cloudly.whitelist.list")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        var page = 1
        if (args.size > 1) {
            try {
                page = args[1].toInt()
            } catch (e: NumberFormatException) {
                sendMessage(sender, "commands.whitelist.usage-list")
                return
            }
        }
          // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val result = WhitelistManager.getWhitelistedPlayers(page, 10)
                val entries = result.first
                val totalPages = result.second
                
                if (entries.isEmpty()) {
                    if (page == 1) {
                        sendMessage(sender, "commands.whitelist.list-empty")
                    } else {
                        sendMessage(sender, "commands.whitelist.invalid-page", totalPages.toString())
                    }
                    return@launch
                }
                
                if (page > totalPages) {
                    sendMessage(sender, "commands.whitelist.invalid-page", totalPages.toString())
                    return@launch
                }
                
                // Send header
                sendMessage(sender, "commands.whitelist.list-header", page.toString(), totalPages.toString())
                
                // Send entries
                entries.forEach { entry ->
                    sendMessage(sender, "commands.whitelist.list-entry", entry.username, entry.addedBy)
                }
                
                // Send footer if there are more pages
                if (page < totalPages) {
                    sendMessage(sender, "commands.whitelist.list-footer", (page + 1).toString())
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error listing whitelisted players", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist reload command
     */
    private fun handleReload(sender: CommandSender) {
        if (!hasPermission(sender, "cloudly.whitelist.reload")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val success = WhitelistManager.reload()
                
                if (success) {
                    sendMessage(sender, "commands.whitelist.reloaded")
                } else {
                    sendMessage(sender, "commands.whitelist.database-error")
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error reloading whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Show command usage
     */
    private fun showUsage(sender: CommandSender) {
        // Show admin command only if they have the permission
        if (hasPermission(sender, "cloudly.whitelist.admin")) {
            sendMessage(sender, "common.usage-format", "/whitelist <add|remove|enable|disable|list|reload|admin>")
        } else {
            sendMessage(sender, "common.usage-format", "/whitelist <add|remove|enable|disable|list|reload>")
        }
    }
    
    /**
     * Check if sender has specific permission
     */
    private fun hasPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) || sender.hasPermission("cloudly.whitelist.*") || sender.hasPermission("cloudly.*")    }
    
    /**
     * Kick all non-whitelisted players when whitelist is enabled
     */
    private suspend fun kickNonWhitelistedPlayers() {
        try {
            val kickMessage = LanguageManager.getMessage("commands.whitelist.kick-message")
            
            Bukkit.getOnlinePlayers().forEach { player ->
                if (!WhitelistManager.isPlayerWhitelisted(player.uniqueId)) {
                    Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                        player.kickPlayer(kickMessage)
                    })
                }
            }
        } catch (e: Exception) {
            CloudlyPlugin.instance.logger.log(Level.WARNING, "Error kicking non-whitelisted players", e)
        }
    }
    
    /**
     * Handle whitelist admin commands
     * All admin subcommands are with the prefix `/whitelist admin <command>`
     * Only players with the `whitelist.admin` permission can see and use these commands
     */
    private fun handleAdmin(sender: CommandSender, args: Array<String>) {
        if (!hasPermission(sender, "cloudly.whitelist.admin")) {
            sendMessage(sender, "commands.whitelist.no-permission")
            return
        }
        
        if (args.size < 2) {
            showAdminUsage(sender)
            return
        }
        
        // Handle admin subcommands
        when (args[1].lowercase()) {
            "info" -> handleAdminInfo(sender)
            "purge" -> handleAdminPurge(sender)
            "logs" -> handleAdminLogs(sender, args)
            "clear" -> handleAdminClear(sender, args)
            "export" -> handleAdminExport(sender)
            "import" -> handleAdminImport(sender, args)
            "review" -> handleAdminReview(sender)
            "approve" -> handleAdminApprove(sender, args)
            "deny" -> handleAdminDeny(sender, args)
            else -> showAdminUsage(sender)
        }
    }
    
    /**
     * Show whitelist admin command usage
     */
    private fun showAdminUsage(sender: CommandSender) {
        sendMessage(sender, "common.usage-format", "/whitelist admin <info|purge|logs|clear|export|import|review|approve|deny>")
    }
    
    /**
     * Handle whitelist admin info command
     * Shows detailed information about the whitelist system
     */
    private fun handleAdminInfo(sender: CommandSender) {
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {                val isEnabled = WhitelistManager.isWhitelistEnabled()
                val result = WhitelistManager.getWhitelistedPlayers(1, 999)
                val entries = result.first
                val playerCount = entries.size
                
                // Send header
                sendMessage(sender, "commands.whitelist.admin.info-header")
                
                // Send whitelist status
                val statusKey = if (isEnabled) "commands.whitelist.admin.status-enabled" else "commands.whitelist.admin.status-disabled"
                sendMessage(sender, statusKey)
                
                // Send player count
                sendMessage(sender, "commands.whitelist.admin.player-count", playerCount.toString())
                
                // Send database info
                val dbType = CloudlyPlugin.instance.config.getString("database.type", "sqlite") ?: "sqlite"
                sendMessage(sender, "commands.whitelist.admin.database-type", dbType)
                
                // Send cache info
                val cacheDuration = CloudlyPlugin.instance.config.getLong("whitelist.cache.duration", 30)
                sendMessage(sender, "commands.whitelist.admin.cache-duration", cacheDuration.toString())
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error getting whitelist admin info", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist admin purge command
     * Removes inactive or duplicate entries from the whitelist
     */
    private fun handleAdminPurge(sender: CommandSender) {
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val purgeCount = WhitelistManager.purgeInactiveEntries()
                sendMessage(sender, "commands.whitelist.admin.purge-success", purgeCount.toString())
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error purging whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist admin logs command
     * Shows recent whitelist activity logs
     */
    private fun handleAdminLogs(sender: CommandSender, args: Array<String>) {
        var count = 10 // Default to 10 entries
        
        if (args.size > 2) {
            try {
                if (args[2].lowercase() == "all") {
                    count = Int.MAX_VALUE
                } else {
                    count = args[2].toInt()
                }
            } catch (e: NumberFormatException) {
                sendMessage(sender, "commands.whitelist.admin.invalid-count")
                return
            }
        }
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val logs = WhitelistManager.getWhitelistLogs(count)
                
                if (logs.isEmpty()) {
                    sendMessage(sender, "commands.whitelist.admin.no-logs")
                    return@launch
                }
                
                // Send header
                sendMessage(sender, "commands.whitelist.admin.logs-header", 
                    logs.size.toString(),
                    if (count == Int.MAX_VALUE) "all" else count.toString())
                
                // Send log entries
                logs.forEach { log ->
                    val actionType = log.actionType
                    val timestamp = log.timestamp
                    val username = log.username
                    val performedBy = log.performedBy
                    
                    // Format timestamp
                    val date = Date(timestamp)
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val formattedDate = dateFormat.format(date)
                    
                    sendMessage(sender, "commands.whitelist.admin.log-entry", 
                        formattedDate, actionType, username, performedBy)
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error retrieving whitelist logs", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist admin clear command
     * Clears all whitelist entries
     */
    private fun handleAdminClear(sender: CommandSender, args: Array<String>) {
        if (args.size < 3 || args[2].lowercase() != "confirm") {
            sendMessage(sender, "commands.whitelist.admin.clear-confirm")
            return
        }
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val clearedCount = WhitelistManager.clearWhitelist(if (sender is Player) sender.name else "CONSOLE")
                sendMessage(sender, "commands.whitelist.admin.clear-success", clearedCount.toString())
                
                // Kick non-whitelisted players if whitelist is enabled
                if (WhitelistManager.isWhitelistEnabled()) {
                    kickNonWhitelistedPlayers()
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error clearing whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist admin export command
     * Exports the whitelist to a JSON file
     */
    private fun handleAdminExport(sender: CommandSender) {
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val fileName = "whitelist_export_${System.currentTimeMillis()}.json"
                val exportPath = CloudlyPlugin.instance.dataFolder.resolve(fileName)
                
                val exportCount = WhitelistManager.exportWhitelist(exportPath)
                
                if (exportCount > 0) {
                    sendMessage(sender, "commands.whitelist.admin.export-success", 
                        exportCount.toString(), fileName)
                } else {
                    sendMessage(sender, "commands.whitelist.admin.export-empty")
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error exporting whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist admin import command
     * Imports whitelist entries from a JSON file
     */
    private fun handleAdminImport(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            sendMessage(sender, "commands.whitelist.admin.import-usage")
            return
        }
        
        val fileName = args[2]
        
        // Launch async operation
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val importPath = CloudlyPlugin.instance.dataFolder.resolve(fileName)
                
                if (!importPath.exists() || !importPath.isFile) {
                    sendMessage(sender, "commands.whitelist.admin.file-not-found", fileName)
                    return@launch
                }
                
                val addedBy = if (sender is Player) sender.name else "CONSOLE"
                val importCount = WhitelistManager.importWhitelist(importPath, addedBy)
                
                sendMessage(sender, "commands.whitelist.admin.import-success", importCount.toString(), fileName)
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error importing whitelist", e)
                sendMessage(sender, "commands.whitelist.database-error")
            }
        }
    }
    
    /**
     * Handle whitelist apply command
     */
    private fun handleApply(sender: CommandSender, args: Array<String>) {
        // Only players can apply
        if (sender !is Player) {
            sendMessage(sender, "commands.whitelist.player-only")
            return
        }
        
        if (args.size < 2) {
            sendMessage(sender, "commands.whitelist.usage-apply")
            return
        }
        
        val reason = args.drop(1).joinToString(" ")
        
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val result = ApplicationManager.submitApplication(sender, reason)
                
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    when (result) {
                        ApplicationManager.ApplicationResult.SUCCESS -> {
                            sendMessage(sender, "commands.whitelist.apply.success")
                            sendMessage(sender, "commands.whitelist.apply.pending")
                        }
                        ApplicationManager.ApplicationResult.ALREADY_EXISTS -> {
                            sendMessage(sender, "commands.whitelist.apply.already-exists")
                        }
                        ApplicationManager.ApplicationResult.ALREADY_WHITELISTED -> {
                            sendMessage(sender, "commands.whitelist.apply.already-whitelisted")
                        }
                        ApplicationManager.ApplicationResult.ERROR -> {
                            sendMessage(sender, "commands.whitelist.apply.error")
                        }
                    }
                })
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error processing application", e)
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    sendMessage(sender, "commands.whitelist.database-error")
                })
            }
        }
    }
    
    /**
     * Handle whitelist admin review command
     */
    private fun handleAdminReview(sender: CommandSender) {
        if (sender !is Player) {
            sendMessage(sender, "commands.whitelist.admin.review.player-only")
            return
        }
        
        // Open the review GUI
        ApplicationReviewGUI.openReviewGUI(sender)
    }
    
    /**
     * Handle whitelist admin approve command
     */
    private fun handleAdminApprove(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            sendMessage(sender, "commands.whitelist.admin.usage-approve")
            return
        }
        
        val playerName = args[2]
        val reason = if (args.size > 3) args.drop(3).joinToString(" ") else null
        
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                // Find the application
                val applications = ApplicationManager.getPendingApplications()
                val application = applications.find { it.username.equals(playerName, ignoreCase = true) }
                
                if (application == null) {
                    Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                        sendMessage(sender, "commands.whitelist.admin.application-not-found", playerName)
                    })
                    return@launch
                }
                
                val adminName = if (sender is Player) sender.name else "CONSOLE"
                val success = ApplicationManager.approveApplication(application.id, adminName, reason)
                
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    if (success) {
                        sendMessage(sender, "commands.whitelist.admin.approve.success", playerName)
                    } else {
                        sendMessage(sender, "commands.whitelist.admin.approve.error")
                    }
                })
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error approving application", e)
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    sendMessage(sender, "commands.whitelist.database-error")
                })
            }
        }
    }
    
    /**
     * Handle whitelist admin deny command
     */
    private fun handleAdminDeny(sender: CommandSender, args: Array<String>) {
        if (args.size < 4) {
            sendMessage(sender, "commands.whitelist.admin.usage-deny")
            return
        }
        
        val playerName = args[2]
        val reason = args.drop(3).joinToString(" ")
        
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                // Find the application
                val applications = ApplicationManager.getPendingApplications()
                val application = applications.find { it.username.equals(playerName, ignoreCase = true) }
                
                if (application == null) {
                    Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                        sendMessage(sender, "commands.whitelist.admin.application-not-found", playerName)
                    })
                    return@launch
                }
                
                val adminName = if (sender is Player) sender.name else "CONSOLE"
                val success = ApplicationManager.denyApplication(application.id, adminName, reason)
                
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    if (success) {
                        sendMessage(sender, "commands.whitelist.admin.deny.success", playerName)
                    } else {
                        sendMessage(sender, "commands.whitelist.admin.deny.error")
                    }
                })
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error denying application", e)
                Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                    sendMessage(sender, "commands.whitelist.database-error")
                })
            }
        }
    }
}