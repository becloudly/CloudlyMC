/*
 * Cloudly - Base Command Class
 * 
 * Abstract base class for all Cloudly commands providing common functionality
 * like permission checking, usage messages, and tab completion.
 * All methods are null-safe and handle errors gracefully.
 */
package cloudly.command

import cloudly.util.CloudlyUtils
import cloudly.util.LanguageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

/**
 * Abstract base class for all plugin commands
 * Provides common functionality and enforces a consistent structure
 * All operations are null-safe and error-resistant
 */
abstract class BaseCommand : CommandExecutor, TabCompleter {
    
    /**
     * The permission required to use this command
     * Override this in subclasses
     */
    protected abstract val permission: String?
    
    /**
     * Whether this command can only be used by players
     * Override this in subclasses
     */
    protected abstract val playerOnly: Boolean
    
    /**
     * Execute the command logic
     * Override this in subclasses to implement command functionality
     */
    protected abstract fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean
    
    /**
     * Provide tab completion suggestions
     * Override this in subclasses to provide custom tab completion
     */
    protected open fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        return emptyList()
    }
    
    /**
     * Main command executor - handles permission checking and delegation
     * All operations are null-safe and handle errors gracefully
     */
    final override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        return try {
            // Validate input parameters
            if (!isValidCommandInput(sender, command, args)) {
                return true
            }
            
            // Check if command is player-only and sender is not a player
            if (playerOnly && !isPlayer(sender)) {
                sendSafeMessage(sender, "common.player-only")
                return true
            }
            
            // Check permissions
            if (!hasPermissionSafely(sender)) {
                sendSafeMessage(sender, "common.no-permission")
                return true
            }
            
            // Execute the command
            executeCommand(sender, command, args)
            
        } catch (e: Exception) {
            handleCommandError(sender, e)
            true
        }
    }
    
    /**
     * Tab completion handler - null-safe and error-resistant
     */
    final override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String>? {
        return try {
            // Validate input
            if (!isValidTabCompleteInput(sender, command, args)) {
                return emptyList()
            }
            
            // Check permissions for tab completion
            if (!hasPermissionSafely(sender)) {
                return emptyList()
            }
            
            complete(sender, command, args) ?: emptyList()
            
        } catch (e: Exception) {
            // Log error but don't show to user for tab completion
            logError("Tab completion error", e)
            emptyList()
        }
    }
    
    /**
     * Validate command input parameters
     */
    private fun isValidCommandInput(sender: CommandSender?, command: Command?, args: Array<String>?): Boolean {
        if (sender == null) {
            logError("Command sender is null", null)
            return false
        }
        if (command == null) {
            logError("Command is null", null)
            return false
        }
        if (args == null) {
            logError("Command args are null", null)
            return false
        }
        return true
    }
    
    /**
     * Validate tab complete input parameters
     */
    private fun isValidTabCompleteInput(sender: CommandSender?, command: Command?, args: Array<String>?): Boolean {
        return sender != null && command != null && args != null
    }
    
    /**
     * Safely check if sender is a player
     */
    private fun isPlayer(sender: CommandSender?): Boolean {
        return try {
            sender is Player
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Execute command with error handling
     */
    private fun executeCommand(sender: CommandSender, command: Command, args: Array<String>): Boolean {
        return try {
            execute(sender, command, args)
        } catch (e: Exception) {
            handleCommandError(sender, e)
            true
        }
    }
    
    /**
     * Handle command execution errors
     */
    private fun handleCommandError(sender: CommandSender?, e: Exception) {
        try {
            logError("Command execution error", e)
            sendSafeMessage(sender, "common.error-occurred")
        } catch (errorHandlingException: Exception) {
            // Final fallback - log to console
            System.err.println("Critical error handling command error: ${errorHandlingException.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Check if the sender has permission to use this command - null-safe
     */
    private fun hasPermissionSafely(sender: CommandSender?): Boolean {
        return try {
            if (sender == null) return false
            
            val requiredPermission = permission
            if (requiredPermission.isNullOrBlank()) {
                return true // No permission required
            }
            
            when (sender) {
                is Player -> CloudlyUtils.hasPermission(sender, requiredPermission)
                else -> true // Console always has permission
            }
        } catch (e: Exception) {
            logError("Error checking permissions", e)
            false // Deny access on error for security
        }
    }
    
    /**
     * Send a message safely with error handling
     */
    private fun sendSafeMessage(sender: CommandSender?, messageKey: String, vararg args: Any) {
        try {
            if (sender != null && !messageKey.isBlank()) {
                CloudlyUtils.sendPrefixedTranslatedMessage(sender, messageKey, *args)
            }
        } catch (e: Exception) {
            try {
                // Fallback to direct message
                sender?.sendMessage("Error: $messageKey")
            } catch (fallbackError: Exception) {
                logError("Failed to send fallback message", fallbackError)
            }
        }
    }
    
    /**
     * Send a usage message to the command sender - null-safe
     */
    protected fun sendUsage(sender: CommandSender?, usage: String?) {
        try {
            if (sender != null && !usage.isNullOrBlank()) {
                CloudlyUtils.sendPrefixedTranslatedMessage(sender, "common.usage-format", usage)
            }
        } catch (e: Exception) {
            try {
                // Fallback usage message
                sender?.sendMessage("Usage: ${usage ?: "Unknown"}")
            } catch (fallbackError: Exception) {
                logError("Failed to send usage message", fallbackError)
            }
        }
    }
    
    /**
     * Safe method to send translated messages
     */
    protected fun sendTranslatedMessage(sender: CommandSender?, key: String?, vararg args: Any) {
        try {
            if (sender != null && !key.isNullOrBlank()) {
                CloudlyUtils.sendTranslatedMessage(sender, key, *args)
            }
        } catch (e: Exception) {
            try {
                // Fallback message
                sender?.sendMessage("$key: ${args.joinToString(", ")}")
            } catch (fallbackError: Exception) {
                logError("Failed to send translated message", fallbackError)
            }
        }
    }
    
    /**
     * Safe method to send prefixed translated messages
     */
    protected fun sendPrefixedTranslatedMessage(sender: CommandSender?, key: String?, vararg args: Any) {
        try {
            if (sender != null && !key.isNullOrBlank()) {
                CloudlyUtils.sendPrefixedTranslatedMessage(sender, key, *args)
            }
        } catch (e: Exception) {
            try {
                // Fallback prefixed message
                val prefix = try { LanguageManager.getPrefix() } catch (e: Exception) { "[Cloudly] " }
                sender?.sendMessage("$prefix$key: ${args.joinToString(", ")}")
            } catch (fallbackError: Exception) {
                logError("Failed to send prefixed translated message", fallbackError)
            }
        }
    }
      /**
     * Safe error logging
     */
    private fun logError(message: String, exception: Exception?) {
        try {
            CloudlyUtils.logColored("Command Error: $message", Level.WARNING)
            exception?.printStackTrace()
        } catch (e: Exception) {
            // Final fallback to system error stream
            System.err.println("Command Error: $message")
            exception?.printStackTrace()
        }
    }
    
    /**
     * Check if a string is safe to use (not null and not blank)
     */
    protected fun isSafeString(value: String?): Boolean {
        return !value.isNullOrBlank()
    }
    
    /**
     * Get safe string or default value
     */
    protected fun getSafeString(value: String?, default: String = ""): String {
        return if (isSafeString(value)) value!! else default
    }
}