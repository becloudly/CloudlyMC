package de.cloudly.discord

import de.cloudly.CloudlyPaper
import de.cloudly.whitelist.model.DiscordConnection
import kotlinx.coroutines.*
import okhttp3.*
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

/**
 * Service for Discord API integration and user verification.
 * Handles Discord bot authentication and server membership verification.
 */
class DiscordService(private val plugin: CloudlyPaper) {
    
    private val configManager = plugin.getConfigManager()
    
    private var httpClient: OkHttpClient? = null
    private var botToken: String? = null
    private var serverId: String? = null
    private var apiTimeout: Long = 10
    private var cacheDuration: Long = 30
    
    // Cache for Discord user lookups to reduce API calls
    private val userCache = ConcurrentHashMap<String, CachedDiscordUser>()
    
    // Player-side rate limiting (30 second cooldown per player)
    private val playerCooldowns = ConcurrentHashMap<UUID, Long>()
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Rate limiting for Discord API
    private val rateLimiter = Semaphore(5) // Max 5 concurrent requests
    private val lastRequestTime = AtomicLong(0)
    
    // Role verification configuration
    private var requireRole = false
    private var requiredRoleId: String? = null
    
    /**
     * Rate limit Discord API calls to prevent hitting rate limits.
     * Ensures maximum 5 concurrent requests and minimum 200ms between requests.
     */
    private suspend fun <T> rateLimit(block: suspend () -> T): T {
        rateLimiter.acquire()
        try {
            // Ensure minimum 200ms between requests
            val timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime.get()
            if (timeSinceLastRequest < 200) {
                delay(200 - timeSinceLastRequest)
            }
            lastRequestTime.set(System.currentTimeMillis())
            return block()
        } finally {
            rateLimiter.release()
        }
    }
    // Task reference for cache cleanup
    private var cacheCleanupTask: BukkitTask? = null
    
    /**
     * Initialize the Discord service with configuration.
     */
    fun initialize(): Boolean {
        return try {
            val debugMode = configManager.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Initializing Discord service...")
            }
            
            // Load Discord configuration
            val discordEnabled = configManager.getBoolean("discord.enabled", false)
            if (debugMode) {
                plugin.logger.info("Discord enabled in config: $discordEnabled")
            }
            
            if (!discordEnabled) {
                if (debugMode) {
                    plugin.logger.info("Discord integration is disabled in configuration")
                }
                return true
            }
            
            botToken = configManager.getString("discord.bot_token", "")
            serverId = configManager.getString("discord.server_id", "")
            apiTimeout = configManager.getInt("discord.api_timeout", 10).toLong()
            cacheDuration = configManager.getInt("discord.cache_duration", 30).toLong()
            
            // Load role verification settings
            requireRole = configManager.getBoolean("discord.require_role", false)
            requiredRoleId = configManager.getString("discord.required_role_id", "")
            
            plugin.logger.info("Discord config loaded successfully")
            
            if (botToken.isNullOrBlank() || botToken == "YOUR_BOT_TOKEN_HERE") {
                plugin.logger.warning("Discord bot token is not configured. Discord features will be disabled.")
                return false
            }
            
            if (serverId.isNullOrBlank() || serverId == "YOUR_SERVER_ID_HERE") {
                plugin.logger.warning("Discord server ID is not configured. Discord features will be disabled.")
                return false
            }
            
            // Initialize HTTP client
            plugin.logger.info("Creating OkHttp client...")
            httpClient = OkHttpClient.Builder()
                .connectTimeout(apiTimeout, TimeUnit.SECONDS)
                .readTimeout(apiTimeout, TimeUnit.SECONDS)
                .writeTimeout(apiTimeout, TimeUnit.SECONDS)
                .build()
            
            // Start periodic cache cleanup to prevent memory leak
            startCacheCleanup()
            
            plugin.logger.info("Discord service initialized successfully")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize Discord service", e)
            false
        }
    }
    
    /**
     * Start periodic cache cleanup task to remove expired entries.
     * This prevents memory leaks from expired cache entries accumulating.
     */
    private fun startCacheCleanup() {
        cacheCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            val removed = userCache.entries.removeIf { it.value.isExpired(cacheDuration) }
            if (removed && configManager.getBoolean("plugin.debug", false)) {
                plugin.logger.info("Discord cache cleanup: removed expired entries")
            }
        }, 20 * 60, 20 * 60) // Every minute (1200 ticks)
    }
    
    /**
     * Check if Discord integration is properly configured and enabled.
     */
    fun isEnabled(): Boolean {
        return configManager.getBoolean("discord.enabled", false) && 
               !botToken.isNullOrBlank() && 
               !serverId.isNullOrBlank() &&
               httpClient != null
    }
    
    /**
     * Check if player is on cooldown for Discord verification.
     * @param uuid The player's UUID
     * @return true if on cooldown, false otherwise
     */
    fun isOnCooldown(uuid: UUID): Boolean {
        val lastAttempt = playerCooldowns[uuid] ?: return false
        val cooldownMs = 30_000L // 30 seconds
        return System.currentTimeMillis() - lastAttempt < cooldownMs
    }
    
    /**
     * Get remaining cooldown time in seconds.
     */
    fun getRemainingCooldown(uuid: UUID): Long {
        val lastAttempt = playerCooldowns[uuid] ?: return 0
        val cooldownMs = 30_000L
        val elapsed = System.currentTimeMillis() - lastAttempt
        val remaining = cooldownMs - elapsed
        return if (remaining > 0) remaining / 1000 else 0
    }
    
    /**
     * Set cooldown for player.
     */
    private fun setCooldown(uuid: UUID) {
        playerCooldowns[uuid] = System.currentTimeMillis()
    }
    
    /**
     * Verify if a Discord user exists and is a member of the configured server.
     * This method is async and should be called from a coroutine or background thread.
     * @param uuid The player's UUID (for cooldown tracking)
     * @param discordUsername The Discord username to verify
     */
    suspend fun verifyDiscordUser(uuid: UUID, discordUsername: String): DiscordVerificationResult {
        if (!isEnabled()) {
            plugin.logger.info("Discord verification failed: Service not enabled")
            return DiscordVerificationResult.ServiceDisabled
        }
        
        // Set cooldown
        setCooldown(uuid)
        
        return withContext(Dispatchers.IO) {
            try {
                plugin.logger.info("Starting Discord verification for user: $discordUsername")
                
                // Check cache first
                val cached = userCache[discordUsername.lowercase()]
                if (cached != null && !cached.isExpired(cacheDuration)) {
                    plugin.logger.info("Using cached result for user: $discordUsername")
                    return@withContext if (cached.isValid) {
                        DiscordVerificationResult.Success(cached.discordId, cached.actualUsername)
                    } else {
                        DiscordVerificationResult.UserNotFound
                    }
                }
                
                // Search for user by username
                plugin.logger.info("Searching Discord user: $discordUsername")
                val discordUser = searchUserByUsername(discordUsername)
                if (discordUser == null) {
                    plugin.logger.info("Discord user not found: $discordUsername")
                    return@withContext DiscordVerificationResult.UserNotFound
                }
                
                plugin.logger.info("Found Discord user: ${discordUser.username} (${discordUser.id})")
                
                // Check if user is member of the server
                plugin.logger.info("Checking server membership for user: ${discordUser.id}")
                val isMember = checkServerMembership(discordUser.id)
                if (!isMember) {
                    plugin.logger.info("Discord user is not a member of the server: ${discordUser.username}")
                    return@withContext DiscordVerificationResult.NotServerMember
                }
                
                // Check role if required
                if (requireRole && !requiredRoleId.isNullOrBlank()) {
                    plugin.logger.info("Checking role membership for user: ${discordUser.id}")
                    val hasRole = checkRoleMembership(discordUser.id, requiredRoleId!!)
                    if (!hasRole) {
                        plugin.logger.info("Discord user does not have required role: ${discordUser.username}")
                        return@withContext DiscordVerificationResult.MissingRole
                    }
                }
                
                plugin.logger.info("Discord verification successful for user: ${discordUser.username}")
                
                // Cache the result
                userCache[discordUsername.lowercase()] = CachedDiscordUser(
                    discordId = discordUser.id,
                    actualUsername = discordUser.username,
                    isValid = true,
                    cacheTime = Instant.now()
                )
                
                DiscordVerificationResult.Success(discordUser.id, discordUser.username)
            } catch (e: IOException) {
                plugin.logger.log(Level.WARNING, "Discord API connection failed for user: $discordUsername", e)
                DiscordVerificationResult.ApiError("Connection failed: ${e.message}")
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Discord verification failed for user: $discordUsername", e)
                DiscordVerificationResult.ApiError("Verification failed: ${e.message}")
            }
        }
    }
    
    /**
     * Search for a Discord user by username using the Discord API.
     */
    private suspend fun searchUserByUsername(username: String): DiscordUser? {
        val client = httpClient ?: return null
        val token = botToken ?: return null
        val guildId = serverId ?: return null
        
        // Use the search guild members endpoint to find users by username
        val url = "https://discord.com/api/v10/guilds/$guildId/members/search?query=${username}&limit=1"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bot $token")
            .header("User-Agent", "CloudlyMC/1.0")
            .build()
        
        return withContext(Dispatchers.IO) {
            rateLimit {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        plugin.logger.warning("Discord API error: ${response.code} ${response.message}")
                        return@use null
                    }
                    
                    val responseBody = response.body?.string() ?: return@use null
                    val jsonArray = org.json.JSONArray(responseBody)
                    
                    if (jsonArray.length() == 0) {
                        return@use null
                    }
                    
                    val member = jsonArray.getJSONObject(0)
                    val user = member.getJSONObject("user")
                    
                    DiscordUser(
                        id = user.getString("id"),
                        username = user.getString("username"),
                        discriminator = user.optString("discriminator", "0")
                    )
                }
            }
        }
    }
    
    /**
     * Check if a Discord user is a member of the configured server.
     */
    private suspend fun checkServerMembership(userId: String): Boolean {
        val client = httpClient ?: return false
        val token = botToken ?: return false
        val guildId = serverId ?: return false
        
        val url = "https://discord.com/api/v10/guilds/$guildId/members/$userId"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bot $token")
            .header("User-Agent", "CloudlyMC/1.0")
            .build()
        
        return withContext(Dispatchers.IO) {
            rateLimit {
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            }
        }
    }
    
    /**
     * Check if a Discord user has a specific role.
     */
    private suspend fun checkRoleMembership(userId: String, roleId: String): Boolean {
        val client = httpClient ?: return false
        val token = botToken ?: return false
        val guildId = serverId ?: return false
        
        val url = "https://discord.com/api/v10/guilds/$guildId/members/$userId"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bot $token")
            .header("User-Agent", "CloudlyMC/1.0")
            .build()
        
        return withContext(Dispatchers.IO) {
            rateLimit {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use false
                    }
                    
                    val responseBody = response.body?.string() ?: return@use false
                    val member = JSONObject(responseBody)
                    val roles = member.getJSONArray("roles")
                    
                    // Check if required role is in the roles list
                    for (i in 0 until roles.length()) {
                        if (roles.getString(i) == roleId) {
                            return@use true
                        }
                    }
                    
                    false
                }
            }
        }
    }
    
    /**
     * Create a Discord connection object from verification result.
     */
    fun createDiscordConnection(result: DiscordVerificationResult.Success): DiscordConnection {
        return DiscordConnection(
            discordId = result.discordId,
            discordUsername = result.username,
            verified = true,
            connectedAt = Instant.now(),
            verifiedAt = Instant.now()
        )
    }
    
    /**
     * Shutdown the Discord service and clean up resources.
     */
    fun shutdown() {
        try {
            // Cancel the cache cleanup task
            cacheCleanupTask?.cancel()
            cacheCleanupTask = null
            
            coroutineScope.cancel()
            httpClient?.dispatcher?.executorService?.shutdown()
            userCache.clear()
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error shutting down Discord service", e)
        }
    }
    
    /**
     * Clear the user cache (useful for testing or manual refresh).
     */
    fun clearCache() {
        userCache.clear()
    }
}

/**
 * Result of Discord user verification.
 */
sealed class DiscordVerificationResult {
    object ServiceDisabled : DiscordVerificationResult()
    object UserNotFound : DiscordVerificationResult()
    object NotServerMember : DiscordVerificationResult()
    object MissingRole : DiscordVerificationResult()
    data class Success(val discordId: String, val username: String) : DiscordVerificationResult()
    data class ApiError(val message: String) : DiscordVerificationResult()
}

/**
 * Internal representation of a Discord user.
 */
private data class DiscordUser(
    val id: String,
    val username: String,
    val discriminator: String
)

/**
 * Cached Discord user information.
 */
private data class CachedDiscordUser(
    val discordId: String,
    val actualUsername: String,
    val isValid: Boolean,
    val cacheTime: Instant
) {
    fun isExpired(cacheDurationMinutes: Long): Boolean {
        return Instant.now().isAfter(cacheTime.plusSeconds(cacheDurationMinutes * 60))
    }
}
