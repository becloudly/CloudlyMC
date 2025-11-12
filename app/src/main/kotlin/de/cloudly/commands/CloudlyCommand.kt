package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.discord.DiscordVerificationResult
import de.cloudly.utils.TimeUtils
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Main command handler for the Cloudly plugin.
 * Handles all /cloudly subcommands.
 */
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
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
        // Handle subcommands
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> {
                // Check admin permission for reload
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleReloadCommand(sender)
            }
            "info" -> {
                // Check admin permission for info
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleInfoCommand(sender)
            }
            "whitelist" -> {
                // Check whitelist permission for whitelist commands
                if (!sender.hasPermission("cloudly.whitelist")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleWhitelistCommand(sender, args)
            }
            "connect" -> {
                // Discord connect command - available to all users
                if (sender !is Player) {
                    sender.sendMessage(Messages.Commands.Discord.PLAYERS_ONLY)
                    return true
                }
                handleDiscordConnectCommand(sender, args)
            }
            "help", null -> handleHelpCommand(sender)
            else -> {
                sender.sendMessage(Messages.Commands.unknownSubcommand(args[0]))
                handleHelpCommand(sender)
            }
        }
        
        return true
    }
    
    /**
     * Handles the reload subcommand.
     * Usage: /cloudly reload
     */
    private fun handleReloadCommand(sender: CommandSender) {
        sender.sendMessage(Messages.Commands.Reload.STARTING_CONFIG)
        try {
            plugin.getConfigManager().reloadConfig()
            sender.sendMessage(Messages.Commands.Reload.CONFIG_SUCCESS)
        } catch (e: Exception) {
            sender.sendMessage(Messages.Commands.Reload.CONFIG_FAILED)
            plugin.logger.warning("Fehler beim Neuladen der Konfiguration: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handles the info subcommand.
     * Displays plugin information and current configuration status.
     */
    private fun handleInfoCommand(sender: CommandSender) {
        val configManager = plugin.getConfigManager()
        
        sender.sendMessage("")
        sender.sendMessage(Messages.Commands.Info.HEADER)
        sender.sendMessage(Messages.Commands.Info.version(plugin.description.version))
        sender.sendMessage(Messages.Commands.Info.debug(configManager.getBoolean("plugin.debug", false)))
        sender.sendMessage(Messages.Commands.Info.SERVER_TYPE)
        sender.sendMessage(Messages.Commands.Info.AUTHOR)
        sender.sendMessage(Messages.Commands.Info.FOOTER)
    }

    /**
     * Handles the whitelist subcommand and its nested subcommands.
     * Usage: /cloudly whitelist <add|remove|list|gui|on|off|reload|info>
     */
    private fun handleWhitelistCommand(sender: CommandSender, args: Array<out String>) {
        val whitelistService = plugin.getWhitelistService()
        
        if (args.size < 2) {
            sender.sendMessage(Messages.Commands.Whitelist.USAGE)
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) {
                    sender.sendMessage(Messages.Commands.Whitelist.ADD_USAGE)
                    return
                }
                
                val playerName = args[2]
                
                // Try to get UUID from online player first
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    // For offline players, get UUID from Bukkit's offline player
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    offlinePlayer.uniqueId
                }
                
                val addedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                
                if (whitelistService.addPlayer(uuid, playerName, addedBy)) {
                    sender.sendMessage(Messages.Commands.Whitelist.playerAdded(playerName))
                } else {
                    sender.sendMessage(Messages.Commands.Whitelist.addFailed(playerName))
                }
            }
            
            "remove" -> {
                if (args.size < 3) {
                    sender.sendMessage(Messages.Commands.Whitelist.REMOVE_USAGE)
                    return
                }
                
                val playerName = args[2]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    offlinePlayer.uniqueId
                }
                
                val removedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                
                if (whitelistService.removePlayer(uuid, removedBy)) {
                    // Check if the player is currently online and kick them
                    val onlinePlayer = Bukkit.getPlayer(uuid)
                    if (onlinePlayer != null && onlinePlayer.isOnline) {
                        onlinePlayer.kickPlayer(Messages.Commands.Whitelist.PLAYER_REMOVED_KICK_MESSAGE)
                        sender.sendMessage(Messages.Commands.Whitelist.playerRemovedAndKicked(playerName))
                    } else {
                        sender.sendMessage(Messages.Commands.Whitelist.playerRemoved(playerName))
                    }
                } else {
                    sender.sendMessage(Messages.Commands.Whitelist.playerNotWhitelisted(playerName))
                }
            }
            
            "list" -> {
                val players = whitelistService.getAllPlayers()
                if (players.isEmpty()) {
                    sender.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
                    return
                }
                
                sender.sendMessage("")
                sender.sendMessage(Messages.Commands.Whitelist.listHeader(players.size))
                players.forEach { player ->
                    val date = TimeUtils.formatTimestamp(player.addedAt)
                    sender.sendMessage(Messages.Commands.Whitelist.listEntry(player.username, date))
                }
                sender.sendMessage(Messages.Commands.Whitelist.LIST_FOOTER)
            }
            
            "gui" -> {
                // Open GUI for players only
                if (sender !is Player) {
                    sender.sendMessage(Messages.Gui.Whitelist.ONLY_PLAYERS)
                    return
                }
                
                plugin.getWhitelistGuiManager().openWhitelistGui(sender)
            }
            
            "on" -> {
                val changedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                whitelistService.enable(true, changedBy)
                sender.sendMessage(Messages.Commands.Whitelist.ENABLED)
            }
            
            "off" -> {
                val changedBy = if (sender is Player) sender.uniqueId else UUID.fromString("00000000-0000-0000-0000-000000000000")
                whitelistService.enable(false, changedBy)
                sender.sendMessage(Messages.Commands.Whitelist.DISABLED)
            }
            
            "reload" -> {
                whitelistService.reload()
                sender.sendMessage(Messages.Commands.Whitelist.RELOADED)
            }
            
            "info" -> {
                if (args.size < 3) {
                    sender.sendMessage(Messages.Commands.Whitelist.INFO_USAGE)
                    return
                }
                
                val playerName = args[2]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
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
                    
                    val date = TimeUtils.formatTimestamp(whitelistPlayer.addedAt)
                    
                    sender.sendMessage("")
                    sender.sendMessage(Messages.Commands.Whitelist.infoHeader(whitelistPlayer.username))
                    sender.sendMessage(Messages.Commands.Whitelist.infoAddedBy(addedByName))
                    sender.sendMessage(Messages.Commands.Whitelist.infoAddedOn(date))
                    
                    // Show Discord connection info if available
                    if (whitelistPlayer.discordConnection != null) {
                        val discord = whitelistPlayer.discordConnection
                        val status = if (discord.verified) "§aVerifiziert" else "§cNicht verifiziert"
                        sender.sendMessage("  §e▪ §fDiscord§8: §7${discord.discordUsername} §8(§7$status§8)")
                    } else {
                        sender.sendMessage("  §e▪ §fDiscord§8: §cNicht verbunden")
                    }
                    sender.sendMessage(Messages.Commands.Whitelist.INFO_FOOTER)
                } else {
                    sender.sendMessage(Messages.Commands.Whitelist.playerNotWhitelisted(playerName))
                }
            }
            
            else -> {
                sender.sendMessage(Messages.Commands.Whitelist.INVALID_SUBCOMMAND)
            }
        }
    }
    
    /**
     * Handles the Discord connect command.
     * Usage: /cloudly connect <discord_username>
     */
    private fun handleDiscordConnectCommand(sender: Player, args: Array<out String>) {
        val discordService = plugin.getDiscordService()
        val whitelistService = plugin.getWhitelistService()
        
        // Check cooldown using Discord service's built-in cooldown tracking
        if (discordService.isOnCooldown(sender.uniqueId)) {
            val remaining = discordService.getRemainingCooldown(sender.uniqueId)
            sender.sendMessage(Messages.Commands.Discord.cooldown(remaining.toInt()))
            return
        }
        
        // Check if Discord service is enabled
        if (!discordService.isEnabled()) {
            sender.sendMessage(Messages.Commands.Discord.DISABLED)
            return
        }
        
        // Check if player is whitelisted
        val whitelistPlayer = whitelistService.getPlayer(sender.uniqueId)
        if (whitelistPlayer == null) {
            sender.sendMessage(Messages.Commands.Discord.NOT_WHITELISTED)
            return
        }
        
        // Check if player already has Discord connected
        if (whitelistPlayer.discordConnection != null && whitelistPlayer.discordConnection.verified) {
            sender.sendMessage(Messages.Commands.Discord.alreadyConnected(whitelistPlayer.discordConnection.discordUsername))
            return
        }
        
        // Check command arguments
        if (args.size < 2) {
            sender.sendMessage(Messages.Commands.Discord.CONNECT_USAGE)
            return
        }
        
        val discordUsername = args[1]
        
        // Validate Discord username format (basic validation)
        if (discordUsername.length < 2 || discordUsername.length > 32) {
            sender.sendMessage(Messages.Commands.Discord.INVALID_USERNAME)
            return
        }
        
        sender.sendMessage(Messages.Commands.Discord.verifying(discordUsername))
        
        // Perform Discord verification using coroutines
        coroutineScope.launch {
            try {
                val verificationResult = discordService.verifyDiscordUser(sender.uniqueId, discordUsername)
                
                // Switch back to main thread for player interaction
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    handleDiscordVerificationResult(sender, verificationResult, discordUsername)
                })
            } catch (e: Exception) {
                plugin.logger.severe("Fehler bei Discord-Verifizierung: ${e.message}")
                e.printStackTrace()
                
                // Send error message to player
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    sender.sendMessage(Messages.Commands.Discord.VERIFICATION_ERROR)
                })
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
        val whitelistService = plugin.getWhitelistService()
        
        when (result) {
            is DiscordVerificationResult.Success -> {
                // Create Discord connection
                val discordConnection = plugin.getDiscordService().createDiscordConnection(result)
                
                // Update whitelist player with Discord connection
                val currentPlayer = whitelistService.getPlayer(sender.uniqueId)
                if (currentPlayer != null) {
                    if (whitelistService.updatePlayerDiscord(sender.uniqueId, discordConnection, sender.uniqueId)) {
                        sender.sendMessage(Messages.Commands.Discord.connectedSuccessfully(result.username))
                        
                        // Mark player as verified if verification is required
                        if (plugin.getConfigManager().getBoolean("discord.require_verification", false)) {
                            plugin.getDiscordVerificationListener().markPlayerVerified(sender)
                        }
                    } else {
                        sender.sendMessage(Messages.Commands.Discord.CONNECTION_FAILED)
                    }
                } else {
                    sender.sendMessage(Messages.Commands.Discord.NOT_WHITELISTED)
                }
            }
            is DiscordVerificationResult.UserNotFound -> {
                sender.sendMessage(Messages.Commands.Discord.userNotFound(inputUsername))
            }
            is DiscordVerificationResult.NotServerMember -> {
                sender.sendMessage(Messages.Commands.Discord.notServerMember(inputUsername))
            }
            is DiscordVerificationResult.MissingRole -> {
                sender.sendMessage(Messages.Commands.Discord.missingRole(inputUsername))
            }
            is DiscordVerificationResult.ServiceDisabled -> {
                sender.sendMessage(Messages.Commands.Discord.DISABLED)
            }
            is DiscordVerificationResult.ApiError -> {
                sender.sendMessage(Messages.Commands.Discord.API_ERROR)
                plugin.logger.warning("Discord API Fehler: ${result.message}")
            }
        }
    }
    
    /**
     * Handles the help subcommand.
     * Displays available commands and their usage.
     */
    private fun handleHelpCommand(sender: CommandSender) {
        sender.sendMessage("")
        sender.sendMessage(Messages.Commands.Help.HEADER)
        
        // Show admin commands if user has admin permission
        if (sender.hasPermission("cloudly.admin")) {
            sender.sendMessage(Messages.Commands.Help.ADMIN_HEADER)
            sender.sendMessage(Messages.Commands.Help.RELOAD)
            sender.sendMessage(Messages.Commands.Help.INFO)
        }
        
        // Show whitelist commands if user has whitelist permission
        if (sender.hasPermission("cloudly.whitelist")) {
            sender.sendMessage(Messages.Commands.Help.WHITELIST_HEADER)
            sender.sendMessage(Messages.Commands.Help.WHITELIST)
        }
        
        // Show Discord connect command (available to all players)
        sender.sendMessage(Messages.Commands.Help.DISCORD_HEADER)
        sender.sendMessage(Messages.Commands.Help.DISCORD_CONNECT)

        // Show general commands
        sender.sendMessage(Messages.Commands.Help.GENERAL_HEADER)
        sender.sendMessage(Messages.Commands.Help.HELP)
        
        sender.sendMessage(Messages.Commands.Help.SEPARATOR)
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
            plugin.logger.warning("Fehler beim Herunterfahren von CloudlyCommand: ${e.message}")
        }
    }
}
