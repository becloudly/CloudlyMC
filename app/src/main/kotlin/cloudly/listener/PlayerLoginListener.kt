/*
 * Cloudly - Player Login Listener
 * 
 * Handles player login events to check whitelist status and prevent non-whitelisted
 * players from joining when the whitelist system is enabled.
 * All operations are async and null-safe with proper error handling.
 */
package cloudly.listener

import cloudly.CloudlyPlugin
import cloudly.util.LanguageManager
import cloudly.util.WhitelistManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import java.util.logging.Level

/**
 * PlayerLoginListener handles whitelist checking on player login
 * Integrates with the custom whitelist system to override Minecraft's default behavior
 */
class PlayerLoginListener : Listener {
    
    /**
     * Handle player login event with highest priority to ensure whitelist is checked first
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        // Skip if event is already cancelled or plugin not initialized
        if (event.result != PlayerLoginEvent.Result.ALLOWED) {
            return
        }
        
        if (!CloudlyPlugin.instance.isPluginInitialized()) {
            return
        }
        
        try {
            // Only check whitelist if it's enabled
            if (!WhitelistManager.isWhitelistEnabled()) {
                return
            }
            
            // Check if player is whitelisted (this is a suspend function, so we need to handle it carefully)
            val player = event.player
            val isWhitelisted = runBlocking {
                try {
                    WhitelistManager.isPlayerWhitelisted(player.uniqueId)
                } catch (e: Exception) {
                    CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error checking whitelist for player ${player.name}", e)
                    // In case of error, allow join to prevent blocking legitimate players
                    true
                }
            }
            
            if (!isWhitelisted) {
                // Get kick message from language system
                val kickMessage = try {
                    LanguageManager.getMessage("commands.whitelist.kick-message")
                } catch (e: Exception) {
                    CloudlyPlugin.instance.logger.log(Level.WARNING, "Error getting kick message", e)
                    // Fallback message
                    "§cYou are not whitelisted on this server!"
                }
                
                // Deny login
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage)
                
                // Log the attempt (async to not block login process)
                CloudlyPlugin.instance.getPluginScope().launch {
                    try {
                        CloudlyPlugin.instance.logger.info("Denied login for non-whitelisted player: ${player.name} (${player.uniqueId})")
                    } catch (e: Exception) {
                        // Ignore logging errors
                    }
                }
            }
            
        } catch (e: Exception) {
            CloudlyPlugin.instance.logger.log(Level.SEVERE, "Critical error in PlayerLoginListener", e)
            // In case of critical error, allow the join to prevent blocking all players
        }
    }
}
