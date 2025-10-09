package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.config.HotReloadManager
import de.cloudly.discord.DiscordVerificationResult
import de.cloudly.utils.SchedulerUtils
import de.cloudly.whitelist.model.WhitelistPlayer
import kotlinx.coroutines.*
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Main command handler for the Cloudly plugin.
 * Handles all /cloudly subcommands including hot-reload functionality.
 */
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
    private val hotReloadManager = HotReloadManager(plugin)
    
    // Coroutine scope for async Discord operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Cooldown tracking for rate limiting
    private val cooldowns = ConcurrentHashMap<UUID, Long>()
    
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
            "connect" -> {
                // Discord connect command - available to all users
                if (sender !is Player) {
                    sender.sendMessage(languageManager.getMessage("commands.discord.players_only"))
                    return true
                }
                handleDiscordConnectCommand(sender, args)
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
     * Check if a player is on cooldown for a command.
     * 
     * @param player The player to check
     * @param seconds The cooldown duration in seconds
     * @return true if the player can use the command, false if still on cooldown
     */
    private fun checkCooldown(player: Player, seconds: Int): Boolean {
        val now = System.currentTimeMillis()
        val lastUse = cooldowns[player.uniqueId] ?: 0
        return if (now - lastUse >= seconds * 1000) {
            cooldowns[player.uniqueId] = now
            true
        } else {
            false
        }
    }
    
    /**
     * Get the remaining cooldown time for a player in seconds.
     * 
     * @param player The player to check
     * @param seconds The cooldown duration in seconds
     * @return The remaining cooldown time in seconds, or 0 if no cooldown
     */
    private fun getRemainingCooldown(player: Player, seconds: Int): Long {
        val now = System.currentTimeMillis()
        val lastUse = cooldowns[player.uniqueId] ?: 0
        val elapsed = now - lastUse
        val cooldownMs = seconds * 1000L
        return if (elapsed < cooldownMs) {
            (cooldownMs - elapsed) / 1000
        } else {
            0
        }
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
            sender.sendMessage(languageManager.getMessage("commands.whitelist.usage"))
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.add_usage"))
                    return
                }
                
                val playerName = args[2]
                
                // Try to get UUID from online player first
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    // For offline players, get UUID from Bukkit's offline player (works even if they never played)
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    offlinePlayer.uniqueId
                }
                
                val addedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                
                if (whitelistService.addPlayer(uuid, playerName, addedBy)) {
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
                    // For offline players, get UUID from Bukkit's offline player (works even if they never played)
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    offlinePlayer.uniqueId
                }
                
                val removedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                
                if (whitelistService.removePlayer(uuid, removedBy)) {
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
            
            "gui" -> {
                // Open GUI for players only
                if (sender !is Player) {
                    sender.sendMessage(languageManager.getMessage("gui.whitelist.only_players"))
                    return
                }
                
                plugin.getWhitelistGuiManager().openWhitelistGui(sender)
            }
            
            "on" -> {
                val changedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                whitelistService.enable(true, changedBy)
                sender.sendMessage(languageManager.getMessage("commands.whitelist.enabled"))
            }
            
            "off" -> {
                val changedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                whitelistService.enable(false, changedBy)
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
                    // For offline players, get UUID from Bukkit's offline player (works even if they never played)
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    offlinePlayer.uniqueId
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
                    
                    // Show Discord connection info if available
                    if (whitelistPlayer.discordConnection != null) {
                        val discord = whitelistPlayer.discordConnection
                        val status = if (discord.verified) "§aVerified" else "§cNot verified"
                        sender.sendMessage("§7- Discord: §f${discord.discordUsername} §7($status§7)")
                    } else {
                        sender.sendMessage("§7- Discord: §cNot connected")
                    }
                } else {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.player_not_whitelisted", "player" to playerName))
                }
            }
            
            else -> {
                sender.sendMessage(languageManager.getMessage("commands.whitelist.invalid_subcommand"))
            }
        }
    }
    
    /**
     * Handles the Discord connect command.
     * Usage: /cloudly connect <discord_username>
     */
    private fun handleDiscordConnectCommand(sender: Player, args: Array<out String>) {
        val languageManager = plugin.getLanguageManager()
        val discordService = plugin.getDiscordService()
        val whitelistService = plugin.getWhitelistService()
        
        // Check cooldown to prevent command spam and API abuse
        val cooldownSeconds = 30
        if (!checkCooldown(sender, cooldownSeconds)) {
            val remaining = getRemainingCooldown(sender, cooldownSeconds)
            sender.sendMessage(languageManager.getMessage("commands.discord.cooldown", 
                "seconds" to remaining.toString()))
            return
        }
        
        // Check if Discord service is enabled
        if (!discordService.isEnabled()) {
            sender.sendMessage(languageManager.getMessage("commands.discord.disabled"))
            return
        }
        
        // Check if player is whitelisted
        val whitelistPlayer = whitelistService.getPlayer(sender.uniqueId)
        if (whitelistPlayer == null) {
            sender.sendMessage(languageManager.getMessage("commands.discord.not_whitelisted"))
            return
        }
        
        // Check if player already has Discord connected
        if (whitelistPlayer.discordConnection != null && whitelistPlayer.discordConnection.verified) {
            sender.sendMessage(languageManager.getMessage("commands.discord.already_connected", 
                "discord_username" to whitelistPlayer.discordConnection.discordUsername))
            return
        }
        
        // Check command arguments
        if (args.size < 2) {
            sender.sendMessage(languageManager.getMessage("commands.discord.connect_usage"))
            return
        }
        
        val discordUsername = args[1]
        
        // Validate Discord username format (basic validation)
        if (discordUsername.length < 2 || discordUsername.length > 32) {
            sender.sendMessage(languageManager.getMessage("commands.discord.invalid_username"))
            return
        }
        
        sender.sendMessage(languageManager.getMessage("commands.discord.verifying", "discord_username" to discordUsername))
        
        // Check if Discord service is available
        if (!discordService.isEnabled()) {
            sender.sendMessage(languageManager.getMessage("commands.discord.disabled"))
            return
        }
        
        // Perform Discord verification using pure coroutines instead of mixing with schedulers
        coroutineScope.launch {
            try {
                val verificationResult = discordService.verifyDiscordUser(discordUsername)
                
                // Switch back to main thread for player interaction
                // Use a simple approach that works on both Paper and Folia
                if (SchedulerUtils.isFolia()) {
                    // On Folia, try to run the task directly
                    try {
                        handleDiscordVerificationResult(sender, verificationResult, discordUsername)
                    } catch (e: Exception) {
                        plugin.logger.severe("Error handling Discord verification result on Folia: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    // On Paper, use scheduler
                    SchedulerUtils.runTask(plugin, Runnable {
                        handleDiscordVerificationResult(sender, verificationResult, discordUsername)
                    })
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error during Discord verification: ${e.message}")
                e.printStackTrace()
                
                // Send error message to player - handle threading safely
                if (SchedulerUtils.isFolia()) {
                    try {
                        sender.sendMessage(languageManager.getMessage("commands.discord.verification_error"))
                    } catch (ex: Exception) {
                        plugin.logger.severe("Error sending error message on Folia: ${ex.message}")
                    }
                } else {
                    SchedulerUtils.runTask(plugin, Runnable {
                        sender.sendMessage(languageManager.getMessage("commands.discord.verification_error"))
                    })
                }
            }
        }
    }
    
    /**
     * Handle the result of Discord verification.
     */
    private fun handleDiscordVerificationResult(
        sender: Player, 
        result: DiscordVerificationResult, 
        inputUsername: String
    ) {
        val languageManager = plugin.getLanguageManager()
        val whitelistService = plugin.getWhitelistService()
        
        when (result) {
            is DiscordVerificationResult.Success -> {
                // Create Discord connection
                val discordConnection = plugin.getDiscordService().createDiscordConnection(result)
                
                // Update whitelist player with Discord connection
                val currentPlayer = whitelistService.getPlayer(sender.uniqueId)
                if (currentPlayer != null) {
                    val updatedPlayer = currentPlayer.copy(discordConnection = discordConnection)
                    
                    if (whitelistService.updatePlayerDiscord(sender.uniqueId, discordConnection, sender.uniqueId)) {
                        sender.sendMessage(languageManager.getMessage("commands.discord.connected_successfully", 
                            "discord_username" to result.username))
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.discord.connection_failed"))
                    }
                } else {
                    sender.sendMessage(languageManager.getMessage("commands.discord.not_whitelisted"))
                }
            }
            is DiscordVerificationResult.UserNotFound -> {
                sender.sendMessage(languageManager.getMessage("commands.discord.user_not_found", 
                    "discord_username" to inputUsername))
            }
            is DiscordVerificationResult.NotServerMember -> {
                sender.sendMessage(languageManager.getMessage("commands.discord.not_server_member", 
                    "discord_username" to inputUsername))
            }
            is DiscordVerificationResult.ServiceDisabled -> {
                sender.sendMessage(languageManager.getMessage("commands.discord.disabled"))
            }
            is DiscordVerificationResult.ApiError -> {
                sender.sendMessage(languageManager.getMessage("commands.discord.api_error"))
                plugin.logger.warning("Discord API error: ${result.message}")
            }
        }
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
            sender.sendMessage(languageManager.getMessage("commands.help.whitelist"))
        }
        
        // Show Discord connect command (available to all players)
        sender.sendMessage(languageManager.getMessage("commands.help.discord.connect"))

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
                
                // Discord connect is available to all players
                subcommands.add("connect")
                
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
                            listOf("add", "remove", "list", "gui", "on", "off", "reload", "info")
                                .filter { it.startsWith(args[1].lowercase()) }
                        } else emptyList()
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                // Three-level arguments
                when (args[0].lowercase()) {
                    "whitelist" -> {
                        if (sender.hasPermission("cloudly.whitelist") &&
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
            else -> emptyList()
        }
    }
    
    /**
     * Shutdown the command handler and clean up resources.
     */
    fun shutdown() {
        try {
            coroutineScope.cancel()
        } catch (e: Exception) {
            plugin.logger.warning("Error shutting down CloudlyCommand: ${e.message}")
        }
    }
}