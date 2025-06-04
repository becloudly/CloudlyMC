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
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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
                val (entries, totalPages) = WhitelistManager.getWhitelistedPlayers(page, 10)
                
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
        sendMessage(sender, "common.usage-format", "/whitelist <add|remove|enable|disable|list|reload>")
    }
    
    /**
     * Check if sender has specific permission
     */
    private fun hasPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) || sender.hasPermission("cloudly.whitelist.*") || sender.hasPermission("cloudly.*")
    }
    
    /**
     * Send a message to the sender using the language manager
     */
    private fun sendMessage(sender: CommandSender, key: String, vararg args: Any) {
        try {
            val message = LanguageManager.getMessage(key, *args)
            sender.sendMessage(message)
        } catch (e: Exception) {
            CloudlyPlugin.instance.logger.log(Level.WARNING, "Error sending message with key: $key", e)
            sender.sendMessage("§cError occurred while processing command")
        }
    }
    
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
}