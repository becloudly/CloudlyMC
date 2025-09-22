package de.cloudly.permissions.commands

import de.cloudly.permissions.PermissionManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main permission system command handler for /perms.
 * Provides information, reload, and system management functionality.
 */
class PermissionCommand(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager
) : CommandExecutor, TabCompleter {
    
    private val languageManager = (plugin as de.cloudly.CloudlyPaper).getLanguageManager()
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!permissionManager.isEnabled()) {
            sender.sendMessage("§cPermission system is disabled.")
            return true
        }
        
        // Check base permission
        if (!sender.hasPermission("cloudly.permissions.admin") && !sender.hasPermission("cloudly.permissions.info")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return true
        }
        
        if (args.isEmpty()) {
            showMainHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "info" -> handleInfo(sender, args)
            "check" -> handleCheck(sender, args)
            "cleanup" -> handleCleanup(sender)
            "stats" -> handleStats(sender)
            "help" -> showMainHelp(sender)
            else -> {
                sender.sendMessage("§cUnknown subcommand. Use /perms help for available commands.")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!permissionManager.isEnabled()) {
            return emptyList()
        }
        
        if (args.size == 1) {
            val subcommands = mutableListOf<String>()
            
            if (sender.hasPermission("cloudly.permissions.reload")) {
                subcommands.add("reload")
            }
            if (sender.hasPermission("cloudly.permissions.info")) {
                subcommands.addAll(listOf("info", "check", "stats"))
            }
            if (sender.hasPermission("cloudly.permissions.admin")) {
                subcommands.add("cleanup")
            }
            
            subcommands.add("help")
            
            return subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        
        if (args.size == 2 && args[0].equals("info", ignoreCase = true)) {
            return plugin.server.onlinePlayers.map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        if (args.size == 2 && args[0].equals("check", ignoreCase = true)) {
            return plugin.server.onlinePlayers.map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        if (args.size == 3 && args[0].equals("check", ignoreCase = true)) {
            // Return common permissions for tab completion
            return listOf(
                "minecraft.command.me",
                "minecraft.command.tell",
                "minecraft.command.help",
                "cloudly.permissions.admin",
                "cloudly.whitelist.use"
            ).filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        return emptyList()
    }
    
    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("cloudly.permissions.reload")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        sender.sendMessage("§eReloading permission system...")
        
        val success = permissionManager.reload()
        if (success) {
            sender.sendMessage(languageManager.getMessage("permissions.system.reloaded"))
        } else {
            sender.sendMessage("§cFailed to reload permission system. Check console for errors.")
        }
    }
    
    private fun handleInfo(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.info")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            if (sender is Player) {
                showPlayerInfo(sender, sender)
            } else {
                sender.sendMessage("§cPlease specify a player name.")
            }
            return
        }
        
        val targetName = args[1]
        val target = plugin.server.getPlayer(targetName)
        
        if (target == null) {
            sender.sendMessage("§cPlayer '$targetName' not found.")
            return
        }
        
        showPlayerInfo(sender, target)
    }
    
    private fun handleCheck(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.info")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /perms check <player> <permission>")
            return
        }
        
        val targetName = args[1]
        val permission = args[2]
        val target = plugin.server.getPlayer(targetName)
        
        if (target == null) {
            sender.sendMessage("§cPlayer '$targetName' not found.")
            return
        }
        
        val hasPermission = permissionManager.hasPermission(target, permission)
        val status = if (hasPermission) "§aTRUE" else "§cFALSE"
        
        sender.sendMessage("§ePermission Check:")
        sender.sendMessage("§7Player: §f${target.name}")
        sender.sendMessage("§7Permission: §f$permission")
        sender.sendMessage("§7Result: $status")
    }
    
    private fun handleCleanup(sender: CommandSender) {
        if (!sender.hasPermission("cloudly.permissions.admin")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        sender.sendMessage("§eRunning permission cleanup...")
        
        val cleanedUsers = permissionManager.getStorage().cleanupExpiredPermissions()
        sender.sendMessage("§aCleanup completed. Cleaned expired permissions for $cleanedUsers users.")
    }
    
    private fun handleStats(sender: CommandSender) {
        if (!sender.hasPermission("cloudly.permissions.info")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        val stats = permissionManager.getSystemInfo()
        
        sender.sendMessage("§e§l--- Permission System Statistics ---")
        sender.sendMessage("§7Enabled: §f${stats["enabled"]}")
        sender.sendMessage("§7Online Players: §f${stats["onlinePlayers"]}")
        sender.sendMessage("§7Total Groups: §f${stats["groups"]}")
        sender.sendMessage("§7Total Users: §f${stats["users"]}")
        sender.sendMessage("§7Discovered Permissions: §f${stats["discoveredPermissions"]}")
        sender.sendMessage("§7Permission Cache Size: §f${stats["permissionCacheSize"]}")
        
        val formattingStats = stats["formattingStats"] as? Map<*, *>
        if (formattingStats != null) {
            sender.sendMessage("§7Chat Formatting: §f${formattingStats["chatEnabled"]}")
            sender.sendMessage("§7Tablist Formatting: §f${formattingStats["tablistEnabled"]}")
            sender.sendMessage("§7Nametag Formatting: §f${formattingStats["nametagEnabled"]}")
        }
    }
    
    private fun showPlayerInfo(sender: CommandSender, target: Player) {
        val primaryGroup = permissionManager.getPlayerPrimaryGroup(target) ?: "base"
        val prefix = permissionManager.getPlayerPrefix(target) ?: ""
        val suffix = permissionManager.getPlayerSuffix(target) ?: ""
        val userService = permissionManager.getUserService()
        val groups = userService.getUserGroups(target.uniqueId)
        val permissions = userService.getUserPermissions(target.uniqueId)
        
        sender.sendMessage("§e§l--- Player Information: ${target.name} ---")
        sender.sendMessage("§7UUID: §f${target.uniqueId}")
        sender.sendMessage("§7Primary Group: §f$primaryGroup")
        sender.sendMessage("§7Prefix: §f$prefix")
        sender.sendMessage("§7Suffix: §f$suffix")
        sender.sendMessage("§7Groups (${groups.size}): §f${groups.joinToString(", ")}")
        
        if (permissions.isNotEmpty()) {
            sender.sendMessage("§7Individual Permissions (${permissions.size}):")
            permissions.take(10).forEach { permission ->
                sender.sendMessage("  §f- $permission")
            }
            if (permissions.size > 10) {
                sender.sendMessage("  §7... and ${permissions.size - 10} more")
            }
        } else {
            sender.sendMessage("§7Individual Permissions: §fnone")
        }
    }
    
    private fun showMainHelp(sender: CommandSender) {
        sender.sendMessage("§e§l--- CloudlyMC Permission Commands ---")
        sender.sendMessage("§7/perms reload §f- Reload the permission system")
        sender.sendMessage("§7/perms info [player] §f- Show player permission info")
        sender.sendMessage("§7/perms check <player> <permission> §f- Check if player has permission")
        sender.sendMessage("§7/perms cleanup §f- Clean up expired permissions")
        sender.sendMessage("§7/perms stats §f- Show system statistics")
        sender.sendMessage("§7/group <subcommand> §f- Group management commands")
        sender.sendMessage("§7/user <subcommand> §f- User management commands")
    }
}
