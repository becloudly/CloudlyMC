package de.cloudly.permissions.listeners

import de.cloudly.permissions.PermissionManager
import de.cloudly.permissions.services.FormattingService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Handles chat formatting events.
 * Applies prefixes, suffixes, and custom chat formats from permission groups.
 */
class ChatFormattingListener(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager,
    private val formattingService: FormattingService
) : Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (!permissionManager.isEnabled() || !formattingService.isChatFormattingEnabled()) {
            return
        }
        
        try {
            val player = event.player
            val originalMessage = event.message
            
            // Get formatted message from formatting service
            val formattedMessage = formattingService.formatChatMessage(player, originalMessage)
            
            if (formattedMessage != null) {
                // Cancel the event and send the formatted message manually
                event.isCancelled = true
                
                // Send the formatted message to all recipients
                event.recipients.forEach { recipient ->
                    recipient.sendMessage(formattedMessage)
                }
                
                // Also send to console
                plugin.server.consoleSender.sendMessage(formattedMessage)
                
                plugin.logger.fine("Applied chat formatting for player ${player.name}")
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error in chat formatting listener", e)
        }
    }
}
