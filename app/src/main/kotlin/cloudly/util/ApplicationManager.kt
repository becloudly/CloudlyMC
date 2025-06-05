/*
 * Cloudly - Application Manager
 * 
 * Manages whitelist applications including creation, review, and database operations.
 * Provides high-performance database operations with caching and async support.
 * All methods are null-safe and handle errors gracefully.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.sql.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * ApplicationManager handles all whitelist application operations
 * Features caching, async operations, and full null-safety
 */
object ApplicationManager {
    
    private lateinit var plugin: CloudlyPlugin
    private var isInitialized = false
    
    // Cache for application data
    private val applicationCache = ConcurrentHashMap<UUID, WhitelistApplication>()
    private val pendingApplicationsCache = mutableListOf<WhitelistApplication>()
    private var lastCacheUpdate = 0L
    private var cacheDuration = 5L * 60 * 1000 // 5 minutes for applications
    
    /**
     * Data class for whitelist applications
     */
    data class WhitelistApplication(
        val id: Int = 0,
        val uuid: UUID,
        val username: String,
        val reason: String,
        val appliedAt: Long,
        val status: ApplicationStatus = ApplicationStatus.PENDING,
        val reviewedBy: String? = null,
        val reviewedAt: Long? = null,
        val reviewReason: String? = null
    )
    
    /**
     * Application status enum
     */
    enum class ApplicationStatus {
        PENDING, APPROVED, DENIED
    }
    
    /**
     * Initialize the application manager
     */
    suspend fun initialize(pluginInstance: CloudlyPlugin): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            plugin = pluginInstance
            
            // Create application table if it doesn't exist
            if (!createApplicationTable()) {
                plugin.logger.severe("Failed to create application table")
                return@withContext false
            }
            
            // Load pending applications cache
            loadPendingApplicationsCache()
            
            isInitialized = true
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error initializing ApplicationManager", e)
            false
        }
    }
    
    /**
     * Create application table
     */    private suspend fun createApplicationTable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext false
            
            val createTableSQL = """
                CREATE TABLE IF NOT EXISTS whitelist_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    username TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    applied_at INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    reviewed_by TEXT,
                    reviewed_at INTEGER,
                    review_reason TEXT,
                    UNIQUE(uuid)
                )
            """.trimIndent()
              connection.createStatement().use { statement ->
                statement.execute(createTableSQL)
            }
            
            // Create index for faster queries
            val createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_applications_status ON whitelist_applications(status)"
            connection.createStatement().use { statement ->
                statement.execute(createIndexSQL)
            }
            
            connection.close()
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error creating application table", e)
            false
        }
    }
    
    /**
     * Submit a new whitelist application
     */
    suspend fun submitApplication(player: Player, reason: String): ApplicationResult = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if player already has a pending application
            if (hasExistingApplication(player.uniqueId)) {
                return@withContext ApplicationResult.ALREADY_EXISTS
            }
            
            // Check if player is already whitelisted
            if (WhitelistManager.isPlayerWhitelisted(player.uniqueId)) {
                return@withContext ApplicationResult.ALREADY_WHITELISTED
            }
              val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext ApplicationResult.ERROR
            
            val insertSQL = """
                INSERT INTO whitelist_applications 
                (uuid, username, reason, applied_at, status) 
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            
            connection.prepareStatement(insertSQL).use { statement ->
                statement.setString(1, player.uniqueId.toString())
                statement.setString(2, player.name)
                statement.setString(3, reason)
                statement.setLong(4, System.currentTimeMillis())
                statement.setString(5, ApplicationStatus.PENDING.name)
                
                statement.executeUpdate()
            }
            
            connection.close()
            
            // Update cache
            loadPendingApplicationsCache()
            
            // Notify admins
            notifyAdminsOfNewApplication(player.name)
            
            ApplicationResult.SUCCESS
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error submitting application", e)
            ApplicationResult.ERROR
        }
    }
    
    /**
     * Check if player has an existing application
     */    suspend fun hasExistingApplication(uuid: UUID): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext false
            
            val selectSQL = "SELECT COUNT(*) FROM whitelist_applications WHERE uuid = ? AND status = 'PENDING'"
            
            connection.prepareStatement(selectSQL).use { statement ->
                statement.setString(1, uuid.toString())
                
                val resultSet = statement.executeQuery()
                val count = if (resultSet.next()) resultSet.getInt(1) else 0
                
                connection.close()
                count > 0
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error checking existing application", e)
            false
        }
    }
    
    /**
     * Get player's application status
     */    suspend fun getApplicationStatus(uuid: UUID): ApplicationStatus? = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext null
            
            val selectSQL = "SELECT status FROM whitelist_applications WHERE uuid = ? ORDER BY applied_at DESC LIMIT 1"
            
            connection.prepareStatement(selectSQL).use { statement ->
                statement.setString(1, uuid.toString())
                
                val resultSet = statement.executeQuery()
                val status = if (resultSet.next()) {
                    ApplicationStatus.valueOf(resultSet.getString("status"))
                } else null
                
                connection.close()
                status
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error getting application status", e)
            null
        }
    }
    
    /**
     * Get all pending applications
     */
    suspend fun getPendingApplications(): List<WhitelistApplication> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Return cached data if recent
            if (System.currentTimeMillis() - lastCacheUpdate < cacheDuration) {
                return@withContext pendingApplicationsCache.toList()
            }
            
            loadPendingApplicationsCache()
            pendingApplicationsCache.toList()
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error getting pending applications", e)
            emptyList()
        }
    }
    
    /**
     * Load pending applications cache from database
     */
    private suspend fun loadPendingApplicationsCache() = withContext(Dispatchers.IO) {
        try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext
            
            val selectSQL = """
                SELECT id, uuid, username, reason, applied_at, status, reviewed_by, reviewed_at, review_reason
                FROM whitelist_applications 
                WHERE status = 'PENDING' 
                ORDER BY applied_at ASC
            """.trimIndent()
            
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(selectSQL)
                
                pendingApplicationsCache.clear()
                while (resultSet.next()) {
                    val application = WhitelistApplication(
                        id = resultSet.getInt("id"),
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        reason = resultSet.getString("reason"),
                        appliedAt = resultSet.getLong("applied_at"),
                        status = ApplicationStatus.valueOf(resultSet.getString("status")),
                        reviewedBy = resultSet.getString("reviewed_by"),
                        reviewedAt = resultSet.getLong("reviewed_at").takeIf { it != 0L },
                        reviewReason = resultSet.getString("review_reason")
                    )
                    pendingApplicationsCache.add(application)
                }
            }
            
            connection.close()
            lastCacheUpdate = System.currentTimeMillis()
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error loading pending applications cache", e)
        }
    }
    
    /**
     * Approve an application
     */    suspend fun approveApplication(applicationId: Int, reviewedBy: String, reason: String? = null): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext false
            
            // Get application details first
            val selectSQL = "SELECT uuid, username FROM whitelist_applications WHERE id = ?"
            val (uuid, username) = connection.prepareStatement(selectSQL).use { statement ->
                statement.setInt(1, applicationId)
                val resultSet = statement.executeQuery()
                
                if (resultSet.next()) {
                    UUID.fromString(resultSet.getString("uuid")) to resultSet.getString("username")
                } else {
                    connection.close()
                    return@withContext false
                }
            }
            
            // Update application status
            val updateSQL = """
                UPDATE whitelist_applications 
                SET status = 'APPROVED', reviewed_by = ?, reviewed_at = ?, review_reason = ?
                WHERE id = ?
            """.trimIndent()
            
            connection.prepareStatement(updateSQL).use { statement ->
                statement.setString(1, reviewedBy)
                statement.setLong(2, System.currentTimeMillis())
                statement.setString(3, reason)
                statement.setInt(4, applicationId)
                
                statement.executeUpdate()
            }
            
            connection.close()
            
            // Add player to whitelist
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
            WhitelistManager.addPlayerToWhitelist(offlinePlayer, reviewedBy)
            
            // Update cache
            loadPendingApplicationsCache()
            
            // Unfreeze player if online
            val onlinePlayer = Bukkit.getPlayer(uuid)
            onlinePlayer?.let { PlayerFreezeManager.unfreezePlayer(it) }
            
            // Notify player if online
            onlinePlayer?.let { player ->
                LanguageManager.sendMessage(player, "whitelist.application.approved", reason ?: "")
            }
            
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error approving application", e)
            false
        }
    }
    
    /**
     * Deny an application
     */    suspend fun denyApplication(applicationId: Int, reviewedBy: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = CloudlyUtils.getDatabaseConnection()
                ?: return@withContext false
            
            // Get application details first
            val selectSQL = "SELECT uuid, username FROM whitelist_applications WHERE id = ?"
            val (uuid, username) = connection.prepareStatement(selectSQL).use { statement ->
                statement.setInt(1, applicationId)
                val resultSet = statement.executeQuery()
                
                if (resultSet.next()) {
                    UUID.fromString(resultSet.getString("uuid")) to resultSet.getString("username")
                } else {
                    connection.close()
                    return@withContext false
                }
            }
            
            // Update application status
            val updateSQL = """
                UPDATE whitelist_applications 
                SET status = 'DENIED', reviewed_by = ?, reviewed_at = ?, review_reason = ?
                WHERE id = ?
            """.trimIndent()
            
            connection.prepareStatement(updateSQL).use { statement ->
                statement.setString(1, reviewedBy)
                statement.setLong(2, System.currentTimeMillis())
                statement.setString(3, reason)
                statement.setInt(4, applicationId)
                
                statement.executeUpdate()
            }
            
            connection.close()
            
            // Update cache
            loadPendingApplicationsCache()
            
            // Kick player if online
            val onlinePlayer = Bukkit.getPlayer(uuid)
            onlinePlayer?.let { player ->
                LanguageManager.sendMessage(player, "whitelist.application.denied", reason)
                player.kickPlayer(LanguageManager.getMessage("whitelist.application.denied.kick", reason))
            }
            
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error denying application", e)
            false
        }
    }
    
    /**
     * Notify admins of new application
     */
    private fun notifyAdminsOfNewApplication(playerName: String) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getOnlinePlayers()
                .filter { it.hasPermission("cloudly.whitelist.admin") }
                .forEach { admin ->
                    LanguageManager.sendMessage(admin, "whitelist.application.admin.notification", playerName)
                    admin.sendTitle(
                        LanguageManager.getMessage("whitelist.application.admin.title"),
                        LanguageManager.getMessage("whitelist.application.admin.subtitle", playerName),
                        10, 70, 20
                    )
                    admin.playSound(admin.location, "entity.experience_orb.pickup", 1.0f, 1.0f)
                }
        })
    }
    
    /**
     * Get total pending applications count
     */
    suspend fun getPendingApplicationsCount(): Int = withContext(Dispatchers.IO) {
        return@withContext pendingApplicationsCache.size
    }
    
    /**
     * Application result enum
     */
    enum class ApplicationResult {
        SUCCESS, ALREADY_EXISTS, ALREADY_WHITELISTED, ERROR
    }
}
