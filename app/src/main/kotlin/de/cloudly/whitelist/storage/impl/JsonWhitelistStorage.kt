package de.cloudly.whitelist.storage.impl

import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import de.cloudly.whitelist.storage.WhitelistStorage
import org.bukkit.plugin.java.JavaPlugin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * JSON implementation of the WhitelistStorage interface.
 * Stores whitelist data in a JSON file using org.json library.
 */
class JsonWhitelistStorage(
    private val plugin: JavaPlugin,
    private val filePath: String
) : WhitelistStorage {
    
    private val players = ConcurrentHashMap<UUID, WhitelistPlayer>()
    private val file: File
    
    init {
        // Resolve the file path relative to the plugin's data folder
        file = File(plugin.dataFolder, filePath)
    }
    
    override fun initialize(): Boolean {
        try {
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            
            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile()
                // Write an empty array to the file
                Files.write(file.toPath(), "[]".toByteArray())
                return true
            }
            
            // Load existing data
            loadData()
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize JSON whitelist storage", e)
            return false
        }
    }
    
    override fun addPlayer(player: WhitelistPlayer): Boolean {
        try {
            players[player.uuid] = player
            saveData()
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to add player to whitelist", e)
            return false
        }
    }
    
    override fun removePlayer(uuid: UUID): Boolean {
        try {
            if (players.remove(uuid) != null) {
                saveData()
                return true
            }
            return false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to remove player from whitelist", e)
            return false
        }
    }
    
    override fun isWhitelisted(uuid: UUID): Boolean {
        return players.containsKey(uuid)
    }
    
    override fun getPlayer(uuid: UUID): WhitelistPlayer? {
        return players[uuid]
    }
    
    override fun getAllPlayers(): List<WhitelistPlayer> {
        return players.values.toList()
    }
    
    override fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection): Boolean {
        try {
            val existingPlayer = players[uuid] ?: return false
            val updatedPlayer = existingPlayer.copy(discordConnection = discordConnection)
            players[uuid] = updatedPlayer
            saveData()
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to update player Discord connection", e)
            return false
        }
    }
    
    override fun close() {
        // No resources to close for JSON storage
    }
    
    /**
     * Load whitelist data from the JSON file.
     */
    private fun loadData() {
        if (!file.exists()) {
            return
        }
        
        try {
            val content = Files.readString(file.toPath())
            val jsonArray = JSONArray(content)
            
            // Clear existing data and add loaded players
            players.clear()
            
            for (i in 0 until jsonArray.length()) {
                val playerJson = jsonArray.getJSONObject(i)
                val player = parseWhitelistPlayer(playerJson)
                if (player != null) {
                    players[player.uuid] = player
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load whitelist data from JSON", e)
        }
    }
    
    /**
     * Save whitelist data to the JSON file.
     */
    private fun saveData() {
        try {
            val jsonArray = JSONArray()
            
            players.values.forEach { player ->
                val playerJson = serializeWhitelistPlayer(player)
                jsonArray.put(playerJson)
            }
            
            Files.write(file.toPath(), jsonArray.toString(2).toByteArray())
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save whitelist data to JSON", e)
        }
    }
    
    /**
     * Parse a WhitelistPlayer from a JSONObject.
     */
    private fun parseWhitelistPlayer(json: JSONObject): WhitelistPlayer? {
        return try {
            val uuid = UUID.fromString(json.getString("uuid"))
            val username = json.getString("username")
            val addedBy = if (json.has("addedBy") && !json.isNull("addedBy")) {
                UUID.fromString(json.getString("addedBy"))
            } else null
            val addedAt = Instant.ofEpochSecond(json.getLong("addedAt"))
            val reason = if (json.has("reason") && !json.isNull("reason")) {
                json.getString("reason")
            } else null
            
            val discordConnection = if (json.has("discordConnection") && !json.isNull("discordConnection")) {
                parseDiscordConnection(json.getJSONObject("discordConnection"))
            } else null
            
            WhitelistPlayer(
                uuid = uuid,
                username = username,
                addedBy = addedBy,
                addedAt = addedAt,
                reason = reason,
                discordConnection = discordConnection
            )
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to parse whitelist player from JSON", e)
            null
        }
    }
    
    /**
     * Parse a DiscordConnection from a JSONObject.
     */
    private fun parseDiscordConnection(json: JSONObject): DiscordConnection? {
        return try {
            DiscordConnection(
                discordId = json.getString("discordId"),
                discordUsername = json.getString("discordUsername"),
                verified = json.getBoolean("verified"),
                connectedAt = Instant.ofEpochSecond(json.getLong("connectedAt")),
                verifiedAt = if (json.has("verifiedAt") && !json.isNull("verifiedAt")) {
                    Instant.ofEpochSecond(json.getLong("verifiedAt"))
                } else null
            )
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to parse Discord connection from JSON", e)
            null
        }
    }
    
    /**
     * Serialize a WhitelistPlayer to a JSONObject.
     */
    private fun serializeWhitelistPlayer(player: WhitelistPlayer): JSONObject {
        val json = JSONObject()
        json.put("uuid", player.uuid.toString())
        json.put("username", player.username)
        json.put("addedBy", player.addedBy?.toString())
        json.put("addedAt", player.addedAt.epochSecond)
        json.put("reason", player.reason)
        
        if (player.discordConnection != null) {
            json.put("discordConnection", serializeDiscordConnection(player.discordConnection))
        }
        
        return json
    }
    
    /**
     * Serialize a DiscordConnection to a JSONObject.
     */
    private fun serializeDiscordConnection(discord: DiscordConnection): JSONObject {
        val json = JSONObject()
        json.put("discordId", discord.discordId)
        json.put("discordUsername", discord.discordUsername)
        json.put("verified", discord.verified)
        json.put("connectedAt", discord.connectedAt.epochSecond)
        json.put("verifiedAt", discord.verifiedAt?.epochSecond)
        return json
    }
}