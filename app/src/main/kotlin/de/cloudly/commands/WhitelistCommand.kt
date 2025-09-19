package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.config.LanguageManager
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

class WhitelistCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
    private val languageManager: LanguageManager = plugin.getLanguageManager()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val whitelistService = plugin.getWhitelistService()
        
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (args.size < 2) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.add_usage"))
                    return true
                }
                
                val playerName = args[1]
                val reason = if (args.size > 2) args.drop(2).joinToString(" ") else "Added by admin"
                
                // Try to get UUID from online player first
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    // For offline players, try to get UUID from Bukkit's offline player
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return true
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
                if (args.size < 2) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.remove_usage"))
                    return true
                }
                
                val playerName = args[1]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return true
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
                    return true
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
                if (args.size < 2) {
                    sender.sendMessage(languageManager.getMessage("commands.whitelist.info_usage"))
                    return true
                }
                
                val playerName = args[1]
                val targetPlayer = Bukkit.getPlayer(playerName)
                val uuid = targetPlayer?.uniqueId ?: run {
                    val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                    if (offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer.uniqueId
                    } else {
                        sender.sendMessage(languageManager.getMessage("commands.whitelist.player_never_played", "player" to playerName))
                        return true
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
            
            else -> sendUsage(sender)
        }
        
        return true
    }
    
    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(languageManager.getMessage("commands.whitelist.usage"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_add"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_remove"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_list"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_on"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_off"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_reload"))
        sender.sendMessage(languageManager.getMessage("commands.whitelist.help_info"))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val subcommands = listOf("add", "remove", "list", "on", "off", "reload", "info")
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && (args[0].equals("add", ignoreCase = true) || 
                              args[0].equals("remove", ignoreCase = true) || 
                              args[0].equals("info", ignoreCase = true))) {
            return Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }
        
        return emptyList()
    }
}