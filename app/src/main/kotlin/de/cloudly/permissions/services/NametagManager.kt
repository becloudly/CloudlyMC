package de.cloudly.permissions.services

import de.cloudly.permissions.PermissionManager
import de.cloudly.utils.SchedulerUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Manages nametag formatting for players using scoreboards and teams.
 * Applies prefixes and suffixes above players' heads.
 */
class NametagManager(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager,
    private val formattingService: FormattingService
) {
    
    private val playerTeams = ConcurrentHashMap<String, String>() // UUID -> Team name
    private val teamGroups = ConcurrentHashMap<String, String>() // Team name -> Group name
    private var scoreboard: Scoreboard? = null
    private var initialized = false
    
    /**
     * Initialize the nametag manager.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Wait a bit for server to fully initialize before creating scoreboard
            SchedulerUtils.runTaskLaterAsynchronously(plugin, Runnable {
                try {
                    initializeScoreboard()
                } catch (e: Exception) {
                    plugin.logger.log(Level.SEVERE, "Failed to initialize scoreboard after delay", e)
                }
            }, 20L) // Wait 1 second (20 ticks)
            
            plugin.logger.info("Nametag manager initialization scheduled")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to schedule nametag manager initialization", e)
            false
        }
    }
    
    /**
     * Internal method to initialize the scoreboard.
     */
    private fun initializeScoreboard() {
        try {
            // Get or create a scoreboard for nametag management
            val scoreboardManager = Bukkit.getScoreboardManager()
            if (scoreboardManager == null) {
                plugin.logger.severe("ScoreboardManager is not available")
                return
            }
            
            // Try to create a custom scoreboard first
            scoreboard = try {
                scoreboardManager.newScoreboard
            } catch (e: UnsupportedOperationException) {
                // Some server implementations (like Canvas) don't support custom scoreboards
                plugin.logger.warning("Custom scoreboards not supported, using main scoreboard. Teams will use 'cloudly_' prefix to avoid conflicts.")
                scoreboardManager.mainScoreboard
            }
            
            if (scoreboard == null) {
                plugin.logger.severe("Failed to get any scoreboard (both custom and main failed)")
                return
            }
            
            // Test if team creation is supported before marking as initialized
            val testTeamName = "cloudly_test_${System.currentTimeMillis()}"
            try {
                val testTeam = scoreboard!!.registerNewTeam(testTeamName)
                testTeam.unregister() // Clean up test team
                
                initialized = true
                plugin.logger.info("Nametag manager initialized successfully with ${if (scoreboard == scoreboardManager.mainScoreboard) "main" else "custom"} scoreboard")
                
            } catch (e: UnsupportedOperationException) {
                plugin.logger.warning("Scoreboard team creation not supported on this server - nametag functionality disabled")
                plugin.logger.info("This is normal on Canvas/Folia servers. All other plugin features will work normally.")
                return
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize scoreboard", e)
            throw e
        }
    }
    
    /**
     * Update nametag for a specific player.
     * 
     * @param player The player to update
     */
    fun updatePlayerNametag(player: Player) {
        if (!initialized || !permissionManager.isEnabled() || !formattingService.isNametagFormattingEnabled()) {
            return
        }
        
        SchedulerUtils.runTask(plugin, Runnable {
            try {
                val primaryGroup = permissionManager.getPlayerPrimaryGroup(player) ?: "base"
                val prefix = permissionManager.getPlayerPrefix(player) ?: ""
                val suffix = permissionManager.getPlayerSuffix(player) ?: ""
                
                // Create team name based on group and weight for sorting
                val group = permissionManager.getGroupService().getGroup(primaryGroup)
                val weight = group?.weight ?: 1
                
                // Use cloudly_ prefix when using main scoreboard to avoid conflicts
                val teamPrefix = if (scoreboard == Bukkit.getScoreboardManager()?.mainScoreboard) "cloudly_" else ""
                val teamName = "${teamPrefix}perm_${weight.toString().padStart(3, '0')}_$primaryGroup"
                
                // Remove player from old team if exists
                removePlayerFromTeam(player)
                
                // Create or get team
                val team = getOrCreateTeam(teamName, primaryGroup, prefix, suffix)
                
                // Add player to team
                team.addEntry(player.name)
                playerTeams[player.uniqueId.toString()] = teamName
                
                // Set player's scoreboard
                player.scoreboard = scoreboard!!
                
                plugin.logger.fine("Updated nametag for player ${player.name} (group: $primaryGroup)")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error updating nametag for ${player.name}", e)
            }
        })
    }
    
    /**
     * Update nametags for all online players.
     */
    fun updateAllPlayerNametags() {
        if (!initialized || !permissionManager.isEnabled()) {
            return
        }
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.server.onlinePlayers.forEach { player ->
                    updatePlayerNametag(player)
                }
                
                plugin.logger.fine("Updated nametags for ${plugin.server.onlinePlayers.size} players")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error updating all player nametags", e)
            }
        })
    }
    
    /**
     * Remove a player from their nametag team.
     * 
     * @param player The player to remove
     */
    fun removePlayerFromTeam(player: Player) {
        if (!initialized) return
        
        try {
            val teamName = playerTeams[player.uniqueId.toString()]
            if (teamName != null) {
                val team = scoreboard?.getTeam(teamName)
                team?.removeEntry(player.name)
                playerTeams.remove(player.uniqueId.toString())
                
                plugin.logger.fine("Removed player ${player.name} from nametag team $teamName")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error removing player ${player.name} from team", e)
        }
    }
    
    /**
     * Get or create a team for a group with specific formatting.
     * 
     * @param teamName The name of the team
     * @param groupName The group name
     * @param prefix The prefix for the team
     * @param suffix The suffix for the team
     * @return The team object
     */
    private fun getOrCreateTeam(teamName: String, groupName: String, prefix: String, suffix: String): Team {
        val sb = scoreboard ?: throw IllegalStateException("Scoreboard not initialized")
        
        var team = sb.getTeam(teamName)
        if (team == null) {
            team = sb.registerNewTeam(teamName)
            teamGroups[teamName] = groupName
            plugin.logger.fine("Created new nametag team: $teamName for group $groupName")
        }
        
        // Update team formatting (prefix and suffix)
        try {
            // Bukkit has character limits for team prefix/suffix
            val maxPrefixLength = 16
            val maxSuffixLength = 16
            
            val finalPrefix = if (prefix.length > maxPrefixLength) {
                prefix.substring(0, maxPrefixLength)
            } else prefix
            
            val finalSuffix = if (suffix.length > maxSuffixLength) {
                suffix.substring(0, maxSuffixLength)
            } else suffix
            
            team.prefix = finalPrefix
            team.suffix = finalSuffix
            
            // Set team options
            team.setAllowFriendlyFire(true)
            team.setCanSeeFriendlyInvisibles(false)
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error setting team formatting for $teamName", e)
        }
        
        return team
    }
    
    /**
     * Clear nametag formatting for a player.
     * 
     * @param player The player to clear
     */
    fun clearPlayerNametag(player: Player) {
        if (!initialized) return
        
        SchedulerUtils.runTask(plugin, Runnable {
            try {
                removePlayerFromTeam(player)
                
                // Reset player's scoreboard to main scoreboard
                player.scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard ?: return@Runnable
                
                plugin.logger.fine("Cleared nametag formatting for player ${player.name}")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error clearing nametag for ${player.name}", e)
            }
        })
    }
    
    /**
     * Clear nametag formatting for all players.
     */
    fun clearAllPlayerNametags() {
        if (!initialized) return
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.server.onlinePlayers.forEach { player ->
                    clearPlayerNametag(player)
                }
                
                // Clear all teams from our scoreboard
                scoreboard?.teams?.forEach { team ->
                    team.unregister()
                }
                
                playerTeams.clear()
                teamGroups.clear()
                
                plugin.logger.info("Cleared nametag formatting for all players")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error clearing all player nametags", e)
            }
        })
    }
    
    /**
     * Update teams when group formatting changes.
     * 
     * @param groupName The group that was updated
     */
    fun updateGroupTeams(groupName: String) {
        if (!initialized) return
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                // Find all teams for this group
                val teamsToUpdate = teamGroups.entries
                    .filter { it.value == groupName }
                    .map { it.key }
                
                // Update each team
                teamsToUpdate.forEach { teamName ->
                    val team = scoreboard?.getTeam(teamName)
                    if (team != null) {
                        // Get updated group information
                        val group = permissionManager.getGroupService().getGroup(groupName)
                        if (group != null) {
                            team.prefix = group.prefix ?: ""
                            team.suffix = group.suffix ?: ""
                        }
                    }
                }
                
                if (teamsToUpdate.isNotEmpty()) {
                    plugin.logger.fine("Updated ${teamsToUpdate.size} teams for group $groupName")
                }
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error updating teams for group $groupName", e)
            }
        })
    }
    
    /**
     * Get nametag statistics.
     * 
     * @return Map with nametag statistics
     */
    fun getNametagStats(): Map<String, Any> {
        return mapOf(
            "initialized" to initialized,
            "trackedPlayers" to playerTeams.size,
            "activeTeams" to (scoreboard?.teams?.size ?: 0),
            "groupTeams" to teamGroups.size,
            "onlinePlayers" to plugin.server.onlinePlayers.size
        )
    }
    
    /**
     * Reload the nametag manager.
     */
    fun reload() {
        if (initialized) {
            // Update all player nametags with new configuration
            updateAllPlayerNametags()
            plugin.logger.info("Nametag manager reloaded")
        }
    }
    
    /**
     * Shutdown the nametag manager.
     */
    fun shutdown() {
        if (initialized) {
            // Clear all player nametags
            clearAllPlayerNametags()
            
            scoreboard = null
            initialized = false
            
            plugin.logger.info("Nametag manager shutdown")
        }
    }
}
