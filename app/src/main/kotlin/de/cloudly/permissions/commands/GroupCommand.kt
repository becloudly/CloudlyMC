package de.cloudly.permissions.commands

import de.cloudly.permissions.PermissionManager
import de.cloudly.permissions.utils.PermissionUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.java.JavaPlugin

/**
 * Group management command handler for /group.
 * Handles creation, deletion, modification, and information about permission groups.
 */
class GroupCommand(
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
        if (!sender.hasPermission("cloudly.permissions.group.edit") && !sender.hasPermission("cloudly.permissions.group.list")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return true
        }
        
        if (args.isEmpty()) {
            showGroupHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "delete" -> handleDelete(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            "set" -> handleSet(sender, args)
            "style" -> handleStyle(sender, args)
            "permission" -> handlePermission(sender, args)
            "help" -> showGroupHelp(sender)
            else -> {
                sender.sendMessage("§cUnknown subcommand. Use /group help for available commands.")
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
            
            if (sender.hasPermission("cloudly.permissions.group.create")) {
                subcommands.add("create")
            }
            if (sender.hasPermission("cloudly.permissions.group.delete")) {
                subcommands.add("delete")
            }
            if (sender.hasPermission("cloudly.permissions.group.list")) {
                subcommands.add("list")
            }
            if (sender.hasPermission("cloudly.permissions.group.edit")) {
                subcommands.addAll(listOf("info", "set", "style", "permission"))
            }
            
            subcommands.add("help")
            
            return subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        
        // Tab completion for group names
        if (args.size == 2 && args[0].lowercase() in listOf("delete", "info", "set", "style", "permission")) {
            return permissionManager.getGroupService().getAllGroups()
                .map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        // Tab completion for set subcommands
        if (args.size == 3 && args[0].equals("set", ignoreCase = true)) {
            return listOf("weight", "prefix", "suffix")
                .filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        // Tab completion for style subcommands
        if (args.size == 3 && args[0].equals("style", ignoreCase = true)) {
            return listOf("set", "remove")
                .filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        // Tab completion for style properties
        if (args.size == 4 && args[0].equals("style", ignoreCase = true) && 
            args[2].lowercase() in listOf("set", "remove")) {
            return listOf("prefix", "suffix")
                .filter { it.startsWith(args[3], ignoreCase = true) }
        }
        
        // Tab completion for permission subcommands
        if (args.size == 3 && args[0].equals("permission", ignoreCase = true)) {
            return listOf("add", "remove", "list")
                .filter { it.startsWith(args[2], ignoreCase = true) }
        }
        
        return emptyList()
    }
    
    private fun handleCreate(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.create")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /group create <name> [weight]")
            return
        }
        
        val groupName = args[1].lowercase()
        val weight = if (args.size >= 3) {
            args[2].toIntOrNull() ?: run {
                sender.sendMessage("§cInvalid weight. Must be a positive integer.")
                return
            }
        } else 10 // Default weight
        
        if (weight < 1) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.invalid_weight"))
            return
        }
        
        val success = permissionManager.getGroupService().createGroup(groupName, weight)
        if (success) {
            sender.sendMessage(languageManager.getMessage("permissions.groups.created", "group" to groupName, "weight" to weight.toString()))
        } else {
            sender.sendMessage("§cFailed to create group. Group may already exist or name is invalid.")
        }
    }
    
    private fun handleDelete(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.delete")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /group delete <name>")
            return
        }
        
        val groupName = args[1].lowercase()
        
        if (groupName == "base") {
            sender.sendMessage(languageManager.getMessage("permissions.groups.cannot_delete_base"))
            return
        }
        
        val success = permissionManager.getGroupService().deleteGroup(groupName)
        if (success) {
            sender.sendMessage(languageManager.getMessage("permissions.groups.deleted", "group" to groupName))
            
            // Refresh all player permissions since group was deleted
            permissionManager.refreshAllPlayerPermissions()
        } else {
            sender.sendMessage(languageManager.getMessage("permissions.groups.not_found", "group" to groupName))
        }
    }
    
    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission("cloudly.permissions.group.list")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        val groups = permissionManager.getGroupService().getGroupsByWeight()
        
        if (groups.isEmpty()) {
            sender.sendMessage("§eNo groups found.")
            return
        }
        
        sender.sendMessage("§e§l--- Permission Groups ---")
        sender.sendMessage("§7Groups are listed by weight (highest first):")
        
        groups.forEach { group ->
            val userCount = permissionManager.getStorage().getUsersInGroup(group.name).size
            val prefix = group.prefix?.let { "§f$it" } ?: "§7none"
            val suffix = group.suffix?.let { "§f$it" } ?: "§7none"
            
            sender.sendMessage("§f${group.name} §7(Weight: §e${group.weight}§7, Users: §e$userCount§7)")
            sender.sendMessage("  §7Prefix: $prefix §7| Suffix: $suffix")
            sender.sendMessage("  §7Permissions: §e${group.permissions.size}")
        }
    }
    
    private fun handleInfo(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /group info <name>")
            return
        }
        
        val groupName = args[1].lowercase()
        val group = permissionManager.getGroupService().getGroup(groupName)
        
        if (group == null) {
            sender.sendMessage(languageManager.getMessage("permissions.groups.not_found", "group" to groupName))
            return
        }
        
        val userCount = permissionManager.getStorage().getUsersInGroup(group.name).size
        
        sender.sendMessage("§e§l--- Group Information: ${group.name} ---")
        sender.sendMessage("§7Weight: §f${group.weight}")
        sender.sendMessage("§7Prefix: §f${group.prefix ?: "none"}")
        sender.sendMessage("§7Suffix: §f${group.suffix ?: "none"}")
        sender.sendMessage("§7Users: §f$userCount")
        sender.sendMessage("§7Default Group: §f${group.isDefault}")
        
        if (group.permissions.isNotEmpty()) {
            sender.sendMessage("§7Permissions (${group.permissions.size}):")
            group.permissions.take(15).forEach { permission ->
                sender.sendMessage("  §f- $permission")
            }
            if (group.permissions.size > 15) {
                sender.sendMessage("  §7... and ${group.permissions.size - 15} more")
            }
        } else {
            sender.sendMessage("§7Permissions: §fnone")
        }
    }
    
    private fun handleSet(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 4) {
            sender.sendMessage("§cUsage: /group set <name> <property> <value>")
            sender.sendMessage("§7Properties: weight, prefix, suffix")
            return
        }
        
        val groupName = args[1].lowercase()
        val property = args[2].lowercase()
        val value = args.drop(3).joinToString(" ")
        
        val success = when (property) {
            "weight" -> {
                val weight = value.toIntOrNull()
                if (weight == null || weight < 1) {
                    sender.sendMessage(languageManager.getMessage("permissions.errors.invalid_weight"))
                    return
                }
                permissionManager.getGroupService().setGroupWeight(groupName, weight)
            }
            "prefix" -> {
                val prefix = if (value.equals("none", ignoreCase = true)) null else value
                permissionManager.getGroupService().setGroupPrefix(groupName, prefix)
            }
            "suffix" -> {
                val suffix = if (value.equals("none", ignoreCase = true)) null else value
                permissionManager.getGroupService().setGroupSuffix(groupName, suffix)
            }
            else -> {
                sender.sendMessage("§cInvalid property. Use: weight, prefix, suffix")
                return
            }
        }
        
        if (success) {
            sender.sendMessage("§aSuccessfully updated $property for group '$groupName'.")
            
            // Refresh permissions for affected players
            permissionManager.refreshAllPlayerPermissions()
        } else {
            sender.sendMessage("§cFailed to update group. Group may not exist.")
        }
    }
    
    private fun handlePermission(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage: /group permission <name> <add|remove|list> [permission]")
            return
        }
        
        val groupName = args[1].lowercase()
        val action = args[2].lowercase()
        
        when (action) {
            "add" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /group permission <name> add <permission>")
                    return
                }
                
                val permission = args[3]
                val success = permissionManager.getGroupService().addPermission(groupName, permission)
                
                if (success) {
                    sender.sendMessage(languageManager.getMessage("permissions.groups.permission_added", "permission" to permission, "group" to groupName))
                    permissionManager.refreshAllPlayerPermissions()
                } else {
                    sender.sendMessage("§cFailed to add permission. Group may not exist or permission is invalid.")
                }
            }
            "remove" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /group permission <name> remove <permission>")
                    return
                }
                
                val permission = args[3]
                val success = permissionManager.getGroupService().removePermission(groupName, permission)
                
                if (success) {
                    sender.sendMessage(languageManager.getMessage("permissions.groups.permission_removed", "permission" to permission, "group" to groupName))
                    permissionManager.refreshAllPlayerPermissions()
                } else {
                    sender.sendMessage("§cFailed to remove permission. Group may not exist.")
                }
            }
            "list" -> {
                val permissions = permissionManager.getGroupService().getGroupPermissions(groupName)
                
                if (permissions.isEmpty()) {
                    sender.sendMessage("§eGroup '$groupName' has no permissions.")
                    return
                }
                
                sender.sendMessage("§e§l--- Permissions for group '$groupName' ---")
                permissions.forEach { permission ->
                    sender.sendMessage("§f- $permission")
                }
            }
            else -> {
                sender.sendMessage("§cInvalid action. Use: add, remove, list")
            }
        }
    }
    
    private fun handleStyle(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("cloudly.permissions.group.edit")) {
            sender.sendMessage(languageManager.getMessage("permissions.errors.no_permission"))
            return
        }
        
        if (args.size < 4) {
            sender.sendMessage("§cUsage: /group style <set|remove> <group> <prefix|suffix> [value]")
            sender.sendMessage("§7Examples:")
            sender.sendMessage("§7  /group style set admin prefix &c[Admin]")
            sender.sendMessage("§7  /group style remove admin prefix")
            return
        }
        
        val action = args[1].lowercase()
        val groupName = args[2].lowercase()
        val property = args[3].lowercase()
        
        if (action !in listOf("set", "remove")) {
            sender.sendMessage("§cInvalid action. Use: set, remove")
            return
        }
        
        if (property !in listOf("prefix", "suffix")) {
            sender.sendMessage("§cInvalid property. Use: prefix, suffix")
            return
        }
        
        val success = when (action) {
            "set" -> {
                if (args.size < 5) {
                    sender.sendMessage("§cUsage: /group style set <group> <prefix|suffix> <value>")
                    return
                }
                val value = args.drop(4).joinToString(" ")
                
                when (property) {
                    "prefix" -> permissionManager.getGroupService().setGroupPrefix(groupName, value)
                    "suffix" -> permissionManager.getGroupService().setGroupSuffix(groupName, value)
                    else -> false
                }
            }
            "remove" -> {
                when (property) {
                    "prefix" -> permissionManager.getGroupService().setGroupPrefix(groupName, null)
                    "suffix" -> permissionManager.getGroupService().setGroupSuffix(groupName, null)
                    else -> false
                }
            }
            else -> false
        }
        
        if (success) {
            if (action == "set") {
                val value = args.drop(4).joinToString(" ")
                sender.sendMessage("§aSuccessfully set $property for group '$groupName' to: $value")
            } else {
                sender.sendMessage("§aSuccessfully removed $property from group '$groupName'")
            }
            
            // Refresh permissions for affected players
            permissionManager.refreshAllPlayerPermissions()
        } else {
            sender.sendMessage("§cFailed to update group style. Group may not exist.")
        }
    }
    
    private fun showGroupHelp(sender: CommandSender) {
        sender.sendMessage("§e§l--- Group Management Commands ---")
        sender.sendMessage("§7/group create <name> [weight] §f- Create a new group")
        sender.sendMessage("§7/group delete <name> §f- Delete a group")
        sender.sendMessage("§7/group list §f- List all groups")
        sender.sendMessage("§7/group info <name> §f- Show group information")
        sender.sendMessage("§7/group set <name> <property> <value> §f- Set group properties")
        sender.sendMessage("§7/group style <set|remove> <name> <prefix|suffix> [value] §f- Manage styling")
        sender.sendMessage("§7/group permission <name> <add|remove|list> [perm] §f- Manage permissions")
        sender.sendMessage("§7Properties: §fweight, prefix, suffix")
    }
}
