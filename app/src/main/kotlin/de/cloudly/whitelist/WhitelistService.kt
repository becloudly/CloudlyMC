package de.cloudly.whitelist

import de.cloudly.storage.config.StorageConfig
import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.factory.StorageFactory
import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.logging.Level

/**
 * Service for managing the custom whitelist system using the new generic storage.
 * Handles player whitelist operations and login events.
 */
class WhitelistService(private val plugin: JavaPlugin) : Listener {
    
    private val storageConfig = StorageConfig(plugin) // Use global storage config
    private val storageFactory = StorageFactory(plugin)
    private var repository: DataRepository<WhitelistPlayer>? = null
    private var enabled = false
    
    /**
     * Initialize the whitelist service.
     * Loads configuration and sets up the storage implementation.
     */
    fun initialize() {
        // Load configuration
        storageConfig.load()
        enabled = plugin.config.getBoolean("whitelist.enabled", false)
        
        if (!enabled) {
            plugin.logger.info("Custom whitelist is disabled in config")
            return
        }
        
        // Create and initialize repository using new generic storage
        val deserializer: (String, String) -> WhitelistPlayer? = { data, _ ->
            WhitelistPlayer.deserialize(data)
        }
        
        repository = storageFactory.createRepository("whitelist", storageConfig, deserializer)
        
        if (repository?.initialize() == true) {
            plugin.logger.info("Whitelist repository initialized successfully using new storage system")
            
            // Register event listener
            plugin.server.pluginManager.registerEvents(this, plugin)
        } else {
            plugin.logger.severe("Failed to initialize whitelist repository")
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
        if (!enabled || repository == null) return false
        
        val whitelistPlayer = WhitelistPlayer(
            uuid = player.uniqueId,
            username = player.name,
            addedBy = addedBy,
            reason = reason
        )
        
        return repository?.store(player.uniqueId.toString(), whitelistPlayer) ?: false
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
        if (!enabled || repository == null) return false
        
        val whitelistPlayer = WhitelistPlayer(
            uuid = uuid,
            username = username,
            addedBy = addedBy,
            reason = reason
        )
        
        return repository?.store(uuid.toString(), whitelistPlayer) ?: false
    }
    
    /**
     * Remove a player from the whitelist.
     * @param uuid The UUID of the player to remove
     * @return true if the player was removed successfully, false otherwise
     */
    fun removePlayer(uuid: UUID): Boolean {
        if (!enabled || repository == null) return false
        
        return repository?.remove(uuid.toString()) ?: false
    }
    
    /**
     * Check if a player is whitelisted.
     * @param uuid The UUID of the player to check
     * @return true if the player is whitelisted, false otherwise
     */
    fun isWhitelisted(uuid: UUID): Boolean {
        if (!enabled || repository == null) return true // If disabled, allow all players
        
        return repository?.exists(uuid.toString()) ?: true
    }
    
    /**
     * Get a whitelisted player by UUID.
     * @param uuid The UUID of the player to get
     * @return The WhitelistPlayer if found, null otherwise
     */
    fun getPlayer(uuid: UUID): WhitelistPlayer? {
        if (!enabled || repository == null) return null
        
        return repository?.retrieve(uuid.toString())
    }
    
    /**
     * Get all whitelisted players.
     * @return A list of all whitelisted players
     */
    fun getAllPlayers(): List<WhitelistPlayer> {
        if (!enabled || repository == null) return emptyList()
        
        return repository?.getAll()?.values?.toList() ?: emptyList()
    }
    
    /**
     * Update a player's Discord connection information.
     * @param uuid The UUID of the player to update
     * @param discordConnection The Discord connection information
     * @return true if the update was successful, false otherwise
     */
    fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection): Boolean {
        if (!enabled || repository == null) return false
        
        val existingPlayer = repository?.retrieve(uuid.toString()) ?: return false
        val updatedPlayer = existingPlayer.copy(discordConnection = discordConnection)
        
        return repository?.store(uuid.toString(), updatedPlayer) ?: false
    }
    
    /**
     * Enable or disable the whitelist.
     * @param enable true to enable the whitelist, false to disable it
     */
    fun enable(enable: Boolean) {
        this.enabled = enable
        plugin.config.set("whitelist.enabled", enable)
        plugin.saveConfig()
        
        if (enable && repository == null) {
            // Re-initialize repository if enabling and repository doesn't exist
            val deserializer: (String, String) -> WhitelistPlayer? = { data, _ ->
                WhitelistPlayer.deserialize(data)
            }
            
            repository = storageFactory.createRepository("whitelist", storageConfig, deserializer)
            repository?.initialize()
            
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
        // Close existing repository
        repository?.close()
        
        // Reinitialize
        initialize()
    }
    
    /**
     * Shutdown the whitelist service.
     * Closes the repository connection.
     */
    fun shutdown() {
        repository?.close()
        repository = null
        enabled = false
    }
    
    /**
     * Handle player login events to enforce the whitelist.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (!enabled || repository == null) return
        
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