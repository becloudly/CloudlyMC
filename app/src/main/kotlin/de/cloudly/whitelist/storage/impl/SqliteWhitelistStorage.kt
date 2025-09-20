package de.cloudly.whitelist.storage.impl

import de.cloudly.whitelist.model.DiscordConnection
import de.cloudly.whitelist.model.WhitelistPlayer
import de.cloudly.whitelist.storage.WhitelistStorage
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.logging.Level

/**
 * SQLite implementation of the WhitelistStorage interface.
 * Stores whitelist data in a SQLite database.
 */
class SqliteWhitelistStorage(
    private val plugin: JavaPlugin,
    private val filePath: String
) : WhitelistStorage {
    
    private var connection: Connection? = null
    
    override fun initialize(): Boolean {
        try {
            // Ensure the parent directory exists
            val file = File(plugin.dataFolder, filePath)
            file.parentFile?.mkdirs()
            
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC")
            
            // Create the connection
            connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            
            // Create the whitelist table if it doesn't exist
            createTable()
            
            // Migrate existing tables to include Discord fields
            migrateTableForDiscord()
            
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize SQLite whitelist storage", e)
            return false
        }
    }
    
    override fun addPlayer(player: WhitelistPlayer): Boolean {
        val sql = """
            INSERT OR REPLACE INTO whitelist (uuid, username, added_by, added_at, reason, discord_id, discord_username, discord_verified, discord_connected_at, discord_verified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, player.uuid.toString())
                statement.setString(2, player.username)
                statement.setString(3, player.addedBy?.toString())
                statement.setTimestamp(4, Timestamp.from(player.addedAt))
                statement.setString(5, player.reason)
                
                // Discord connection fields
                statement.setString(6, player.discordConnection?.discordId)
                statement.setString(7, player.discordConnection?.discordUsername)
                statement.setBoolean(8, player.discordConnection?.verified ?: false)
                statement.setTimestamp(9, player.discordConnection?.connectedAt?.let { Timestamp.from(it) })
                statement.setTimestamp(10, player.discordConnection?.verifiedAt?.let { Timestamp.from(it) })
                
                statement.executeUpdate()
                return true
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to add player to whitelist", e)
        }
        
        return false
    }
    
    override fun removePlayer(uuid: UUID): Boolean {
        val sql = "DELETE FROM whitelist WHERE uuid = ?"
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, uuid.toString())
                
                val rowsAffected = statement.executeUpdate()
                return rowsAffected > 0
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to remove player from whitelist", e)
        }
        
        return false
    }
    
    override fun isWhitelisted(uuid: UUID): Boolean {
        val sql = "SELECT 1 FROM whitelist WHERE uuid = ?"
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, uuid.toString())
                
                val resultSet = statement.executeQuery()
                return resultSet.next()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to check if player is whitelisted", e)
        }
        
        return false
    }
    
    override fun getPlayer(uuid: UUID): WhitelistPlayer? {
        val sql = "SELECT * FROM whitelist WHERE uuid = ?"
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, uuid.toString())
                
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    val discordConnection = createDiscordConnectionFromResultSet(resultSet)
                    
                    return WhitelistPlayer(
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        addedBy = resultSet.getString("added_by")?.let { UUID.fromString(it) },
                        addedAt = resultSet.getTimestamp("added_at").toInstant(),
                        reason = resultSet.getString("reason"),
                        discordConnection = discordConnection
                    )
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get player from whitelist", e)
        }
        
        return null
    }
    
    override fun getAllPlayers(): List<WhitelistPlayer> {
        val players = mutableListOf<WhitelistPlayer>()
        val sql = "SELECT * FROM whitelist"
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                val resultSet = statement.executeQuery()
                
                while (resultSet.next()) {
                    val discordConnection = createDiscordConnectionFromResultSet(resultSet)
                    
                    val player = WhitelistPlayer(
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        addedBy = resultSet.getString("added_by")?.let { UUID.fromString(it) },
                        addedAt = resultSet.getTimestamp("added_at").toInstant(),
                        reason = resultSet.getString("reason"),
                        discordConnection = discordConnection
                    )
                    
                    players.add(player)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all players from whitelist", e)
        }
        
        return players
    }
    
    override fun updatePlayerDiscord(uuid: UUID, discordConnection: DiscordConnection): Boolean {
        val sql = """
            UPDATE whitelist 
            SET discord_id = ?, discord_username = ?, discord_verified = ?, discord_connected_at = ?, discord_verified_at = ?
            WHERE uuid = ?
        """.trimIndent()
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, discordConnection.discordId)
                statement.setString(2, discordConnection.discordUsername)
                statement.setBoolean(3, discordConnection.verified)
                statement.setTimestamp(4, Timestamp.from(discordConnection.connectedAt))
                statement.setTimestamp(5, discordConnection.verifiedAt?.let { Timestamp.from(it) })
                statement.setString(6, uuid.toString())
                
                val rowsAffected = statement.executeUpdate()
                return rowsAffected > 0
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to update player Discord connection", e)
        }
        
        return false
    }
    
    override fun close() {
        try {
            connection?.close()
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to close SQLite connection", e)
        }
    }
    
    /**
     * Create the whitelist table if it doesn't exist.
     */
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS whitelist (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                added_by TEXT,
                added_at TIMESTAMP NOT NULL,
                reason TEXT,
                discord_id TEXT,
                discord_username TEXT,
                discord_verified BOOLEAN DEFAULT FALSE,
                discord_connected_at TIMESTAMP,
                discord_verified_at TIMESTAMP
            )
        """.trimIndent()
        
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(sql)
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to create whitelist table", e)
        }
    }
    
    /**
     * Migrate existing table to include Discord fields.
     */
    private fun migrateTableForDiscord() {
        val columns = listOf(
            "discord_id TEXT",
            "discord_username TEXT", 
            "discord_verified BOOLEAN DEFAULT FALSE",
            "discord_connected_at TIMESTAMP",
            "discord_verified_at TIMESTAMP"
        )
        
        columns.forEach { column ->
            try {
                val sql = "ALTER TABLE whitelist ADD COLUMN $column"
                connection?.createStatement()?.use { statement ->
                    statement.execute(sql)
                }
            } catch (e: SQLException) {
                // Column probably already exists, ignore
            }
        }
    }
    
    /**
     * Create a DiscordConnection from database result set.
     */
    private fun createDiscordConnectionFromResultSet(resultSet: java.sql.ResultSet): DiscordConnection? {
        return try {
            val discordId = resultSet.getString("discord_id")
            val discordUsername = resultSet.getString("discord_username")
            
            if (discordId.isNullOrBlank() || discordUsername.isNullOrBlank()) {
                return null
            }
            
            val verified = resultSet.getBoolean("discord_verified")
            val connectedAt = resultSet.getTimestamp("discord_connected_at")?.toInstant() ?: Instant.now()
            val verifiedAt = resultSet.getTimestamp("discord_verified_at")?.toInstant()
            
            DiscordConnection(
                discordId = discordId,
                discordUsername = discordUsername,
                verified = verified,
                connectedAt = connectedAt,
                verifiedAt = verifiedAt
            )
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to parse Discord connection from database", e)
            null
        }
    }
}