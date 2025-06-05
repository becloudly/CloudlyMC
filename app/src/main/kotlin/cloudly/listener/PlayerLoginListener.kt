/*
 * Cloudly - Player Login Listener
 * 
 * Handles player login events to check whitelist status and manage application system.
 * Integrates with the whitelist application system to freeze pending players.
 * All operations are async and null-safe with proper error handling.
 */
package cloudly.listener

import cloudly.CloudlyPlugin
import cloudly.util.ApplicationManager
import cloudly.util.LanguageManager
import cloudly.util.PlayerFreezeManager
import cloudly.util.WhitelistManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
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
                // Check application status
                val applicationStatus = runBlocking {
                    try {
                        ApplicationManager.getApplicationStatus(player.uniqueId)
                    } catch (e: Exception) {
                        CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error checking application status for player ${player.name}", e)
                        null
                    }
                }
                
                when (applicationStatus) {
                    ApplicationManager.ApplicationStatus.PENDING -> {
                        // Allow login but will be frozen
                        // The freezing will happen in PlayerJoinEvent
                        return
                    }
                    
                    ApplicationManager.ApplicationStatus.DENIED -> {
                        // Kick with denial message
                        val kickMessage = LanguageManager.getMessage("commands.whitelist.kick-denied")
                        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage)
                        return
                    }
                    
                    ApplicationManager.ApplicationStatus.APPROVED -> {
                        // This shouldn't happen since approved applications should be whitelisted
                        // But just in case, allow login
                        CloudlyPlugin.instance.logger.warning("Player ${player.name} has approved application but is not whitelisted!")
                        return
                    }
                    
                    null -> {
                        // No application exists, kick with application instructions
                        val kickMessage = LanguageManager.getMessage("commands.whitelist.kick-apply")
                        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage)
                        return
                    }
                }
            }
            
        } catch (e: Exception) {
            CloudlyPlugin.instance.logger.log(Level.SEVERE, "Critical error in PlayerLoginListener", e)
            // In case of critical error, allow the join to prevent blocking all players
        }
    }
    
    /**
     * Handle player join event to freeze players with pending applications
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Skip if plugin not initialized or whitelist disabled
        if (!CloudlyPlugin.instance.isPluginInitialized() || !WhitelistManager.isWhitelistEnabled()) {
            return
        }
        
        // Check if player should be frozen (has pending application)
        CloudlyPlugin.instance.getPluginScope().launch {
            try {
                val isWhitelisted = WhitelistManager.isPlayerWhitelisted(player.uniqueId)
                
                if (!isWhitelisted) {
                    val applicationStatus = ApplicationManager.getApplicationStatus(player.uniqueId)
                    
                    if (applicationStatus == ApplicationManager.ApplicationStatus.PENDING) {
                        // Schedule freeze for next tick to ensure player is fully loaded
                        Bukkit.getScheduler().runTask(CloudlyPlugin.instance, Runnable {
                            if (player.isOnline) {
                                PlayerFreezeManager.freezePlayer(player, "Pending whitelist application")
                            }
                        })
                    }
                }
                
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.log(Level.SEVERE, "Error checking player freeze status for ${player.name}", e)
            }
        }
    }
}
