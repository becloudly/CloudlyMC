/*
 * Cloudly - Base Command System
 * 
 * Provides a foundation for creating commands with built-in
 * permission checking, error handling, and performance optimizations.
 */
package cloudly.command

import cloudly.util.CloudlyUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Base class for all Cloudly commands
 * Provides common functionality and standardized error handling
 */
abstract class BaseCommand : CommandExecutor, TabCompleter {
    
    /**
     * The permission required to execute this command
     * Override in subclasses to set specific permissions
     */
    open val permission: String? = null
    
    /**
     * Whether this command can only be executed by players
     * Override in subclasses to allow console execution
     */
    open val playerOnly: Boolean = false
    
    /**
     * Main command execution logic
     * Override this method in subclasses to implement command functionality
     */
    abstract fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean
    
    /**
     * Tab completion logic
     * Override this method in subclasses to provide custom tab completion
     */
    open fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        return emptyList()
    }
    
    /**
     * Bukkit CommandExecutor implementation
     * Handles permission checking and delegates to execute method
     */
    final override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        try {
            // Check if command is player-only
            if (playerOnly && sender !is Player) {
                CloudlyUtils.sendPrefixedMessage(sender, "&cThis command can only be used by players.")
                return true
            }
            
            // Check permissions
            if (permission != null && sender is Player && !CloudlyUtils.hasPermission(sender, permission!!)) {
                CloudlyUtils.sendPrefixedMessage(sender, "&cYou don't have permission to use this command.")
                return true
            }
            
            // Execute the command
            return execute(sender, command, args)
            
        } catch (e: Exception) {
            CloudlyUtils.sendPrefixedMessage(sender, "&cAn error occurred while executing this command.")
            e.printStackTrace()
            return true
        }
    }
    
    /**
     * Bukkit TabCompleter implementation
     * Delegates to complete method with error handling
     */
    final override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        try {
            // Check permissions for tab completion
            if (permission != null && sender is Player && !CloudlyUtils.hasPermission(sender, permission!!)) {
                return emptyList()
            }
            
            return complete(sender, command, args)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Helper method to send usage message
     */
    protected fun sendUsage(sender: CommandSender, usage: String) {
        CloudlyUtils.sendPrefixedMessage(sender, "&cUsage: &f$usage")
    }
    
    /**
     * Helper method to check if sender is a player and cast safely
     */
    protected fun requirePlayer(sender: CommandSender): Player? {
        if (sender !is Player) {
            CloudlyUtils.sendPrefixedMessage(sender, "&cThis command can only be used by players.")
            return null
        }
        return sender
    }
}

/**
 * Example command implementation
 * Remove this when you create your actual commands
 */
class ExampleCommand : BaseCommand() {
    
    override val permission = "cloudly.command.example"
    override val playerOnly = false
    
    override fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean {
        when {
            args.isEmpty() -> {
                CloudlyUtils.sendPrefixedMessage(sender, "&aCloudly is running! Version: ${org.bukkit.Bukkit.getPluginManager().getPlugin("Cloudly")?.description?.version}")
                return true
            }
            
            args[0].equals("reload", ignoreCase = true) -> {
                if (sender is Player && !CloudlyUtils.hasPermission(sender, "cloudly.admin")) {
                    CloudlyUtils.sendPrefixedMessage(sender, "&cYou don't have permission to reload the plugin.")
                    return true
                }
                
                // Reload logic would go here
                CloudlyUtils.sendPrefixedMessage(sender, "&aCloudly reloaded successfully!")
                return true
            }
            
            else -> {
                sendUsage(sender, "/cloudly [reload]")
                return true
            }
        }
    }
    
    override fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf<String>()
                if (sender !is Player || CloudlyUtils.hasPermission(sender, "cloudly.admin")) {
                    suggestions.add("reload")
                }
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}
