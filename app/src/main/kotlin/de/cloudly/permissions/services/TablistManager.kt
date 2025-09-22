package de.cloudly.permissions.services

import de.cloudly.permissions.PermissionManager
import de.cloudly.utils.SchedulerUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Manages tablist formatting for players.
 * Updates player tablist entries with prefixes and suffixes.
 */
class TablistManager(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager,
    private val formattingService: FormattingService
) {
    
    private val playerTablistData = ConcurrentHashMap<String, TablistData>()
    private var initialized = false
    private var updateTaskId: BukkitTask? = null
    
    /**
     * Initialize the tablist manager.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            initialized = true
            
            // Start periodic tablist update task (every 30 seconds)
            startUpdateTask()
            
            plugin.logger.info("Tablist manager initialized successfully")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize tablist manager", e)
            false
        }
    }
    
    /**
     * Update tablist for a specific player.
     * 
     * @param player The player to update
     */
    fun updatePlayerTablist(player: Player) {
        if (!initialized || !permissionManager.isEnabled() || !formattingService.isTablistFormattingEnabled()) {
            return
        }
        
        try {
            val formattedEntry = formattingService.formatTablistEntry(player)
            
            if (formattedEntry != null) {
                // Store current tablist data
                val tablistData = TablistData(
                    playerName = formattedEntry,
                    lastUpdated = System.currentTimeMillis()
                )
                
                playerTablistData[player.uniqueId.toString()] = tablistData
                
                // Update the player's tablist entry on the main thread
                SchedulerUtils.runTask(plugin, Runnable {
                    try {
                        player.setPlayerListName(formattedEntry)
                        plugin.logger.fine("Updated tablist for player ${player.name}")
                    } catch (e: Exception) {
                        plugin.logger.log(Level.WARNING, "Error updating tablist for ${player.name}", e)
                    }
                })
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error updating tablist for player ${player.name}", e)
        }
    }
    
    /**
     * Update tablist for all online players.
     */
    fun updateAllPlayerTablists() {
        if (!initialized || !permissionManager.isEnabled()) {
            return
        }
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.server.onlinePlayers.forEach { player ->
                    updatePlayerTablist(player)
                }
                
                plugin.logger.fine("Updated tablist for ${plugin.server.onlinePlayers.size} players")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error updating all player tablists", e)
            }
        })
    }
    
    /**
     * Remove a player from tablist tracking.
     * 
     * @param player The player to remove
     */
    fun removePlayer(player: Player) {
        if (!initialized) return
        
        playerTablistData.remove(player.uniqueId.toString())
    }
    
    /**
     * Clear tablist formatting for a player (reset to default).
     * 
     * @param player The player to clear
     */
    fun clearPlayerTablist(player: Player) {
        if (!initialized) return
        
        try {
            SchedulerUtils.runTask(plugin, Runnable {
                try {
                    player.setPlayerListName(null) // Reset to default
                    plugin.logger.fine("Cleared tablist formatting for player ${player.name}")
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Error clearing tablist for ${player.name}", e)
                }
            })
            
            playerTablistData.remove(player.uniqueId.toString())
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error clearing tablist for player ${player.name}", e)
        }
    }
    
    /**
     * Clear tablist formatting for all players.
     */
    fun clearAllPlayerTablists() {
        if (!initialized) return
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.server.onlinePlayers.forEach { player ->
                    clearPlayerTablist(player)
                }
                
                playerTablistData.clear()
                plugin.logger.info("Cleared tablist formatting for all players")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error clearing all player tablists", e)
            }
        })
    }
    
    /**
     * Start the periodic update task.
     */
    private fun startUpdateTask() {
        if (updateTaskId != null) {
            return // Task already running
        }
        
        // Update tablists every 30 seconds
        updateTaskId = SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
            try {
                // Only update if there are players online
                if (plugin.server.onlinePlayers.isNotEmpty()) {
                    updateAllPlayerTablists()
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error in tablist update task", e)
            }
        }, 600L, 600L) // 600 ticks = 30 seconds
    }
    
    /**
     * Stop the periodic update task.
     */
    private fun stopUpdateTask() {
        updateTaskId?.let { task ->
            task.cancel()
            updateTaskId = null
        }
    }
    
    /**
     * Get tablist statistics.
     * 
     * @return Map with tablist statistics
     */
    fun getTablistStats(): Map<String, Any> {
        return mapOf(
            "initialized" to initialized,
            "trackedPlayers" to playerTablistData.size,
            "updateTaskRunning" to (updateTaskId != null),
            "onlinePlayers" to plugin.server.onlinePlayers.size
        )
    }
    
    /**
     * Reload the tablist manager.
     */
    fun reload() {
        if (initialized) {
            // Update all player tablists with new configuration
            updateAllPlayerTablists()
            plugin.logger.info("Tablist manager reloaded")
        }
    }
    
    /**
     * Shutdown the tablist manager.
     */
    fun shutdown() {
        if (initialized) {
            // Stop update task
            stopUpdateTask()
            
            // Clear all player tablists
            clearAllPlayerTablists()
            
            playerTablistData.clear()
            initialized = false
            
            plugin.logger.info("Tablist manager shutdown")
        }
    }
    
    /**
     * Data class for storing tablist information.
     */
    private data class TablistData(
        val playerName: String,
        val lastUpdated: Long
    )
}
