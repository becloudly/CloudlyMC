/*
 * Cloudly - Main Command Implementation
 * 
 * This file contains the main command handler for the Cloudly plugin,
 * including subcommands like 'info' and 'reload'.
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Main command handler for the Cloudly plugin
 * Implements subcommands and their functionality
 */
class CloudlyCommand : BaseCommand() {
    
    override val permission = "cloudly.command"
    override val playerOnly = false
    
    override fun execute(sender: CommandSender, command: Command, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            // Show basic plugin info
            CloudlyUtils.sendPrefixedTranslatedMessage(sender, "commands.cloudly.info", 
                CloudlyPlugin.instance.description.version)
            return true
        }
        
        when (args[0].lowercase()) {
            "info" -> {
                showInfo(sender)
                return true
            }
            
            "reload" -> {
                if (sender is Player && !CloudlyUtils.hasPermission(sender, "cloudly.admin")) {
                    CloudlyUtils.sendPrefixedTranslatedMessage(sender, "commands.cloudly.reload-no-permission")
                    return true
                }
                
                // Reload the plugin
                CloudlyPlugin.instance.reloadPlugin()
                return true
            }
            
            else -> {
                sendUsage(sender, "/cloudly [info|reload]")
                return true
            }
        }
    }
    
    /**
     * Display detailed plugin information
     */
    private fun showInfo(sender: CommandSender) {
        // Get plugin instance
        val plugin = CloudlyPlugin.instance
        
        // Get version
        val version = plugin.description.version
        
        // Get server info
        val serverName = Bukkit.getServer().name
        val serverVersion = Bukkit.getServer().version
        
        // Get language
        val currentLanguage = LanguageManager.getCurrentLanguage()
        
        // Get memory usage
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemoryMB = runtime.totalMemory() / (1024 * 1024)
        
        // Get uptime
        val uptime = formatUptime()
        
        // Get player count
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()
        
        // Send the formatted information
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.header")
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.version", version)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.server", serverName, serverVersion)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.author")
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.language", currentLanguage)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.memory", usedMemoryMB, totalMemoryMB)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.uptime", uptime)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.players", onlinePlayers, maxPlayers)
        CloudlyUtils.sendTranslatedMessage(sender, "commands.cloudly.info-command.footer")
    }
    
    /**
     * Format the JVM uptime in a human-readable format
     */
    private fun formatUptime(): String {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        
        val days = TimeUnit.MILLISECONDS.toDays(uptime)
        val hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    override fun complete(sender: CommandSender, command: Command, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("info")
                if (sender !is Player || CloudlyUtils.hasPermission(sender, "cloudly.admin")) {
                    suggestions.add("reload")
                }
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}
