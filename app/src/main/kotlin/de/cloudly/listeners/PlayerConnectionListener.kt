package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

/**
 * Handles player connection events (join/leave/login) to provide custom messages and queue management.
 * This replaces the default Minecraft join/leave messages with configurable custom ones and
 * integrates with the connection queue system.
 */
class PlayerConnectionListener(private val plugin: CloudlyPaper) : Listener {
    
    /**
     * Handle player login events for queue system.
     * This runs before the player actually joins and can prevent them from joining.
     */
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        try {
            // Check if queue is enabled and add player to queue if needed
            val queueService = plugin.getQueueService()
            if (queueService.isEnabled()) {
                val addedToQueue = queueService.addToQueue(event)
                if (addedToQueue) {
                    plugin.logger.info("Player ${event.player.name} added to connection queue")
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to handle player login event for queue", e)
        }
    }
    
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
            
            // Send custom join message if enabled
            val customJoinEnabled = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.enabled", true)
            if (customJoinEnabled) {
                val player = event.player
                val broadcastToChat = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.broadcast_to_chat", true)
                val broadcastToConsole = plugin.getConfigManager().getBoolean("player_connection.custom_messages.join.broadcast_to_console", true)
                
                // Get messages from Messages object
                val chatMessage = Messages.PlayerConnection.Join.chat(player.name)
                val consoleMessage = Messages.PlayerConnection.Join.console(player.name)
                
                // Broadcast to chat
                if (broadcastToChat) {
                    Bukkit.broadcastMessage(chatMessage)
                }
                
                // Broadcast to console
                if (broadcastToConsole) {
                    plugin.logger.info(consoleMessage)
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
            
            // Send custom leave message if enabled
            val customLeaveEnabled = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.enabled", true)
            if (customLeaveEnabled) {
                val player = event.player
                val broadcastToChat = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.broadcast_to_chat", true)
                val broadcastToConsole = plugin.getConfigManager().getBoolean("player_connection.custom_messages.leave.broadcast_to_console", true)
                
                // Get messages from Messages object
                val chatMessage = Messages.PlayerConnection.Leave.chat(player.name)
                val consoleMessage = Messages.PlayerConnection.Leave.console(player.name)
                
                // Broadcast to chat
                if (broadcastToChat) {
                    Bukkit.broadcastMessage(chatMessage)
                }
                
                // Broadcast to console
                if (broadcastToConsole) {
                    plugin.logger.info(consoleMessage)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to handle player quit event", e)
        }
    }
}
