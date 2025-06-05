/*
 * Cloudly - Whitelist Manager
 * 
 * Manages the custom whitelist system that overrides Minecraft's default whitelist.
 * Provides high-performance database operations with caching and async support.
 * All methods are null-safe and handle errors gracefully.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.sql.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * WhitelistManager handles all whitelist-related database operations
 * Features caching, async operations, and full null-safety
 */
object WhitelistManager {
    
    private lateinit var plugin: CloudlyPlugin
    private var isInitialized = false
    
    // Cache for whitelist data
    private val whitelistCache = ConcurrentHashMap<UUID, WhitelistEntry>()
    private val usernameCache = ConcurrentHashMap<String, UUID>()
    private var whitelistEnabled = false
    private var lastCacheUpdate = 0L
    private var cacheDuration = 30L * 60 * 1000 // 30 minutes in milliseconds
    
    // Database connection
    private var connection: Connection? = null
    
    /**
     * Data class for whitelist entries
     */
    data class WhitelistEntry(
        val uuid: UUID,
        val username: String,
        val addedBy: String,
        val addedAt: Long,
        val active: Boolean = true
    )
    
    /**
     * Data class for whitelist log entries
     */
    data class WhitelistLog(
        val id: Int,
        val actionType: String,
        val username: String,
        val performedBy: String,
        val timestamp: Long
    )
    
    /**
     * Initialize the whitelist manager
     */
    suspend fun initialize(pluginInstance: CloudlyPlugin): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            plugin = pluginInstance
            
            // Load cache settings from config
            loadCacheSettings()
            
            // Initialize database connection
            if (!initializeDatabase()) {
                plugin.logger.severe("Failed to initialize whitelist database")
                return@withContext false
            }
            
            // Create tables if they don't exist
            if (!createTables()) {
                plugin.logger.severe("Failed to create whitelist tables")
                return@withContext false
            }
            
            // Load whitelist status from database
            loadWhitelistStatus()
            
            // Load cache from database
            loadCacheFromDatabase()
            
            isInitialized = true
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error initializing WhitelistManager", e)
            false
        }
    }
    
    /**
     * Load cache settings from configuration
     */
    private fun loadCacheSettings() {
        try {
            val config = plugin.config
            cacheDuration = config.getLong("whitelist.cache.duration", 30) * 60 * 1000
            
            plugin.logger.info("Whitelist cache duration set to ${cacheDuration / 60000} minutes")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error loading cache settings, using defaults", e)
        }
    }
    
    /**
     * Initialize database connection
     */
    private suspend fun initializeDatabase(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = plugin.config
            val dbType = config.getString("database.type", "sqlite")?.lowercase()
            
            connection = when (dbType) {
                "sqlite" -> {
                    val dbFile = config.getString("database.sqlite.file", "cloudly.db") ?: "cloudly.db"
                    val dbPath = plugin.dataFolder.resolve(dbFile)
                    DriverManager.getConnection("jdbc:sqlite:$dbPath")
                }
                "mysql" -> {
                    val host = config.getString("database.mysql.host", "localhost") ?: "localhost"
                    val port = config.getInt("database.mysql.port", 3306)
                    val database = config.getString("database.mysql.database", "cloudly") ?: "cloudly"
                    val username = config.getString("database.mysql.username", "username") ?: "username"
                    val password = config.getString("database.mysql.password", "password") ?: "password"
                    
                    val url = "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true"
                    DriverManager.getConnection(url, username, password)
                }
                else -> {
                    plugin.logger.severe("Unsupported database type: $dbType")
                    return@withContext false
                }
            }
            
            connection?.isValid(5) == true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error initializing database connection", e)
            false
        }
    }
    
    /**
     * Create database tables
     */
    private suspend fun createTables(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext false
              // Create whitelist_users table
            val dbType = plugin.config.getString("database.type", "sqlite")?.lowercase() ?: "sqlite"
            val createUsersTable = if (dbType == "mysql") {
                """
                CREATE TABLE IF NOT EXISTS whitelist_users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL UNIQUE,
                    username VARCHAR(16) NOT NULL,
                    added_by VARCHAR(16) NOT NULL,
                    added_at BIGINT NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE
                )
                """.trimIndent()
            } else {
                """
                CREATE TABLE IF NOT EXISTS whitelist_users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid VARCHAR(36) NOT NULL UNIQUE,
                    username VARCHAR(16) NOT NULL,
                    added_by VARCHAR(16) NOT NULL,
                    added_at BIGINT NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT 1
                )
                """.trimIndent()
            }
            
            // Create whitelist_settings table
            val createSettingsTable = if (dbType == "mysql") {
                """
                CREATE TABLE IF NOT EXISTS whitelist_settings (
                    id INT PRIMARY KEY,
                    enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    last_modified BIGINT NOT NULL,
                    modified_by VARCHAR(16) NOT NULL
                )
                """.trimIndent()
            } else {
                """
                CREATE TABLE IF NOT EXISTS whitelist_settings (
                    id INTEGER PRIMARY KEY,
                    enabled BOOLEAN NOT NULL DEFAULT 0,
                    last_modified BIGINT NOT NULL,
                    modified_by VARCHAR(16) NOT NULL
                )
                """.trimIndent()
            }
            
            conn.createStatement().use { stmt ->
                stmt.execute(createUsersTable)
                stmt.execute(createSettingsTable)
            }
            
            // Insert default settings if table is empty
            val checkSettings = "SELECT COUNT(*) FROM whitelist_settings"
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(checkSettings)
                if (rs.next() && rs.getInt(1) == 0) {
                    val insertDefault = """
                        INSERT INTO whitelist_settings (id, enabled, last_modified, modified_by) 
                        VALUES (1, 0, ?, 'SYSTEM')
                    """.trimIndent()
                    
                    conn.prepareStatement(insertDefault).use { pstmt ->
                        pstmt.setLong(1, System.currentTimeMillis())
                        pstmt.execute()
                    }
                }
            }
            
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error creating database tables", e)
            false
        }
    }
    
    /**
     * Load whitelist status from database
     */
    private suspend fun loadWhitelistStatus() = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext
            
            val query = "SELECT enabled FROM whitelist_settings WHERE id = 1"
            conn.prepareStatement(query).use { pstmt ->
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    whitelistEnabled = rs.getBoolean("enabled")
                }
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error loading whitelist status", e)
        }
    }
    
    /**
     * Load cache from database
     */
    private suspend fun loadCacheFromDatabase() = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext
            
            whitelistCache.clear()
            usernameCache.clear()
            
            val query = "SELECT uuid, username, added_by, added_at, active FROM whitelist_users WHERE active = 1"
            conn.prepareStatement(query).use { pstmt ->
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val username = rs.getString("username")
                    val addedBy = rs.getString("added_by")
                    val addedAt = rs.getLong("added_at")
                    val active = rs.getBoolean("active")
                    
                    val entry = WhitelistEntry(uuid, username, addedBy, addedAt, active)
                    whitelistCache[uuid] = entry
                    usernameCache[username.lowercase()] = uuid
                }
            }
            
            lastCacheUpdate = System.currentTimeMillis()
            plugin.logger.info("Loaded ${whitelistCache.size} whitelist entries into cache")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error loading cache from database", e)
        }
    }
    
    /**
     * Check if whitelist is enabled
     */
    fun isWhitelistEnabled(): Boolean = whitelistEnabled
    
    /**
     * Enable or disable whitelist
     */
    suspend fun setWhitelistEnabled(enabled: Boolean, modifiedBy: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext false
            
            val query = """
                UPDATE whitelist_settings 
                SET enabled = ?, last_modified = ?, modified_by = ? 
                WHERE id = 1
            """.trimIndent()
            
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setBoolean(1, enabled)
                pstmt.setLong(2, System.currentTimeMillis())
                pstmt.setString(3, modifiedBy)
                pstmt.executeUpdate()
            }
            
            whitelistEnabled = enabled
            
            // Add log entry
            val actionType = if (enabled) "ENABLE" else "DISABLE"
            addLogEntry(actionType, "SYSTEM", modifiedBy)
            
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error updating whitelist status", e)
            false
        }
    }
    
    /**
     * Check if player is whitelisted (with caching)
     */
    suspend fun isPlayerWhitelisted(uuid: UUID): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            // Check cache first
            if (isCacheValid()) {
                return@withContext whitelistCache.containsKey(uuid)
            }
            
            // Refresh cache and check again
            loadCacheFromDatabase()
            whitelistCache.containsKey(uuid)
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error checking if player is whitelisted", e)
            false
        }
    }
    
    /**
     * Check if player is whitelisted by username
     */
    suspend fun isPlayerWhitelisted(username: String): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            val uuid = usernameCache[username.lowercase()]
            if (uuid != null) {
                return@withContext isPlayerWhitelisted(uuid)
            }
            
            // Try to get UUID from Bukkit
            val offlinePlayer = Bukkit.getOfflinePlayer(username)
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline) {
                return@withContext isPlayerWhitelisted(offlinePlayer.uniqueId)
            }
            
            false
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error checking if player is whitelisted by username", e)
            false
        }
    }
    
    /**
     * Add player to whitelist
     */
    suspend fun addPlayerToWhitelist(player: OfflinePlayer, addedBy: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext false
            
            // Check if player is already whitelisted
            if (isPlayerWhitelisted(player.uniqueId)) {                
                return@withContext false
            }
            
            val dbType = plugin.config.getString("database.type", "sqlite")?.lowercase() ?: "sqlite"
            val query = if (dbType == "mysql") {
                """
                INSERT INTO whitelist_users (uuid, username, added_by, added_at, active) 
                VALUES (?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE 
                username = VALUES(username),
                added_by = VALUES(added_by),
                added_at = VALUES(added_at),
                active = 1
                """.trimIndent()
            } else {
                """
                INSERT INTO whitelist_users (uuid, username, added_by, added_at, active) 
                VALUES (?, ?, ?, ?, 1)
                ON CONFLICT(uuid) DO UPDATE SET 
                username = excluded.username,
                added_by = excluded.added_by,
                added_at = excluded.added_at,
                active = 1
                """.trimIndent()
            }
            
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setString(1, player.uniqueId.toString())
                pstmt.setString(2, player.name ?: "Unknown")
                pstmt.setString(3, addedBy)
                pstmt.setLong(4, System.currentTimeMillis())
                pstmt.executeUpdate()
            }
            
            // Update cache
            val entry = WhitelistEntry(
                player.uniqueId,
                player.name ?: "Unknown",
                addedBy,
                System.currentTimeMillis()
            )
            whitelistCache[player.uniqueId] = entry
            usernameCache[entry.username.lowercase()] = player.uniqueId
            
            // Add log entry
            addLogEntry("ADD", player.name ?: "Unknown", addedBy)
            
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error adding player to whitelist", e)
            false
        }
    }
    
    /**
     * Remove player from whitelist
     */
    suspend fun removePlayerFromWhitelist(uuid: UUID): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext false
            
            // Get username for logging
            val entry = whitelistCache[uuid]
            val username = entry?.username ?: getPlayerUsername(uuid) ?: "Unknown"
            
            val query = "UPDATE whitelist_users SET active = 0 WHERE uuid = ?"
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setString(1, uuid.toString())
                val rows = pstmt.executeUpdate()
                
                if (rows > 0) {
                    // Remove from cache
                    val entry = whitelistCache.remove(uuid)
                    entry?.let { usernameCache.remove(it.username.lowercase()) }
                    
                    // Add log entry - get performer from the command context
                    val performer = getPerformerForUUID(uuid) ?: "CONSOLE"
                    addLogEntry("REMOVE", username, performer)
                    
                    return@withContext true
                }
            }
            
            false
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error removing player from whitelist", e)
            false
        }
    }
    
    /**
     * Get username for UUID (if not in cache)
     */
    private suspend fun getPlayerUsername(uuid: UUID): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext null
            
            val query = "SELECT username FROM whitelist_users WHERE uuid = ?"
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setString(1, uuid.toString())
                val rs = pstmt.executeQuery()
                
                if (rs.next()) {
                    rs.getString("username")
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error getting username for UUID", e)
            null
        }
    }
    
    /**
     * Get the performer (for logging)
     * This is a placeholder - in a real implementation, you'd get this from command context
     */
    private suspend fun getPerformerForUUID(uuid: UUID): String? {
        // This would normally come from a ThreadLocal or similar in a real implementation
        // For now, we'll just return null and let the caller handle it
        return null
    }
    
    /**
     * Purge inactive whitelist entries
     */
    suspend fun purgeInactiveEntries(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext 0
            
            val query = "DELETE FROM whitelist_users WHERE active = 0"
            conn.prepareStatement(query).use { pstmt ->
                val count = pstmt.executeUpdate()
                
                plugin.logger.info("Purged $count inactive whitelist entries")
                count
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error purging inactive whitelist entries", e)
            0
        }
    }
    
    /**
     * Get whitelist activity logs
     */
    suspend fun getWhitelistLogs(count: Int): List<WhitelistLog> = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext emptyList()
            
            // Check if whitelist_logs table exists, create if not
            ensureLogsTableExists()
            
            val query = """
                SELECT id, action_type, username, performed_by, timestamp 
                FROM whitelist_logs 
                ORDER BY timestamp DESC 
                LIMIT ?
            """.trimIndent()
            
            val logs = mutableListOf<WhitelistLog>()
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setInt(1, count)
                
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    val id = rs.getInt("id")
                    val actionType = rs.getString("action_type")
                    val username = rs.getString("username")
                    val performedBy = rs.getString("performed_by")
                    val timestamp = rs.getLong("timestamp")
                    
                    logs.add(WhitelistLog(id, actionType, username, performedBy, timestamp))
                }
            }
            
            logs
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error getting whitelist logs", e)
            emptyList()
        }
    }
    
    /**
     * Ensure logs table exists
     */
    private suspend fun ensureLogsTableExists() = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext
            
            val dbType = plugin.config.getString("database.type", "sqlite")?.lowercase() ?: "sqlite"
            val createLogsTable = if (dbType == "mysql") {
                """
                CREATE TABLE IF NOT EXISTS whitelist_logs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    action_type VARCHAR(50) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    performed_by VARCHAR(16) NOT NULL,
                    timestamp BIGINT NOT NULL
                )
                """.trimIndent()
            } else {
                """
                CREATE TABLE IF NOT EXISTS whitelist_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action_type VARCHAR(50) NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    performed_by VARCHAR(16) NOT NULL,
                    timestamp BIGINT NOT NULL
                )
                """.trimIndent()
            }
            
            conn.createStatement().use { stmt ->
                stmt.execute(createLogsTable)
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error creating logs table", e)
        }
    }
    
    /**
     * Add a log entry
     */
    private suspend fun addLogEntry(actionType: String, username: String, performedBy: String) = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext
            
            // Ensure logs table exists
            ensureLogsTableExists()
            
            val query = """
                INSERT INTO whitelist_logs (action_type, username, performed_by, timestamp)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setString(1, actionType)
                pstmt.setString(2, username)
                pstmt.setString(3, performedBy)
                pstmt.setLong(4, System.currentTimeMillis())
                pstmt.executeUpdate()
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error adding log entry", e)
        }
    }
    
    /**
     * Clear all whitelist entries
     */
    suspend fun clearWhitelist(clearedBy: String): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext 0
            
            // First, get all usernames to log them
            val usernames = mutableListOf<String>()
            val selectQuery = "SELECT username FROM whitelist_users WHERE active = 1"
            conn.prepareStatement(selectQuery).use { pstmt ->
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    usernames.add(rs.getString("username"))
                }
            }
            
            // Then clear the whitelist
            val query = "UPDATE whitelist_users SET active = 0"
            conn.prepareStatement(query).use { pstmt ->
                val count = pstmt.executeUpdate()
                
                // Log each removal
                usernames.forEach { username ->
                    addLogEntry("CLEAR", username, clearedBy)
                }
                
                // Clear cache
                whitelistCache.clear()
                usernameCache.clear()
                
                plugin.logger.info("Cleared $count whitelist entries by $clearedBy")
                count
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error clearing whitelist", e)
            0
        }
    }
    
    /**
     * Export whitelist to a file
     */
    suspend fun exportWhitelist(exportFile: java.io.File): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext 0
            
            // Get all active whitelist entries
            val query = "SELECT uuid, username, added_by, added_at FROM whitelist_users WHERE active = 1"
            val entries = mutableListOf<WhitelistEntry>()
            
            conn.prepareStatement(query).use { pstmt ->
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val username = rs.getString("username")
                    val addedBy = rs.getString("added_by")
                    val addedAt = rs.getLong("added_at")
                    
                    entries.add(WhitelistEntry(uuid, username, addedBy, addedAt))
                }
            }
            
            if (entries.isEmpty()) {
                return@withContext 0
            }
            
            // Ensure parent directory exists
            if (!exportFile.parentFile.exists()) {
                exportFile.parentFile.mkdirs()
            }
            
            // Create export JSON
            val json = org.json.JSONArray()
            entries.forEach { entry ->
                val jsonEntry = org.json.JSONObject()
                jsonEntry.put("uuid", entry.uuid.toString())
                jsonEntry.put("username", entry.username)
                jsonEntry.put("addedBy", entry.addedBy)
                jsonEntry.put("addedAt", entry.addedAt)
                json.put(jsonEntry)
            }
            
            // Write to file
            java.io.FileWriter(exportFile).use { writer ->
                writer.write(json.toString(2))
            }
            
            plugin.logger.info("Exported ${entries.size} whitelist entries to ${exportFile.name}")
            entries.size
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error exporting whitelist", e)
            0
        }
    }
    
    /**
     * Import whitelist from a file
     */
    suspend fun importWhitelist(importFile: java.io.File, addedBy: String): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!importFile.exists() || !importFile.isFile) {
                return@withContext 0
            }
            
            // Read JSON file
            val jsonContent = importFile.readText()
            val jsonArray = org.json.JSONArray(jsonContent)
            
            var importCount = 0
            
            for (i in 0 until jsonArray.length()) {
                val jsonEntry = jsonArray.getJSONObject(i)
                
                // Check if entry has required fields
                if (!jsonEntry.has("uuid") || !jsonEntry.has("username")) {
                    continue
                }
                
                val uuidStr = jsonEntry.getString("uuid")
                val username = jsonEntry.getString("username")
                
                try {
                    val uuid = UUID.fromString(uuidStr)
                    val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
                    
                    // Add to whitelist
                    if (addPlayerToWhitelist(offlinePlayer, addedBy)) {
                        // Log the import
                        addLogEntry("IMPORT", username, addedBy)
                        importCount++
                    }
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Error importing whitelist entry: $username", e)
                }
            }
            
            plugin.logger.info("Imported $importCount whitelist entries from ${importFile.name}")
            importCount
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error importing whitelist", e)
            0
        }
    }
    
    /**
     * Reload whitelist data from database
     */
    suspend fun reload(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            loadWhitelistStatus()
            loadCacheFromDatabase()
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error reloading whitelist data", e)
            false
        }
    }
    
    /**
     * Check if cache is still valid
     */
    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - lastCacheUpdate < cacheDuration
    }
    
    /**
     * Get paginated list of whitelisted players
     * Returns a pair of (entries, totalPages)
     */
    suspend fun getWhitelistedPlayers(page: Int, pageSize: Int): Pair<List<WhitelistEntry>, Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized || connection == null) {
                return@withContext Pair(emptyList(), 0)
            }
            
            // Get total count first
            val totalCountSql = "SELECT COUNT(*) FROM whitelist WHERE active = 1"
            val totalCount = connection!!.use { conn ->
                conn.prepareStatement(totalCountSql).use { stmt ->
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
            
            val totalPages = if (totalCount > 0) {
                (totalCount + pageSize - 1) / pageSize // Ceiling division
            } else {
                0
            }
            
            if (totalCount == 0) {
                return@withContext Pair(emptyList(), 0)
            }
            
            // Get paginated entries
            val offset = (page - 1) * pageSize
            val sql = """
                SELECT uuid, username, added_by, added_at 
                FROM whitelist 
                WHERE active = 1 
                ORDER BY added_at DESC 
                LIMIT ? OFFSET ?
            """.trimIndent()
            
            val entries = connection!!.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, pageSize)
                    stmt.setInt(2, offset)
                    val rs = stmt.executeQuery()
                    
                    val result = mutableListOf<WhitelistEntry>()
                    while (rs.next()) {
                        result.add(
                            WhitelistEntry(
                                uuid = UUID.fromString(rs.getString("uuid")),
                                username = rs.getString("username"),
                                addedBy = rs.getString("added_by"),
                                addedAt = rs.getLong("added_at"),
                                active = true
                            )
                        )
                    }
                    result
                }
            }
            
            Pair(entries, totalPages)
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error getting whitelisted players", e)
            Pair(emptyList(), 0)
        }
    }
    
    /**
     * Cleanup resources
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        try {
            whitelistCache.clear()
            usernameCache.clear()
            connection?.close()
            isInitialized = false
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error during whitelist cleanup", e)
        }
    }
    
    /**
     * Check if manager is initialized
     */
    fun isInitialized(): Boolean = isInitialized
}