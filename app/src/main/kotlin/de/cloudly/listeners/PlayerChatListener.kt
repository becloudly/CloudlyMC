package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

/**
 * Handles player chat events to provide custom message formatting.
 * Formats messages as: Gray "[HH:MM:SS] | " + White "Name: " + Gray "Message"
 */
class PlayerChatListener(private val plugin: CloudlyPaper) : Listener {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    /**
     * Handle player chat events.
     * Changes the format from default "<Name> Message" (all white) 
     * to "[HH:MM:SS] | " (gray) + "Name: " (white) + "Message" (gray)
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncChatEvent) {
        try {
            val player = event.player
            
            // Get current time formatted as [HH:MM:SS]
            val currentTime = LocalTime.now().format(timeFormatter)
            
            // Create the formatted message
            // Gray "[HH:MM:SS] | " + White "PlayerName: " + Gray message content
            val formattedMessage = Component.text()
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(currentTime, NamedTextColor.GRAY))
                .append(Component.text("] | ", NamedTextColor.GRAY))
                .append(Component.text(player.name, NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(Component.text().color(NamedTextColor.GRAY).append(event.message()))
                .build()
            
            // Set the custom renderer to display our formatted message
            event.renderer { _, _, message, _ ->
                formattedMessage
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to format chat message", e)
        }
    }
}
