package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.discord.DiscordCodeRequestResult
import de.cloudly.utils.TimeUtils
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Main command handler for the Cloudly plugin.
 * Handles all /cloudly subcommands.
 */
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
    // Coroutine scope for async Discord operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Handle subcommands
        when (args.getOrNull(0)?.lowercase()) {
            "info" -> {
                // Check admin permission for info
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleInfoCommand(sender)
            }
            "admin" -> {
                if (!sender.hasPermission("cloudly.admin")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleAdminCommand(sender, args)
            }
            "whitelist" -> {
                // Check whitelist permission for whitelist commands
                if (!sender.hasPermission("cloudly.whitelist")) {
                    sender.sendMessage(Messages.Commands.NO_PERMISSION)
                    return true
                }
                handleWhitelistCommand(sender, args)
            }
            "link" -> {
                // Discord link command - available to all users
                if (sender !is Player) {
                    sender.sendMessage(Messages.Commands.Discord.PLAYERS_ONLY)
                    return true
                }
                handleDiscordLinkCommand(sender, args)
            }
            "unlink" -> {
                if (sender !is Player) {
                    sender.sendMessage(Messages.Commands.Discord.PLAYERS_ONLY)
                    return true
                }
                if (args.size > 1) {
                    sender.sendMessage(Messages.Commands.Discord.UNLINK_USAGE)
                    return true
                }
                handleDiscordUnlinkCommand(sender)
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
     * Handles the admin subcommand.
     * Usage: /cloudly admin gui
     */
    private fun handleAdminCommand(sender: CommandSender, args: Array<out String>) {
        if (args.getOrNull(1)?.equals("gui", ignoreCase = true) != true) {
            sender.sendMessage(Messages.Commands.Admin.GUI_USAGE)
            return
        }

        if (sender !is Player) {
            sender.sendMessage(Messages.Commands.Admin.PLAYERS_ONLY)
            return
        }

        val initialPage = args.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        plugin.getAdminGuiManager().openAdminGui(sender, initialPage)
    }

    /**
     * Handles the whitelist subcommand and its nested subcommands.
     * Usage: /cloudly whitelist <add|remove|list|on|off|info>
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
     * Handles the Discord link command.
     * Usage: /cloudly link <discord_username>
     */
    private fun handleDiscordLinkCommand(sender: Player, args: Array<out String>) {
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
            sender.sendMessage(Messages.Commands.Discord.LINK_USAGE)
            return
        }
        
        val discordUsername = args[1]
        
        // Validate Discord username format (basic validation)
        if (discordUsername.length < 2 || discordUsername.length > 32) {
            sender.sendMessage(Messages.Commands.Discord.INVALID_USERNAME)
            return
        }
        
        sender.sendMessage(Messages.Commands.Discord.verifying(discordUsername))

        coroutineScope.launch {
            try {
                val requestResult = discordService.requestVerificationCode(
                    playerUuid = sender.uniqueId,
                    playerName = sender.name,
                    discordInput = discordUsername,
                    whitelistService = whitelistService
                )

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    when (requestResult) {
                        is DiscordCodeRequestResult.CodeSent -> {
                            sender.sendMessage(Messages.Commands.Discord.CODE_SENT)
                        }
                        DiscordCodeRequestResult.ServiceDisabled -> {
                            sender.sendMessage(Messages.Commands.Discord.DISABLED)
                        }
                        DiscordCodeRequestResult.UserNotFound -> {
                            sender.sendMessage(Messages.Commands.Discord.userNotFound(discordUsername))
                        }
                        DiscordCodeRequestResult.NotServerMember -> {
                            sender.sendMessage(Messages.Commands.Discord.notServerMember(discordUsername))
                        }
                        DiscordCodeRequestResult.MissingRole -> {
                            sender.sendMessage(Messages.Commands.Discord.missingRole(discordUsername))
                        }
                        DiscordCodeRequestResult.AccountAlreadyLinked -> {
                            val discordConnection = whitelistService.getPlayer(sender.uniqueId)?.discordConnection
                            val alreadyName = discordConnection?.discordUsername ?: discordUsername
                            sender.sendMessage(Messages.Commands.Discord.alreadyConnected(alreadyName))
                        }
                        DiscordCodeRequestResult.AccountInUse -> {
                            sender.sendMessage(Messages.Commands.Discord.ACCOUNT_ALREADY_IN_USE)
                        }
                        DiscordCodeRequestResult.PendingAlreadyActive -> {
                            sender.sendMessage(Messages.Commands.Discord.CODE_ALREADY_PENDING)
                        }
                        is DiscordCodeRequestResult.DmFailed -> {
                            sender.sendMessage(Messages.Commands.Discord.CODE_SEND_FAILED)
                            plugin.logger.warning("Discord DM konnte nicht gesendet werden: ${requestResult.reason}")
                        }
                        is DiscordCodeRequestResult.ApiError -> {
                            sender.sendMessage(Messages.Commands.Discord.API_ERROR)
                            plugin.logger.warning("Discord API Fehler: ${requestResult.message}")
                        }
                    }
                })
            } catch (e: Exception) {
                plugin.logger.severe("Fehler bei Discord-Verifizierung: ${e.message}")
                e.printStackTrace()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    sender.sendMessage(Messages.Commands.Discord.VERIFICATION_ERROR)
                })
            }
        }
    }

    /**
     * Handles the Discord unlink command.
     * Usage: /cloudly unlink
     */
    private fun handleDiscordUnlinkCommand(sender: Player) {
        val discordService = plugin.getDiscordService()
        val whitelistService = plugin.getWhitelistService()

        val whitelistPlayer = whitelistService.getPlayer(sender.uniqueId)
        if (whitelistPlayer == null) {
            sender.sendMessage(Messages.Commands.Discord.NOT_WHITELISTED)
            return
        }

        val pendingActive = discordService.hasPendingVerification(sender.uniqueId)
        val discordConnection = whitelistPlayer.discordConnection

        if (!pendingActive && discordConnection == null) {
            sender.sendMessage(Messages.Commands.Discord.UNLINKED_NO_ACCOUNT)
            return
        }

        if (pendingActive) {
            discordService.resetVerificationState(sender.uniqueId)
            sender.sendMessage(Messages.Commands.Discord.UNLINKED_PENDING_CANCELLED)
        }

        if (discordConnection != null) {
            val removed = whitelistService.clearPlayerDiscord(sender.uniqueId, sender.uniqueId)
            if (!removed) {
                sender.sendMessage(Messages.Commands.Discord.UNLINK_FAILED)
                return
            }
            sender.sendMessage(Messages.Commands.Discord.unlinkedSuccessfully(discordConnection.discordUsername))
        }

        discordService.resetVerificationState(sender.uniqueId)
        plugin.getDiscordVerificationListener().restartVerification(sender, force = true)
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
            sender.sendMessage(Messages.Commands.Help.INFO)
            sender.sendMessage(Messages.Commands.Help.ADMIN_GUI)
        }
        
        // Show whitelist commands if user has whitelist permission
        if (sender.hasPermission("cloudly.whitelist")) {
            sender.sendMessage(Messages.Commands.Help.WHITELIST_HEADER)
            sender.sendMessage(Messages.Commands.Help.WHITELIST)
        }
        
        // Show Discord link commands (available to all players)
        sender.sendMessage(Messages.Commands.Help.DISCORD_HEADER)
        sender.sendMessage(Messages.Commands.Help.DISCORD_CONNECT)
        sender.sendMessage(Messages.Commands.Help.DISCORD_UNLINK)

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
                    subcommands.add("admin")
                    subcommands.add("info")
                }
                
                // Whitelist commands
                if (sender.hasPermission("cloudly.whitelist")) {
                    subcommands.add("whitelist")
                }
                
                // Discord link commands are available to all players
                subcommands.add("link")
                subcommands.add("unlink")
                
                // Help is always available
                subcommands.add("help")
                
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                // Subcommand arguments
                when (args[0].lowercase()) {
                    "admin" -> {
                        if (sender.hasPermission("cloudly.admin")) {
                            listOf("gui").filter { it.startsWith(args[1].lowercase()) }
                        } else emptyList()
                    }
                    "whitelist" -> {
                        if (sender.hasPermission("cloudly.whitelist")) {
                            listOf("add", "remove", "list", "on", "off", "info")
                                .filter { it.startsWith(args[1].lowercase()) }
                        } else emptyList()
                    }
                    "link" -> emptyList()
                    "unlink" -> emptyList()
                    else -> emptyList()
                }
            }
            3 -> {
                // Three-level arguments
                when (args[0].lowercase()) {
                    "admin" -> emptyList()
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
