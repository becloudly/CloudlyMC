package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.config.HotReloadManager
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.UUID

/**
 * Main command handler for the Cloudly plugin.
 * Handles all /cloudly subcommands including hot-reload functionality.
 */
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
    private val hotReloadManager = HotReloadManager(plugin)
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val languageManager = plugin.getLanguageManager()
        
        // Handle subcommands
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> {
                // Check admin permission for reload
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(languageManager.getMessage("commands.no_permission"))
                    return true
                }
                handleReloadCommand(sender, args)
            }
            "info" -> {
                // Check admin permission for info
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(languageManager.getMessage("commands.no_permission"))
                    return true
                }
                handleInfoCommand(sender)
            }
            "whitelist" -> {
                // Check whitelist permission for whitelist commands
                if (!sender.hasPermission("cloudly.whitelist")) {
                    sender.sendMessage(languageManager.getMessage("commands.no_permission"))
                    return true
                }
                handleWhitelistCommand(sender, args)
            }
            "help", null -> handleHelpCommand(sender)
            else -> {
                sender.sendMessage(languageManager.getMessage("commands.unknown_subcommand", "subcommand" to args[0]))
                handleHelpCommand(sender)
            }
        }
        
        return true
    }
    
    /**
     * Handles the reload subcommand with optional specific targets.
     * Usage: /cloudly reload [config|lang|all]
     */
    private fun handleReloadCommand(sender: CommandSender, args: Array<out String>) {
        val languageManager = plugin.getLanguageManager()
        val target = args.getOrNull(1)?.lowercase()
        
        when (target) {
            "config" -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_config"))
                hotReloadManager.reloadConfigOnly(sender)
            }
            "lang", "language", "languages" -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_languages"))
                hotReloadManager.reloadLanguagesOnly(sender)
            }
            "all", null -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_full"))
                hotReloadManager.performHotReload(sender)
            }
            else -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.invalid_target", "target" to target))
                sender.sendMessage(languageManager.getMessage("commands.reload.usage"))
            }
        }
    }
    
    /**
     * Handles the info subcommand.
     * Displays plugin information and current configuration status.
     */
    private fun handleInfoCommand(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        val configManager = plugin.getConfigManager()
        
        sender.sendMessage("")
        sender.sendMessage(languageManager.getMessage("commands.info.header"))
        sender.sendMessage(languageManager.getMessage("commands.info.version", "version" to plugin.description.version))
        sender.sendMessage(languageManager.getMessage("commands.info.language", "language" to languageManager.getCurrentLanguage()))
        sender.sendMessage(languageManager.getMessage("commands.info.debug", "debug" to configManager.getBoolean("plugin.debug", false)))
        
        // Server type detection
        val serverType = if (de.cloudly.utils.SchedulerUtils.isFolia()) "Folia" else "Paper/Spigot"
        sender.sendMessage(languageManager.getMessage("commands.info.server_type", "type" to serverType))
        sender.sendMessage(languageManager.getMessage("commands.info.author"))
    }
    
    /**
     * Handles the whitelist subcommand and its nested subcommands.
     * Usage: /cloudly whitelist <add|remove|list|on|off|reload|info>
     */
    private fun handleWhitelistCommand(sender: CommandSender, args: Array<out String>) {
        val languageManager = plugin.getLanguageManager()
        val whitelistService = plugin.getWhitelistService()
        
        if (args.size < 2) {
            sendWhitelistUsage(sender)
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.add_usage"))
                    return
                }
                
                val playerName = args[2]
                val reason = if (args.size > 3) args.drop(3).joinToString(" ") else "Added by admin"
                
                // Try to get UUID from online player first
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    // For offline players, try to get UUID from Bukkit's offline player
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return
                    }
                }
                
                val addedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                
                if (whitelistService.addPlayer(uuid, playerName, addedBy, reason)) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.player_added", "player" to playerName))
                } else {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.add_failed", "player" to playerName))
                }
            }
            
            "remove" -> {
                if (args.size < 3) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.remove_usage"))
                    return
                }
                
                val playerName = args[2]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return
                    }
                }
                
                if (whitelistService.removePlayer(uuid)) {
                    // Check if the player is currently online and kick them
                    val onlinePlayer = Bukkit.getPlayer(uuid)
                    if (onlinePlayer != null && onlinePlayer.isOnline) {
                        // Kick the player with a whitelist message
                        onlinePlayer.kickPlayer(languageManager.getMessage("commands.whitelist.player_removed_kick_message"))
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_removed_and_kicked", "player" to playerName))
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_removed", "player" to playerName))
                    }
                } else {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.player_not_whitelisted", "player" to playerName))
                }
            }
            
            "list" -> {
                val players = whitelistService.getAllPlayers()
                if (players.isEmpty()) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.list_empty"))
                    return
                }
                
                sender.sendMessage(languageManager.getMessage("commands.whitelist.list_header", "count" to players.size.toString()))
                players.forEach { player ->
                    val date = Date.from(player.addedAt)
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.list_entry", 
                        "username" to player.username, 
                        "date" to date.toString()))
                }
            }
            
            "on" -> {
                whitelistService.enable(true)
                sender.sendMessage(languageManager.getMessage("commands.whitelist.enabled"))
            }
            
            "off" -> {
                whitelistService.enable(false)
                sender.sendMessage(languageManager.getMessage("commands.whitelist.disabled"))
            }
            
            "reload" -> {
                whitelistService.reload()
                sender.sendMessage(languageManager.getMessage("commands.whitelist.reloaded"))
            }
            
            "info" -> {
                if (args.size < 3) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_usage"))
                    return
                }
                
                val playerName = args[2]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return
                    }
                }
                
                val whitelistPlayer = whitelistService.getPlayer(uuid)
                if (whitelistPlayer != null) {
                    val addedByUuid = whitelistPlayer.addedBy ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
                    val addedByName = if (addedByUuid == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                        "Console"
                    } else {
                        Bukkit.getOfflinePlayer(addedByUuid).name ?: "Unknown"
                    }
                    
                    val date = Date.from(whitelistPlayer.addedAt)
                    
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_header", "player" to whitelistPlayer.username))
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_added_by", "name" to addedByName))
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_added_on", "date" to date.toString()))
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_reason", "reason" to (whitelistPlayer.reason ?: "Not specified")))
                } else {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.player_not_whitelisted", "player" to playerName))
                }
            }
            
            else -> sendWhitelistUsage(sender)
        }
    }
    
    /**
     * Sends whitelist command usage to the sender.
     */
    private fun sendWhitelistUsage(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        sender.sendMessage(languageManager.getMessage("commands.whitelist.usage"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_add"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_remove"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_list"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_on"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_off"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_reload"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_info"))
    }
    

    
    /**
     * Handles the help subcommand.
     * Displays available commands and their usage.
     */
    private fun handleHelpCommand(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        
        sender.sendMessage("")
        sender.sendMessage(languageManager.getMessage("commands.help.header"))
        
        // Show admin commands if user has admin permission
        if (sender.hasPermission("cloudly.admin")) {
            sender.sendMessage(languageManager.getMessage("commands.help.reload"))
            sender.sendMessage(languageManager.getMessage("commands.help.info"))
        }
        
        // Show whitelist commands if user has whitelist permission
        if (sender.hasPermission("cloudly.whitelist")) {
            sender.sendMessage("§f/cloudly whitelist <add|remove|list|on|off|reload|info> §7- Manage whitelist")
        }
        
        sender.sendMessage(languageManager.getMessage("commands.help.help"))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        return when (args.size) {
            1 -> {
                // Main subcommands
                val subcommands = mutableListOf<String>()
                
                // Admin commands
                if (sender.hasPermission("cloudly.admin")) {
                    subcommands.addAll(listOf("reload", "info"))
                }
                
                // Whitelist commands
                if (sender.hasPermission("cloudly.whitelist")) {
                    subcommands.add("whitelist")
                }
                
                // Help is always available
                subcommands.add("help")
                
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                // Subcommand arguments
                when (args[0].lowercase()) {
                    "reload" -> {
                        if (sender.hasPermission("cloudly.admin")) {
                            listOf("config", "lang", "all")
                                .filter { it.startsWith(args[1].lowercase()) }
                        } else emptyList()
                    }
                    "whitelist" -> {
                        if (sender.hasPermission("cloudly.whitelist")) {
                            listOf("add", "remove", "list", "on", "off", "reload", "info")
                                .filter { it.startsWith(args[1].lowercase()) }
                        } else emptyList()
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                // Three-level arguments (for whitelist player names)
                if (args[0].equals("whitelist", ignoreCase = true) && 
                    sender.hasPermission("cloudly.whitelist") &&
                    (args[1].equals("add", ignoreCase = true) || 
                     args[1].equals("remove", ignoreCase = true) || 
                     args[1].equals("info", ignoreCase = true))) {
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.lowercase().startsWith(args[2].lowercase()) }
                } else emptyList()
            }
            else -> emptyList()
        }
    }
}