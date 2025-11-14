package de.cloudly.whitelist

import de.cloudly.storage.config.StorageConfig
import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.factory.StorageFactory
import de.cloudly.utils.AuditLogger
import de.cloudly.whitelist.attempts.WhitelistAttemptService
import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant
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
    private var auditLogger: AuditLogger? = null
    private var enabled = false
    private var listenerRegistered = false
    private var attemptService: WhitelistAttemptService? = null

    fun setAttemptService(service: WhitelistAttemptService) {
        attemptService = service
    }
    
    /**
     * Log an audit event for whitelist changes.
     * Records timestamp, action, target, actor, and details for security and compliance.
     * Logs to both the plugin logger and a dedicated audit log file.
     * 
     * @param action The action performed (e.g., WHITELIST_ADD, WHITELIST_REMOVE)
     * @param target The UUID of the affected player (or system UUID for global actions)
     * @param actor The UUID of the player or admin who performed the action
     * @param details Additional details about the action
     */
    private fun logAuditEvent(action: String, target: UUID, actor: UUID?, details: String?) {
        val timestamp = de.cloudly.utils.TimeUtils.formatTimestamp(Instant.now())
        plugin.logger.info("[AUDIT] $timestamp - $action - Target: $target - Actor: $actor - Details: $details")
        
        // Also store in dedicated audit log file
        auditLogger?.log(action, target, actor, details)
    }
    
    /**
     * Initialize the whitelist service.
     * Loads configuration and sets up the storage implementation.
     */
    fun initialize() {
        // Initialize audit logger (always, even if whitelist is disabled, for system events)
        if (auditLogger == null) {
            auditLogger = AuditLogger(plugin)
        }
        
        // Load configuration
        storageConfig.load()
        enabled = plugin.config.getBoolean("whitelist.enabled", false)
        
        if (!enabled) {
            if (plugin.config.getBoolean("plugin.debug", false)) {
                plugin.logger.info("Custom whitelist is disabled in config")
            }
            return
        }
        
        // Create and initialize repository using new generic storage
        val deserializer: (String, String) -> WhitelistPlayer? = { data, _ ->
            WhitelistPlayer.deserialize(data)
        }
        
        repository = storageFactory.createRepository("whitelist", storageConfig, deserializer)
        
        if (repository?.initialize() == true) {
            plugin.logger.info("Whitelist repository initialized successfully using new storage system")
            
            // Register event listener if not already registered
            if (!listenerRegistered) {
                plugin.server.pluginManager.registerEvents(this, plugin)
                listenerRegistered = true
            }
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
        
        val result = repository?.store(player.uniqueId.toString(), whitelistPlayer) ?: false
        
        if (result) {
            val details = "Username: ${player.name}${reason?.let { ", Reason: $it" } ?: ""}"
            logAuditEvent("WHITELIST_ADD", player.uniqueId, addedBy, details)
            attemptService?.removeAttempt(player.uniqueId)
        }
        
        return result
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
        
        val result = repository?.store(uuid.toString(), whitelistPlayer) ?: false
        
        if (result) {
            val details = "Username: $username${reason?.let { ", Reason: $it" } ?: ""}"
            logAuditEvent("WHITELIST_ADD", uuid, addedBy, details)
            attemptService?.removeAttempt(uuid)
        }
        
        return result
    }
    
    /**
     * Remove a player from the whitelist.
     * @param uuid The UUID of the player to remove
     * @param removedBy The UUID of the player or admin who removed this player (optional)
     * @return true if the player was removed successfully, false otherwise
     */
    fun removePlayer(uuid: UUID, removedBy: UUID? = null): Boolean {
        if (!enabled || repository == null) return false
        
        // Get player info before removal for audit log
        val player = repository?.retrieve(uuid.toString())
        val username = player?.username ?: "Unknown"
        
        val result = repository?.remove(uuid.toString()) ?: false
        
        if (result) {
            val details = "Username: $username"
            logAuditEvent("WHITELIST_REMOVE", uuid, removedBy, details)
        }
        
        return result
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
     * Get a paginated list of whitelisted players.
     * @param offset The starting index (0-based)
     * @param limit The maximum number of players to return
     * @return A list of whitelisted players within the specified range
     */
    fun getPlayers(offset: Int, limit: Int): List<WhitelistPlayer> {
        if (!enabled || repository == null) return emptyList()
        
        return repository?.getPaginated(offset, limit) ?: emptyList()
    }
    
    /**
     * Get the total count of whitelisted players.
     * @return The number of whitelisted players
     */
    fun getPlayerCount(): Long {
        if (!enabled || repository == null) return 0
        
        return repository?.count() ?: 0
    }
    
    /**
     * Update a player's Discord connection information.
     * @param uuid The UUID of the player to update
     * @param discordConnection The Discord connection information
     * @param updatedBy The UUID of the player or admin who updated this connection (optional)
     * @return true if the update was successful, false otherwise
     */
    fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection, updatedBy: UUID? = null): Boolean {
        if (!enabled || repository == null) return false
        
        val existingPlayer = repository?.retrieve(uuid.toString()) ?: return false
        val updatedPlayer = existingPlayer.copy(discordConnection = discordConnection)
        
        val result = repository?.store(uuid.toString(), updatedPlayer) ?: false
        
        if (result) {
            val details = "Discord: ${discordConnection.discordUsername}, Verified: ${discordConnection.verified}"
            logAuditEvent("DISCORD_UPDATE", uuid, updatedBy, details)
        }
        
        return result
    }

            /**
             * Remove the Discord connection from a player.
             * @param uuid The UUID of the player to update
             * @param updatedBy The UUID of the actor performing the change (optional)
             * @return true if the unlink succeeded, false otherwise
             */
            fun clearPlayerDiscord(uuid: UUID, updatedBy: UUID? = null): Boolean {
                if (!enabled || repository == null) return false

                val existingPlayer = repository?.retrieve(uuid.toString()) ?: return false
                if (existingPlayer.discordConnection == null) {
                    return false
                }

                val updatedPlayer = existingPlayer.copy(discordConnection = null)
                val result = repository?.store(uuid.toString(), updatedPlayer) ?: false

                if (result) {
                    logAuditEvent("DISCORD_CLEAR", uuid, updatedBy, "Discord-Verknüpfung entfernt")
                }

                return result
            }
    
    /**
     * Enable or disable the whitelist.
     * @param enable true to enable the whitelist, false to disable it
     * @param changedBy The UUID of the player or admin who changed the whitelist state (optional)
     */
    fun enable(enable: Boolean, changedBy: UUID? = null) {
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
            if (!listenerRegistered) {
                plugin.server.pluginManager.registerEvents(this, plugin)
                listenerRegistered = true
            }
        }
        
        // Log audit event for enabling/disabling whitelist
        val action = if (enable) "WHITELIST_ENABLE" else "WHITELIST_DISABLE"
        val systemUuid = UUID(0, 0) // Use system UUID for global actions
        val details = "State changed to: ${if (enable) "enabled" else "disabled"}"
        logAuditEvent(action, systemUuid, changedBy, details)
        
        plugin.logger.info("Custom whitelist ${if (enable) "enabled" else "disabled"}")
    }
    
    /**
     * Shutdown the whitelist service.
     * Closes the repository connection and audit logger.
     */
    fun shutdown() {
        repository?.close()
        repository = null
        
        auditLogger?.close()
        auditLogger = null
        
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
            val kickMessage = "Du bist nicht auf der Whitelist dieses Servers"
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage)
            val address = event.address?.hostAddress
            attemptService?.recordAttempt(player.uniqueId, player.name, address, kickMessage)
        }
    }

    /**
     * Locate a whitelisted player by their Discord identifier.
     * Returns the first player that matches the given Discord ID, if any.
     */
    fun findPlayerByDiscordId(discordId: String): WhitelistPlayer? {
        if (!enabled || repository == null) return null

        return repository?.getAll()
            ?.values
            ?.firstOrNull { it.discordConnection?.discordId == discordId }
    }
}