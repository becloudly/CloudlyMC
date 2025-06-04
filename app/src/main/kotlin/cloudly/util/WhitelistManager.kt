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
            plugin.logger.info("WhitelistManager initialized successfully")
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
                    val dbFile = config.getString("database.sqlite.file", "cloudly.db")
                    val dbPath = plugin.dataFolder.resolve(dbFile)
                    DriverManager.getConnection("jdbc:sqlite:$dbPath")
                }
                "mysql" -> {
                    val host = config.getString("database.mysql.host", "localhost")
                    val port = config.getInt("database.mysql.port", 3306)
                    val database = config.getString("database.mysql.database", "cloudly")
                    val username = config.getString("database.mysql.username", "username")
                    val password = config.getString("database.mysql.password", "password")
                    
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
            val dbType = plugin.config.getString("database.type", "sqlite")?.lowercase()
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
            if (isPlayerWhitelisted(player.uniqueId)) {                return@withContext false
            }
            
            val dbType = plugin.config.getString("database.type", "sqlite")?.lowercase()
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
            
            val query = "UPDATE whitelist_users SET active = 0 WHERE uuid = ?"
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setString(1, uuid.toString())
                val rows = pstmt.executeUpdate()
                
                if (rows > 0) {
                    // Remove from cache
                    val entry = whitelistCache.remove(uuid)
                    entry?.let { usernameCache.remove(it.username.lowercase()) }
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
     * Get paginated list of whitelisted players
     */
    suspend fun getWhitelistedPlayers(page: Int, pageSize: Int = 10): Pair<List<WhitelistEntry>, Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = connection ?: return@withContext Pair(emptyList(), 0)
            
            // Get total count
            val countQuery = "SELECT COUNT(*) FROM whitelist_users WHERE active = 1"
            val totalCount = conn.prepareStatement(countQuery).use { pstmt ->
                val rs = pstmt.executeQuery()
                if (rs.next()) rs.getInt(1) else 0
            }
            
            // Get paginated results
            val offset = (page - 1) * pageSize
            val query = """
                SELECT uuid, username, added_by, added_at 
                FROM whitelist_users 
                WHERE active = 1 
                ORDER BY added_at DESC 
                LIMIT ? OFFSET ?
            """.trimIndent()
            
            val entries = mutableListOf<WhitelistEntry>()
            conn.prepareStatement(query).use { pstmt ->
                pstmt.setInt(1, pageSize)
                pstmt.setInt(2, offset)
                
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val username = rs.getString("username")
                    val addedBy = rs.getString("added_by")
                    val addedAt = rs.getLong("added_at")
                    
                    entries.add(WhitelistEntry(uuid, username, addedBy, addedAt))
                }
            }
            
            val totalPages = (totalCount + pageSize - 1) / pageSize
            Pair(entries, totalPages)
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error getting whitelisted players", e)
            Pair(emptyList(), 0)
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