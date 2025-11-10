package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Level

/**
 * Handles player chat events to provide custom message formatting.
 */
class PlayerChatListener(private val plugin: CloudlyPaper) : Listener {
    
    
    /**
     * Handle player chat events.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncChatEvent) {
        try {
            val player = event.player
            
            // Create the formatted message
            val formattedMessage = Component.text()
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
