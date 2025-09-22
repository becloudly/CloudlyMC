package de.cloudly.permissions.commands

import de.cloudly.permissions.PermissionManager
import de.cloudly.permissions.utils.PermissionUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * User permission management command handler for /user.
 * Handles user group assignments, individual permissions, and temporary permissions.
 */
class UserCommand(
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
        if (!sender.hasPermission("cloudly.permissions.user.edit") && !sender.hasPermission("cloudly.permissions.user.view")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return true
        }
        
        if (args.isEmpty()) {
            showUserHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "info" -> handleInfo(sender, args)
            "group" -> handleGroup(sender, args)
            "permission" -> handlePermission(sender, args)
            "cleanup" -> handleCleanup(sender, args)
            "help" -> showUserHelp(sender)
            else -> {
                sender.sendMessage("§cUnknown subcommand. Use /user help for available commands.")
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
            
            if (sender.hasPermission("cloudly.permissions.user.view")) {
                subcommands.add("info")
            }
            if (sender.hasPermission("cloudly.permissions.user.edit")) {
                subcommands.addAll(listOf("group", "permission", "cleanup"))
            }
            
            subcommands.add("help")
            
            return subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        
        // Tab completion for player names
        if (args.size == 2) {
            return plugin.server.onlinePlayers.map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        // Tab completion for group subcommands
        if (args.size == 3 && args[0].equals("group", ignoreCase = true)) {
            return listOf("add", "remove", "list")
                .filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        // Tab completion for permission subcommands
        if (args.size == 3 && args[0].equals("permission", ignoreCase = true)) {
            return listOf("add", "remove", "list")
                .filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        // Tab completion for group names
        if (args.size == 4 && args[0].equals("group", ignoreCase = true) && args[2].equals("add", ignoreCase = true)) {
            return permissionManager.getGroupService().getAllGroups()
                .map { it.name }
                .filter { it.startsWith(args[3], ignoreCase = true) }
        }
        
        return emptyList()
    }
    
    private fun handleInfo(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.user.view")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /user info <player>")
            return
        }
        
        val targetName = args[1]
        val target = plugin.server.getPlayer(targetName)
        
        if (target == null) {
            // Try to find offline user
            val offlineUser = permissionManager.getUserService().getUserByName(targetName)
            if (offlineUser == null) {
                sender.sendMessage("§cPlayer '$targetName' not found.")
                return
            }
            
            showOfflineUserInfo(sender, offlineUser)
            return
        }
        
        showUserInfo(sender, target.uniqueId, target.name)
    }
    
    private fun handleGroup(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.user.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /user group <player> <add|remove|list> [group] [duration]")
            return
        }
        
        val targetName = args[1]
        val action = args[2].lowercase()
        
        val target = plugin.server.getPlayer(targetName)
        val targetUuid = target?.uniqueId ?: run {
            val offlineUser = permissionManager.getUserService().getUserByName(targetName)
            offlineUser?.uuid ?: run {
                sender.sendMessage("§cPlayer '$targetName' not found.")
                return
            }
        }
        
        when (action) {
            "add" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /user group <player> add <group> [duration]")
                    return
                }
                
                val groupName = args[3].lowercase()
                
                if (!permissionManager.getGroupService().groupExists(groupName)) {
                    sender.sendMessage("§cGroup '$groupName' does not exist.")
                    return
                }
                
                val success = if (args.size >= 5) {
                    // Temporary group
                    val duration = args[4]
                    permissionManager.getUserService().addUserToTemporaryGroup(targetUuid, groupName, duration)
                } else {
                    // Permanent group
                    permissionManager.getUserService().addUserToGroup(targetUuid, groupName)
                }
                
                if (success) {
                    val durationText = if (args.size >= 5) " for ${args[4]}" else ""
                    sender.sendMessage(languageManager.getMessage("permissions.users.group_added", "group" to groupName, "player" to targetName))
                    
                    // Refresh player permissions if online
                    target?.let { permissionManager.refreshPlayerPermissions(it) }
                } else {
                    sender.sendMessage("§cFailed to add group. Check console for errors.")
                }
            }
            "remove" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /user group <player> remove <group>")
                    return
                }
                
                val groupName = args[3].lowercase()
                
                if (groupName == "base") {
                    sender.sendMessage("§cCannot remove the base group from a user.")
                    return
                }
                
                val success = permissionManager.getUserService().removeUserFromGroup(targetUuid, groupName)
                
                if (success) {
                    sender.sendMessage(languageManager.getMessage("permissions.users.group_removed", "group" to groupName, "player" to targetName))
                    
                    // Refresh player permissions if online
                    target?.let { permissionManager.refreshPlayerPermissions(it) }
                } else {
                    sender.sendMessage("§cFailed to remove group.")
                }
            }
            "list" -> {
                val groups = permissionManager.getUserService().getUserGroups(targetUuid)
                
                if (groups.isEmpty()) {
                    sender.sendMessage("§e$targetName has no groups.")
                    return
                }
                
                sender.sendMessage("§e§l--- Groups for $targetName ---")
                groups.forEach { group ->
                    sender.sendMessage("§f- $group")
                }
            }
            else -> {
                sender.sendMessage("§cInvalid action. Use: add, remove, list")
            }
        }
    }
    
    private fun handlePermission(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.user.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /user permission <player> <add|remove|list> [permission] [duration]")
            return
        }
        
        val targetName = args[1]
        val action = args[2].lowercase()
        
        val target = plugin.server.getPlayer(targetName)
        val targetUuid = target?.uniqueId ?: run {
            val offlineUser = permissionManager.getUserService().getUserByName(targetName)
            offlineUser?.uuid ?: run {
                sender.sendMessage("§cPlayer '$targetName' not found.")
                return
            }
        }
        
        when (action) {
            "add" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /user permission <player> add <permission> [duration]")
                    return
                }
                
                val permission = args[3]
                
                val success = if (args.size >= 5) {
                    // Temporary permission
                    val duration = args[4]
                    permissionManager.getUserService().addUserTemporaryPermission(targetUuid, permission, duration)
                } else {
                    // Permanent permission
                    permissionManager.getUserService().addUserPermission(targetUuid, permission)
                }
                
                if (success) {
                    val durationText = if (args.size >= 5) " for ${args[4]}" else ""
                    sender.sendMessage(languageManager.getMessage("permissions.users.permission_added", "permission" to permission, "player" to targetName))
                    
                    // Refresh player permissions if online
                    target?.let { permissionManager.refreshPlayerPermissions(it) }
                } else {
                    sender.sendMessage("§cFailed to add permission. Check console for errors.")
                }
            }
            "remove" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /user permission <player> remove <permission>")
                    return
                }
                
                val permission = args[3]
                val success = permissionManager.getUserService().removeUserPermission(targetUuid, permission)
                
                if (success) {
                    sender.sendMessage(languageManager.getMessage("permissions.users.permission_removed", "permission" to permission, "player" to targetName))
                    
                    // Refresh player permissions if online
                    target?.let { permissionManager.refreshPlayerPermissions(it) }
                } else {
                    sender.sendMessage("§cFailed to remove permission.")
                }
            }
            "list" -> {
                val permissions = permissionManager.getUserService().getUserPermissions(targetUuid)
                
                if (permissions.isEmpty()) {
                    sender.sendMessage("§e$targetName has no individual permissions.")
                    return
                }
                
                sender.sendMessage("§e§l--- Individual Permissions for $targetName ---")
                permissions.forEach { permission ->
                    sender.sendMessage("§f- $permission")
                }
            }
            else -> {
                sender.sendMessage("§cInvalid action. Use: add, remove, list")
            }
        }
    }
    
    private fun handleCleanup(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.user.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /user cleanup <player>")
            return
        }
        
        val targetName = args[1]
        val target = plugin.server.getPlayer(targetName)
        val targetUuid = target?.uniqueId ?: run {
            val offlineUser = permissionManager.getUserService().getUserByName(targetName)
            offlineUser?.uuid ?: run {
                sender.sendMessage("§cPlayer '$targetName' not found.")
                return
            }
        }
        
        val removedCount = permissionManager.getUserService().cleanupUserExpiredPermissions(targetUuid)
        sender.sendMessage("§aCleanup completed for $targetName. Removed $removedCount expired items.")
        
        // Refresh player permissions if online
        target?.let { permissionManager.refreshPlayerPermissions(it) }
    }
    
    private fun showUserInfo(sender: CommandSender, uuid: UUID, name: String) {
        val player = plugin.server.getPlayer(uuid)
        val primaryGroup = if (player != null) {
            permissionManager.getPlayerPrimaryGroup(player) ?: "base"
        } else "base"
        val groups = permissionManager.getUserService().getUserGroups(uuid)
        val permissions = permissionManager.getUserService().getUserPermissions(uuid)
        val user = permissionManager.getUserService().getUser(uuid)
        
        sender.sendMessage("§e§l--- User Information: $name ---")
        sender.sendMessage("§7UUID: §f$uuid")
        sender.sendMessage("§7Primary Group: §f$primaryGroup")
        sender.sendMessage("§7Groups (${groups.size}): §f${groups.joinToString(", ")}")
        
        if (user != null) {
            if (user.temporaryGroups.isNotEmpty()) {
                sender.sendMessage("§7Temporary Groups:")
                user.temporaryGroups.forEach { (group, expiry) ->
                    val timeLeft = PermissionUtils.formatDuration(expiry - System.currentTimeMillis() / 1000)
                    sender.sendMessage("  §f- $group §7(expires in $timeLeft)")
                }
            }
            
            if (user.temporaryPermissions.isNotEmpty()) {
                sender.sendMessage("§7Temporary Permissions:")
                user.temporaryPermissions.forEach { (permission, expiry) ->
                    val timeLeft = PermissionUtils.formatDuration(expiry - System.currentTimeMillis() / 1000)
                    sender.sendMessage("  §f- $permission §7(expires in $timeLeft)")
                }
            }
        }
        
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
    
    private fun showOfflineUserInfo(sender: CommandSender, user: de.cloudly.permissions.models.PermissionUser) {
        val groups = user.getActiveGroups()
        val permissions = user.getActivePermissions()
        
        sender.sendMessage("§e§l--- User Information: ${user.username} §7(Offline) ---")
        sender.sendMessage("§7UUID: §f${user.uuid}")
        sender.sendMessage("§7Groups (${groups.size}): §f${groups.joinToString(", ")}")
        sender.sendMessage("§7Last Updated: §f${user.lastUpdated}")
        
        if (user.temporaryGroups.isNotEmpty()) {
            sender.sendMessage("§7Temporary Groups:")
            user.temporaryGroups.forEach { (group, expiry) ->
                val timeLeft = PermissionUtils.formatDuration(expiry - System.currentTimeMillis() / 1000)
                sender.sendMessage("  §f- $group §7(expires in $timeLeft)")
            }
        }
        
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
    
    private fun showUserHelp(sender: CommandSender) {
        sender.sendMessage("§e§l--- User Management Commands ---")
        sender.sendMessage("§7/user info <player> §f- Show user information")
        sender.sendMessage("§7/user group <player> add <group> [duration] §f- Add group to user")
        sender.sendMessage("§7/user group <player> remove <group> §f- Remove group from user")
        sender.sendMessage("§7/user group <player> list §f- List user's groups")
        sender.sendMessage("§7/user permission <player> add <perm> [duration] §f- Add permission")
        sender.sendMessage("§7/user permission <player> remove <perm> §f- Remove permission")
        sender.sendMessage("§7/user permission <player> list §f- List user's permissions")
        sender.sendMessage("§7/user cleanup <player> §f- Clean expired permissions")
        sender.sendMessage("§7Duration examples: §f1d, 2h, 30m, 45s")
    }
}
