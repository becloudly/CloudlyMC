/*
 * Cloudly - Main Command Implementation
 * 
 * This file contains the main command handler for the Cloudly plugin,
 * including subcommands like 'info' and 'reload'.
 * All methods are null-safe and handle errors gracefully.
 */
package cloudly.command

import cloudly.CloudlyPlugin
import cloudly.util.CloudlyUtils
import cloudly.util.LanguageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Main command handler for the Cloudly plugin
 * Implements subcommands and their functionality
 * All operations are null-safe and error-resistant
 */
class CloudlyCommand : BaseCommand() {
    
    override val permission: String? = "cloudly.command"
    override val playerOnly = false
    
    override fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean {
        return try {
            when {
                args.isEmpty() -> {
                    showBasicInfo(sender)
                    true
                }
                else -> {
                    handleSubcommand(sender, args)
                }
            }
        } catch (e: Exception) {
            handleCommandError(sender, "execute", e)
            true
        }
    }
    
    /**
     * Show basic plugin information
     */
    private fun showBasicInfo(sender: CommandSender) {
        try {
            val version = getPluginVersionSafely()
            sendPrefixedTranslatedMessage(sender, "commands.cloudly.info", version)
        } catch (e: Exception) {
            handleCommandError(sender, "showBasicInfo", e)
        }
    }
    
    /**
     * Handle subcommands safely
     */
    private fun handleSubcommand(sender: CommandSender, args: Array<String>): Boolean {
        return try {
            val subcommand = getSafeString(args.getOrNull(0)?.lowercase())
            
            when (subcommand) {
                "info" -> {
                    showDetailedInfo(sender)
                    true
                }
                
                "reload" -> {
                    handleReloadCommand(sender)
                    true
                }
                
                else -> {
                    sendUsage(sender, "/cloudly [info|reload]")
                    true
                }
            }
        } catch (e: Exception) {
            handleCommandError(sender, "handleSubcommand", e)
            true
        }
    }
    
    /**
     * Handle reload command with permission checking
     */
    private fun handleReloadCommand(sender: CommandSender) {
        try {
            if (sender is Player && !hasAdminPermission(sender)) {
                sendPrefixedTranslatedMessage(sender, "commands.cloudly.reload-no-permission")
                return
            }
            
            // Perform reload
            reloadPluginSafely(sender)
            
        } catch (e: Exception) {
            handleCommandError(sender, "handleReloadCommand", e)
        }
    }
    
    /**
     * Check if player has admin permission safely
     */
    private fun hasAdminPermission(player: Player?): Boolean {
        return try {
            player != null && CloudlyUtils.hasPermission(player, "cloudly.admin")
        } catch (e: Exception) {
            false // Deny on error for security
        }
    }
    
    /**
     * Reload plugin safely
     */
    private fun reloadPluginSafely(sender: CommandSender) {
        try {
            val plugin = getPluginInstanceSafely()
            if (plugin != null) {
                plugin.reloadPlugin()
            } else {
                sendPrefixedTranslatedMessage(sender, "common.error-occurred")
            }
        } catch (e: Exception) {
            handleCommandError(sender, "reloadPluginSafely", e)
        }
    }
    
    /**
     * Display detailed plugin information safely
     */
    private fun showDetailedInfo(sender: CommandSender) {
        try {
            // Gather information safely
            val infoData = gatherPluginInfo()
            
            // Send formatted information
            sendInfoMessages(sender, infoData)
            
        } catch (e: Exception) {
            handleCommandError(sender, "showDetailedInfo", e)
            // Fallback: show basic info
            showBasicInfo(sender)
        }
    }
      /**
     * Gather plugin information safely
     */
    private fun gatherPluginInfo(): PluginInfo {
        return try {
            PluginInfo(
                version = getPluginVersionSafely(),
                serverName = getServerNameSafely(),
                serverVersion = getServerVersionSafely(),
                language = getCurrentLanguageSafely(),
                memoryInfo = getMemoryInfoSafely(),
                uptime = getUptimeSafely(),
                playerInfo = getPlayerInfoSafely(),
                javaVersion = getJavaVersionSafely(),
                operatingSystem = getOperatingSystemSafely(),
                performanceRating = getPerformanceRatingSafely()
            )
        } catch (e: Exception) {
            // Fallback with safe defaults
            PluginInfo()
        }
    }
      /**
     * Send info messages to sender
     */
    private fun sendInfoMessages(sender: CommandSender, info: PluginInfo) {
        try {
            sendTranslatedMessage(sender, "commands.cloudly.info-command.header")
            sendTranslatedMessage(sender, "commands.cloudly.info-command.version", info.version)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.server", info.serverName, info.serverVersion)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.language", info.language)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.memory", info.memoryInfo.first, info.memoryInfo.second)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.uptime", info.uptime)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.players", info.playerInfo.first, info.playerInfo.second)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.java-version", info.javaVersion)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.os", info.operatingSystem)
            sendTranslatedMessage(sender, "commands.cloudly.info-command.performance", info.performanceRating)
        } catch (e: Exception) {
            handleCommandError(sender, "sendInfoMessages", e)
        }
    }
    
    /**
     * Get plugin instance safely
     */
    private fun getPluginInstanceSafely(): CloudlyPlugin? {
        return try {
            CloudlyPlugin.getInstanceSafely()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get plugin version safely
     */
    private fun getPluginVersionSafely(): String {
        return try {
            getPluginInstanceSafely()?.description?.version ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get server name safely
     */
    private fun getServerNameSafely(): String {
        return try {
            Bukkit.getServer()?.name ?: "Unknown Server"
        } catch (e: Exception) {
            "Unknown Server"
        }
    }
    
    /**
     * Get server version safely
     */
    private fun getServerVersionSafely(): String {
        return try {
            Bukkit.getServer()?.version ?: "Unknown Version"
        } catch (e: Exception) {
            "Unknown Version"
        }
    }
    
    /**
     * Get current language safely
     */
    private fun getCurrentLanguageSafely(): String {
        return try {
            LanguageManager.getCurrentLanguage()
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get memory information safely
     * Returns Pair<UsedMB, TotalMB>
     */
    private fun getMemoryInfoSafely(): Pair<Long, Long> {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val totalMemoryMB = runtime.totalMemory() / (1024 * 1024)
            Pair(usedMemoryMB, totalMemoryMB)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    /**
     * Get uptime safely
     */
    private fun getUptimeSafely(): String {
        return try {
            formatUptimeSafely()
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get player information safely
     * Returns Pair<OnlinePlayers, MaxPlayers>
     */
    private fun getPlayerInfoSafely(): Pair<Int, Int> {
        return try {
            val onlinePlayers = Bukkit.getOnlinePlayers()?.size ?: 0
            val maxPlayers = Bukkit.getMaxPlayers()
            Pair(onlinePlayers, maxPlayers)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
      /**
     * Format the JVM uptime in a human-readable format - null-safe
     */
    private fun formatUptimeSafely(): String {
        return try {
            val uptime = ManagementFactory.getRuntimeMXBean()?.uptime ?: 0L
            
            val days = TimeUnit.MILLISECONDS.toDays(uptime)
            val hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60
            
            when {
                days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get Java version safely
     */
    private fun getJavaVersionSafely(): String {
        return try {
            System.getProperty("java.version") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get operating system information safely
     */
    private fun getOperatingSystemSafely(): String {
        return try {
            val osName = System.getProperty("os.name") ?: "Unknown"
            val osArch = System.getProperty("os.arch") ?: "Unknown"
            "$osName ($osArch)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get performance rating based on memory usage and TPS
     */
    private fun getPerformanceRatingSafely(): String {
        return try {
            val memoryInfo = getMemoryInfoSafely()
            val memoryUsagePercentage = if (memoryInfo.second > 0) {
                (memoryInfo.first.toDouble() / memoryInfo.second.toDouble()) * 100
            } else 0.0

            when {
                memoryUsagePercentage < 50 -> "Excellent"
                memoryUsagePercentage < 70 -> "Good"
                memoryUsagePercentage < 85 -> "Fair"
                else -> "Poor"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    override fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        return try {
            when (args.size) {
                1 -> {
                    val suggestions = mutableListOf<String>()
                    
                    // Always add info
                    suggestions.add("info")
                    
                    // Add reload if sender has permission
                    if (hasReloadPermission(sender)) {
                        suggestions.add("reload")
                    }
                    
                    // Filter suggestions based on input
                    val input = getSafeString(args.getOrNull(0))
                    suggestions.filter { it.startsWith(input, ignoreCase = true) }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if sender has reload permission safely
     */
    private fun hasReloadPermission(sender: CommandSender?): Boolean {
        return try {
            when (sender) {
                is Player -> hasAdminPermission(sender)
                else -> true // Console has permission
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Handle command errors safely
     */
    private fun handleCommandError(sender: CommandSender?, context: String, e: Exception) {
        try {
            logError("Error in $context", e)
            sendPrefixedTranslatedMessage(sender, "common.error-occurred")
        } catch (errorHandlingException: Exception) {
            // Final fallback
            try {
                sender?.sendMessage("An error occurred while executing the command.")
            } catch (finalError: Exception) {
                // Log to console as last resort
                System.err.println("Critical error in command handling: ${finalError.message}")
            }
        }
    }
      /**
     * Safe error logging
     */
    private fun logError(message: String, exception: Exception) {
        try {
            CloudlyUtils.logColored("CloudlyCommand Error: $message", Level.WARNING)
            exception.printStackTrace()
        } catch (e: Exception) {
            System.err.println("CloudlyCommand Error: $message")
            exception.printStackTrace()
        }
    }
      /**
     * Data class to hold plugin information
     */
    private data class PluginInfo(
        val version: String = "Unknown",
        val serverName: String = "Unknown Server",
        val serverVersion: String = "Unknown Version",
        val language: String = "Unknown",
        val memoryInfo: Pair<Long, Long> = Pair(0L, 0L),
        val uptime: String = "Unknown",
        val playerInfo: Pair<Int, Int> = Pair(0, 0),
        val javaVersion: String = "Unknown",
        val operatingSystem: String = "Unknown",
        val performanceRating: String = "Unknown"
    )
}