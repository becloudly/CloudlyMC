package de.cloudly.permissions.listeners

import de.cloudly.permissions.PermissionManager
import de.cloudly.utils.SchedulerUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Handles player join/quit events for the permission system.
 * Sets up permissions when players join and cleans up when they leave.
 */
class PlayerPermissionListener(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager
) : Listener {
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        if (!permissionManager.isEnabled()) {
            return
        }
        
        try {
            // Handle permission setup asynchronously to avoid blocking the main thread
            SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
                try {
                    // Ensure user exists in the permission system
                    permissionManager.getUserService().ensureUserExists(player.uniqueId, player.name)
                    
                    // Set up Bukkit permission attachments
                    setupPlayerPermissions(player)
                    
                    // Notify permission manager of player join
                    permissionManager.onPlayerJoin(player)
                    
                    plugin.logger.fine("Permission setup completed for player: ${player.name}")
                    
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Error setting up permissions for player ${player.name}", e)
                }
            })
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error during player join permission handling for ${player.name}", e)
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        if (!permissionManager.isEnabled()) {
            return
        }
        
        try {
            // Clean up player permissions
            cleanupPlayerPermissions(player)
            
            // Notify permission manager of player quit
            permissionManager.onPlayerQuit(player)
            
            plugin.logger.fine("Permission cleanup completed for player: ${player.name}")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error during player quit permission handling for ${player.name}", e)
        }
    }
    
    /**
     * Set up Bukkit permission attachments for a player.
     * This applies all effective permissions to the player's Bukkit permission system.
     */
    private fun setupPlayerPermissions(player: org.bukkit.entity.Player) {
        try {
            // Get all effective permissions for this player
            val effectivePermissions = permissionManager.getPlayerPermissions(player)
            
            // Remove any existing permission attachments from our plugin
            player.effectivePermissions
                .filter { it.attachment?.plugin == plugin }
                .forEach { permInfo ->
                    permInfo.attachment?.remove()
                }
            
            // Create new permission attachment
            val attachment = player.addAttachment(plugin)
            
            // Apply all effective permissions
            effectivePermissions.forEach { permission ->
                when {
                    permission.startsWith("-") -> {
                        // Negative permission (explicit denial)
                        val actualPermission = permission.substring(1)
                        attachment.setPermission(actualPermission, false)
                    }
                    permission == "*" -> {
                        // Wildcard permission - grant all
                        attachment.setPermission("*", true)
                    }
                    else -> {
                        // Regular permission
                        attachment.setPermission(permission, true)
                    }
                }
            }
            
            // Recalculate player's permissions
            player.recalculatePermissions()
            
            plugin.logger.fine("Applied ${effectivePermissions.size} permissions to player ${player.name}")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error setting up Bukkit permissions for player ${player.name}", e)
        }
    }
    
    /**
     * Clean up Bukkit permission attachments for a player.
     */
    private fun cleanupPlayerPermissions(player: org.bukkit.entity.Player) {
        try {
            // Remove any permission attachments from our plugin
            player.effectivePermissions
                .filter { it.attachment?.plugin == plugin }
                .forEach { permInfo ->
                    permInfo.attachment?.remove()
                }
            
            // Recalculate player's permissions
            player.recalculatePermissions()
            
            plugin.logger.fine("Cleaned up permissions for player ${player.name}")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error cleaning up permissions for player ${player.name}", e)
        }
    }
    
    /**
     * Refresh permissions for a specific player.
     * This should be called when a player's permissions change.
     */
    fun refreshPlayerPermissions(player: org.bukkit.entity.Player) {
        if (!permissionManager.isEnabled()) {
            return
        }
        
        try {
            // Clear permission cache for this player
            permissionManager.clearPlayerCache(player.uniqueId)
            
            // Reapply permissions
            setupPlayerPermissions(player)
            
            plugin.logger.fine("Refreshed permissions for player ${player.name}")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error refreshing permissions for player ${player.name}", e)
        }
    }
    
    /**
     * Refresh permissions for all online players.
     * This should be called when the permission system is reloaded.
     */
    fun refreshAllPlayerPermissions() {
        if (!permissionManager.isEnabled()) {
            return
        }
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.server.onlinePlayers.forEach { player ->
                    refreshPlayerPermissions(player)
                }
                
                plugin.logger.info("Refreshed permissions for ${plugin.server.onlinePlayers.size} online players")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error refreshing permissions for all players", e)
            }
        })
    }
}
