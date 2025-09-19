package de.cloudly.whitelist.storage.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import de.cloudly.whitelist.model.WhitelistPlayer
import de.cloudly.whitelist.storage.WhitelistStorage
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * JSON implementation of the WhitelistStorage interface.
 * Stores whitelist data in a JSON file.
 */
class JsonWhitelistStorage(
    private val plugin: JavaPlugin,
    private val filePath: String
) : WhitelistStorage {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
        .create()
    private val players = ConcurrentHashMap<UUID, WhitelistPlayer>()
    private val file: File
    
    /**
     * Custom TypeAdapter for java.time.Instant to handle serialization/deserialization
     */
    private class InstantTypeAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
        override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.epochSecond ?: 0L)
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant {
            return Instant.ofEpochSecond(json?.asLong ?: 0L)
        }
    }
    
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
                FileWriter(file).use { writer ->
                    writer.write("[]")
                }
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
            FileReader(file).use { reader ->
                val listType = object : TypeToken<List<WhitelistPlayer>>() {}.type
                val loadedPlayers: List<WhitelistPlayer> = gson.fromJson(reader, listType) ?: emptyList()
                
                // Clear existing data and add loaded players
                players.clear()
                loadedPlayers.forEach { player ->
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
            FileWriter(file).use { writer ->
                val playerList = players.values.toList()
                gson.toJson(playerList, writer)
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save whitelist data to JSON", e)
        }
    }
}