package de.cloudly.whitelist.storage.impl

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
            
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize SQLite whitelist storage", e)
            return false
        }
    }
    
    override fun addPlayer(player: WhitelistPlayer): Boolean {
        val sql = """
            INSERT OR REPLACE INTO whitelist (uuid, username, added_by, added_at, reason)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        
        try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, player.uuid.toString())
                statement.setString(2, player.username)
                statement.setString(3, player.addedBy?.toString())
                statement.setTimestamp(4, Timestamp.from(player.addedAt))
                statement.setString(5, player.reason)
                
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
                    return WhitelistPlayer(
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        addedBy = resultSet.getString("added_by")?.let { UUID.fromString(it) },
                        addedAt = resultSet.getTimestamp("added_at").toInstant(),
                        reason = resultSet.getString("reason")
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
                    val player = WhitelistPlayer(
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        addedBy = resultSet.getString("added_by")?.let { UUID.fromString(it) },
                        addedAt = resultSet.getTimestamp("added_at").toInstant(),
                        reason = resultSet.getString("reason")
                    )
                    
                    players.add(player)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all players from whitelist", e)
        }
        
        return players
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
                reason TEXT
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
}