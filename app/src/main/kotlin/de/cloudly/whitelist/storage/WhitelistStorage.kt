package de.cloudly.whitelist.storage

import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import java.util.UUID

/**
 * Interface for whitelist storage implementations.
 * This interface defines the operations that any whitelist storage implementation must support.
 */
interface WhitelistStorage {
    /**
     * Initialize the storage system.
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean
    
    /**
     * Add a player to the whitelist.
     * @param player The player to add to the whitelist
     * @return true if the player was added successfully, false otherwise
     */
    fun addPlayer(player: WhitelistPlayer): Boolean
    
    /**
     * Remove a player from the whitelist.
     * @param uuid The UUID of the player to remove
     * @return true if the player was removed successfully, false otherwise
     */
    fun removePlayer(uuid: UUID): Boolean
    
    /**
     * Check if a player is whitelisted.
     * @param uuid The UUID of the player to check
     * @return true if the player is whitelisted, false otherwise
     */
    fun isWhitelisted(uuid: UUID): Boolean
    
    /**
     * Get a whitelisted player by UUID.
     * @param uuid The UUID of the player to get
     * @return The WhitelistPlayer if found, null otherwise
     */
    fun getPlayer(uuid: UUID): WhitelistPlayer?
    
    /**
     * Get all whitelisted players.
     * @return A list of all whitelisted players
     */
    fun getAllPlayers(): List<WhitelistPlayer>
    
    /**
     * Update a player's Discord connection information.
     * @param uuid The UUID of the player to update
     * @param discordConnection The Discord connection information
     * @return true if the update was successful, false otherwise
     */
    fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection): Boolean
    
    /**
     * Close the storage connection.
     */
    fun close()
}