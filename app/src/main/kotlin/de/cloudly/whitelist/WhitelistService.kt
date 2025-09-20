package de.cloudly.whitelist

import de.cloudly.whitelist.config.WhitelistConfig
import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import de.cloudly.whitelist.storage.WhitelistStorage
import de.cloudly.whitelist.storage.WhitelistStorageFactory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.logging.Level

/**
 * Service for managing the custom whitelist system.
 * Handles player whitelist operations and login events.
 */
class WhitelistService(private val plugin: JavaPlugin) : Listener {
    
    private val config = WhitelistConfig(plugin)
    private var storage: WhitelistStorage? = null
    private var enabled = false
    
    /**
     * Initialize the whitelist service.
     * Loads configuration and sets up the storage implementation.
     */
    fun initialize() {
        // Load configuration
        config.load()
        enabled = config.enabled
        
        if (!enabled) {
            plugin.logger.info("Custom whitelist is disabled in config")
            return
        }
        
        // Create and initialize storage
        val storageFactory = WhitelistStorageFactory(plugin)
        storage = storageFactory.createStorage(config)
        
        if (storage?.initialize() == true) {
            plugin.logger.info("Whitelist storage initialized successfully")
            
            // Register event listener
            plugin.server.pluginManager.registerEvents(this, plugin)
        } else {
            plugin.logger.severe("Failed to initialize whitelist storage")
            enabled = false
        }
    }
    
    /**
     * Add a player to the whitelist.
     * @param player The player to add
     * @param addedBy The UUID of the player or admin who added this player (optional)
     * @param reason The reason for whitelisting (optional)
     * @return true if the player was added successfully, false otherwise
     */
    fun addPlayer(player: Player, addedBy: UUID? = null, reason: String? = null): Boolean {
        if (!enabled || storage == null) return false
        
        val whitelistPlayer = WhitelistPlayer(
            uuid = player.uniqueId,
            username = player.name,
            addedBy = addedBy,
            reason = reason
        )
        
        return storage?.addPlayer(whitelistPlayer) ?: false
    }
    
    /**
     * Add a player to the whitelist by UUID and username.
     * @param uuid The UUID of the player to add
     * @param username The username of the player to add
     * @param addedBy The UUID of the player or admin who added this player (optional)
     * @param reason The reason for whitelisting (optional)
     * @return true if the player was added successfully, false otherwise
     */
    fun addPlayer(uuid: UUID, username: String, addedBy: UUID? = null, reason: String? = null): Boolean {
        if (!enabled || storage == null) return false
        
        val whitelistPlayer = WhitelistPlayer(
            uuid = uuid,
            username = username,
            addedBy = addedBy,
            reason = reason
        )
        
        return storage?.addPlayer(whitelistPlayer) ?: false
    }
    
    /**
     * Remove a player from the whitelist.
     * @param uuid The UUID of the player to remove
     * @return true if the player was removed successfully, false otherwise
     */
    fun removePlayer(uuid: UUID): Boolean {
        if (!enabled || storage == null) return false
        
        return storage?.removePlayer(uuid) ?: false
    }
    
    /**
     * Check if a player is whitelisted.
     * @param uuid The UUID of the player to check
     * @return true if the player is whitelisted, false otherwise
     */
    fun isWhitelisted(uuid: UUID): Boolean {
        if (!enabled || storage == null) return true // If disabled, allow all players
        
        return storage?.isWhitelisted(uuid) ?: true
    }
    
    /**
     * Get a whitelisted player by UUID.
     * @param uuid The UUID of the player to get
     * @return The WhitelistPlayer if found, null otherwise
     */
    fun getPlayer(uuid: UUID): WhitelistPlayer? {
        if (!enabled || storage == null) return null
        
        return storage?.getPlayer(uuid)
    }
    
    /**
     * Get all whitelisted players.
     * @return A list of all whitelisted players
     */
    fun getAllPlayers(): List<WhitelistPlayer> {
        if (!enabled || storage == null) return emptyList()
        
        return storage?.getAllPlayers() ?: emptyList()
    }
    
    /**
     * Update a player's Discord connection information.
     * @param uuid The UUID of the player to update
     * @param discordConnection The Discord connection information
     * @return true if the update was successful, false otherwise
     */
    fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection): Boolean {
        if (!enabled || storage == null) return false
        
        return storage?.updatePlayerDiscord(uuid, discordConnection) ?: false
    }
    
    /**
     * Enable or disable the whitelist.
     * @param enable true to enable the whitelist, false to disable it
     */
    fun enable(enable: Boolean) {
        this.enabled = enable
        config.enabled = enable
        config.save()
        
        if (enable && storage == null) {
            // Re-initialize storage if enabling and storage doesn't exist
            val storageFactory = WhitelistStorageFactory(plugin)
            storage = storageFactory.createStorage(config)
            storage?.initialize()
            
            // Register event listener if not already registered
            try {
                plugin.server.pluginManager.registerEvents(this, plugin)
            } catch (e: IllegalArgumentException) {
                // Already registered, ignore
            }
        }
        
        plugin.logger.info("Custom whitelist ${if (enable) "enabled" else "disabled"}")
    }
    
    /**
     * Reload the whitelist configuration and storage.
     */
    fun reload() {
        // Close existing storage
        storage?.close()
        
        // Reinitialize
        initialize()
    }
    
    /**
     * Shutdown the whitelist service.
     * Closes the storage connection.
     */
    fun shutdown() {
        storage?.close()
        storage = null
        enabled = false
    }
    
    /**
     * Handle player login events to enforce the whitelist.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (!enabled || storage == null) return
        
        // If the player is already denied for another reason, don't override
        if (event.result != PlayerLoginEvent.Result.ALLOWED) {
            return
        }
        
        val player = event.player
        
        // Check if the player is whitelisted
        if (!isWhitelisted(player.uniqueId)) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_WHITELIST,
                "You are not whitelisted on this server"
            )
        }
    }
}