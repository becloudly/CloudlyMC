package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

/**
 * Handles player connection events (join/leave) to provide custom messages.
 * This replaces the default Minecraft join/leave messages with configurable custom ones.
 */
class PlayerConnectionListener(private val plugin: CloudlyPaper) : Listener {
    
    /**
     * Handle player join events.
     * Suppresses the default join message and sends custom messages if configured.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        try {
            val removeDefaultMessages = plugin.getConfigManager().getBoolean("player_connection.remove_default_messages", true)
            
            // Always suppress the default message if configured
            if (removeDefaultMessages) {
                event.joinMessage = null
            }
            
            // Notify permission system of player join (if enabled)
            try {
                if (plugin.getPermissionManager().isEnabled()) {
                    plugin.getPermissionManager().onPlayerJoin(event.player)
                }
            } catch (e: Exception) {
                // Permission system might not be initialized yet, ignore
            }
            
            // Send custom join message if enabled
            val customJoinEnabled = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.enabled", true)
            if (customJoinEnabled) {
                val player = event.player
                val broadcastToChat = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.broadcast_to_chat", true)
                val broadcastToConsole = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.broadcast_to_console", true)
                
                // Get messages from language files
                val chatMessage = plugin.getLanguageManager().getMessage("player_connection.join.chat")
                val consoleMessage = plugin.getLanguageManager().getMessage("player_connection.join.console")
                
                // Replace placeholders
                val replacedChatMessage = chatMessage
                    .replace("{player_name}", player.name)
                    .replace("{player_display_name}", player.displayName)
                    .replace("{server_name}", Bukkit.getServer().name)
                
                val replacedConsoleMessage = consoleMessage
                    .replace("{player_name}", player.name)
                    .replace("{player_display_name}", player.displayName)
                    .replace("{server_name}", Bukkit.getServer().name)
                
                // Broadcast to chat
                if (broadcastToChat) {
                    Bukkit.broadcastMessage(replacedChatMessage)
                }
                
                // Broadcast to console
                if (broadcastToConsole) {
                    plugin.logger.info(replacedConsoleMessage)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to handle player join event", e)
        }
    }
    
    /**
     * Handle player quit events.
     * Suppresses the default quit message and sends custom messages if configured.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        try {
            val removeDefaultMessages = plugin.getConfigManager().getBoolean("player_connection.remove_default_messages", true)
            
            // Always suppress the default message if configured
            if (removeDefaultMessages) {
                event.quitMessage = null
            }
            
            // Notify permission system of player quit (if enabled)
            try {
                if (plugin.getPermissionManager().isEnabled()) {
                    plugin.getPermissionManager().onPlayerQuit(event.player)
                }
            } catch (e: Exception) {
                // Permission system might not be initialized yet, ignore
            }
            
            // Send custom leave message if enabled
            val customLeaveEnabled = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.enabled", true)
            if (customLeaveEnabled) {
                val player = event.player
                val broadcastToChat = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.broadcast_to_chat", true)
                val broadcastToConsole = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.broadcast_to_console", true)
                
                // Get messages from language files
                val chatMessage = plugin.getLanguageManager().getMessage("player_connection.leave.chat")
                val consoleMessage = plugin.getLanguageManager().getMessage("player_connection.leave.console")
                
                // Replace placeholders
                val replacedChatMessage = chatMessage
                    .replace("{player_name}", player.name)
                    .replace("{player_display_name}", player.displayName)
                    .replace("{server_name}", Bukkit.getServer().name)
                
                val replacedConsoleMessage = consoleMessage
                    .replace("{player_name}", player.name)
                    .replace("{player_display_name}", player.displayName)
                    .replace("{server_name}", Bukkit.getServer().name)
                
                // Broadcast to chat
                if (broadcastToChat) {
                    Bukkit.broadcastMessage(replacedChatMessage)
                }
                
                // Broadcast to console
                if (broadcastToConsole) {
                    plugin.logger.info(replacedConsoleMessage)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to handle player quit event", e)
        }
    }
}
