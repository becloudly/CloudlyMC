package de.cloudly.examples

import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.core.Serializable
import de.cloudly.storage.config.StorageConfig
import de.cloudly.storage.factory.StorageFactory
import org.bukkit.plugin.java.JavaPlugin
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Example showing how to use the new generic storage system
 * for different types of data beyond the whitelist.
 */
class StorageExamples(private val plugin: JavaPlugin) {
    
    private lateinit var storageConfig: StorageConfig
    private lateinit var storageFactory: StorageFactory
    
    // Example repositories for different data types
    private lateinit var playerDataRepository: DataRepository<PlayerData>
    private lateinit var guildRepository: DataRepository<Guild>
    private lateinit var configRepository: DataRepository<ServerConfig>
    
    /**
     * Initialize the storage system and create repositories.
     */
    fun initialize() {
        // Initialize storage configuration
        storageConfig = StorageConfig(plugin)
        storageConfig.load()
        
        // Create storage factory
        storageFactory = StorageFactory(plugin)
        
        // Create repositories for different data types
        createRepositories()
    }
    
    /**
     * Create repositories for different data types.
     */
    private fun createRepositories() {
        // Player data repository
        playerDataRepository = storageFactory.createRepository(
            "player_data",
            storageConfig,
            { data, _ -> PlayerData.deserialize(data) }
        )
        
        // Guild repository
        guildRepository = storageFactory.createRepository(
            "guilds",
            storageConfig,
            { data, _ -> Guild.deserialize(data) }
        )
        
        // Server config repository
        configRepository = storageFactory.createRepository(
            "server_configs",
            storageConfig,
            { data, _ -> ServerConfig.deserialize(data) }
        )
        
        // Initialize all repositories
        playerDataRepository.initialize()
        guildRepository.initialize()
        configRepository.initialize()
        
        plugin.logger.info("All storage repositories initialized successfully")
    }
    
    /**
     * Example usage of the player data repository.
     */
    fun examplePlayerDataUsage() {
        val playerId = UUID.randomUUID()
        
        // Create player data
        val playerData = PlayerData(
            uuid = playerId,
            lastLogin = Instant.now(),
            playtime = 120L,
            level = 15,
            coins = 1500
        )
        
        // Store player data
        playerDataRepository.store(playerId.toString(), playerData)
        
        // Retrieve player data
        val retrievedData = playerDataRepository.retrieve(playerId.toString())
        plugin.logger.info("Retrieved player data: $retrievedData")
        
        // Update player data
        val updatedData = retrievedData?.copy(level = 16, coins = 1600)
        if (updatedData != null) {
            playerDataRepository.store(playerId.toString(), updatedData)
        }
        
        // Check if player exists
        val exists = playerDataRepository.exists(playerId.toString())
        plugin.logger.info("Player exists: $exists")
        
        // Get player count
        val playerCount = playerDataRepository.count()
        plugin.logger.info("Total players: $playerCount")
    }
    
    /**
     * Example usage of the guild repository.
     */
    fun exampleGuildUsage() {
        val guild = Guild(
            id = "guild_001",
            name = "Awesome Guild",
            leader = UUID.randomUUID(),
            members = listOf(UUID.randomUUID(), UUID.randomUUID()),
            createdAt = Instant.now(),
            level = 5
        )
        
        // Store guild
        guildRepository.store(guild.id, guild)
        
        // Get all guilds
        val allGuilds = guildRepository.getAll()
        plugin.logger.info("Total guilds: ${allGuilds.size}")
        
        // Remove guild
        guildRepository.remove(guild.id)
    }
    
    /**
     * Cleanup resources.
     */
    fun shutdown() {
        playerDataRepository.close()
        guildRepository.close()
        configRepository.close()
    }
}

/**
 * Example data class for player statistics.
 */
data class PlayerData(
    val uuid: UUID,
    val lastLogin: Instant,
    val playtime: Long, // in minutes
    val level: Int,
    val coins: Int
) : Serializable {
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("uuid", uuid.toString())
        json.put("lastLogin", lastLogin.epochSecond)
        json.put("playtime", playtime)
        json.put("level", level)
        json.put("coins", coins)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "player_data"
    
    companion object {
        fun deserialize(data: String): PlayerData? {
            return try {
                val json = JSONObject(data)
                PlayerData(
                    uuid = UUID.fromString(json.getString("uuid")),
                    lastLogin = Instant.ofEpochSecond(json.getLong("lastLogin")),
                    playtime = json.getLong("playtime"),
                    level = json.getInt("level"),
                    coins = json.getInt("coins")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Example data class for guild system.
 */
data class Guild(
    val id: String,
    val name: String,
    val leader: UUID,
    val members: List<UUID>,
    val createdAt: Instant,
    val level: Int
) : Serializable {
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("leader", leader.toString())
        json.put("members", members.map { it.toString() })
        json.put("createdAt", createdAt.epochSecond)
        json.put("level", level)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "guild"
    
    companion object {
        fun deserialize(data: String): Guild? {
            return try {
                val json = JSONObject(data)
                val membersArray = json.getJSONArray("members")
                val members = (0 until membersArray.length()).map { i ->
                    UUID.fromString(membersArray.getString(i))
                }
                
                Guild(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    leader = UUID.fromString(json.getString("leader")),
                    members = members,
                    createdAt = Instant.ofEpochSecond(json.getLong("createdAt")),
                    level = json.getInt("level")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Example data class for server configuration.
 */
data class ServerConfig(
    val key: String,
    val value: String,
    val description: String,
    val updatedAt: Instant
) : Serializable {
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("key", key)
        json.put("value", value)
        json.put("description", description)
        json.put("updatedAt", updatedAt.epochSecond)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "server_config"
    
    companion object {
        fun deserialize(data: String): ServerConfig? {
            return try {
                val json = JSONObject(data)
                ServerConfig(
                    key = json.getString("key"),
                    value = json.getString("value"),
                    description = json.getString("description"),
                    updatedAt = Instant.ofEpochSecond(json.getLong("updatedAt"))
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
