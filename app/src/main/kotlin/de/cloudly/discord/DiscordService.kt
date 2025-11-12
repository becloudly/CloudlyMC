package de.cloudly.discord

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.whitelist.WhitelistService
import de.cloudly.whitelist.model.DiscordConnection
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
import java.security.SecureRandom

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
    
    // Rate limiting for Discord API
    private val rateLimiter = Semaphore(5) // Max 5 concurrent requests
    private val lastRequestTime = AtomicLong(0)
    
    // Role verification configuration
    private var requireRole = false
    private var requiredRoleId: String? = null
    private var verificationTimeoutMinutes: Long = 5

    // Track pending Minecraft ↔ Discord verification codes
    private val pendingVerifications = ConcurrentHashMap<UUID, PendingVerification>()
    private val secureRandom = SecureRandom()
    
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
            verificationTimeoutMinutes = configManager.getInt("discord.verification_timeout_minutes", 5).toLong()
            
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
            val debug = configManager.getBoolean("plugin.debug", false)
            val removedUsers = userCache.entries.removeIf { it.value.isExpired(cacheDuration) }
            if (removedUsers && debug) {
                plugin.logger.info("Discord cache cleanup: removed expired entries")
            }

            val now = Instant.now()
            val removedCodes = pendingVerifications.entries.removeIf { it.value.isExpired(now) }
            if (removedCodes && debug) {
                plugin.logger.info("Discord verification cleanup: removed expired codes")
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
     * Reset all verification state for a player, including cooldowns and pending codes.
     */
    fun resetVerificationState(uuid: UUID) {
        playerCooldowns.remove(uuid)
        pendingVerifications.remove(uuid)
    }
    
    /**
     * Start the Discord verification flow by sending a one-time code to the player's Discord account.
     */
    suspend fun requestVerificationCode(
        playerUuid: UUID,
        playerName: String,
        discordInput: String,
        whitelistService: WhitelistService
    ): DiscordCodeRequestResult {
        if (!isEnabled()) {
            plugin.logger.info("Discord verification failed: Service not enabled")
            return DiscordCodeRequestResult.ServiceDisabled
        }

        val debug = configManager.getBoolean("plugin.debug", false)
        if (debug) {
            plugin.logger.info("Player $playerName requested Discord link for '$discordInput'")
        }

        setCooldown(playerUuid)

        val existingPlayer = whitelistService.getPlayer(playerUuid)
        if (existingPlayer?.discordConnection?.verified == true) {
            return DiscordCodeRequestResult.AccountAlreadyLinked
        }

        val existingPending = pendingVerifications[playerUuid]
        val now = Instant.now()
        if (existingPending != null) {
            if (existingPending.isExpired(now)) {
                pendingVerifications.remove(playerUuid)
            } else {
                return DiscordCodeRequestResult.PendingAlreadyActive
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val normalizedInput = discordInput.lowercase()
                val cached = userCache[normalizedInput]
                val discordUser = if (cached != null && !cached.isExpired(cacheDuration)) {
                    if (debug) {
                        plugin.logger.info("Using cached Discord lookup for '$discordInput'")
                    }
                    DiscordUser(
                        id = cached.discordId,
                        username = cached.actualUsername,
                        discriminator = "0"
                    )
                } else {
                    if (debug) {
                        plugin.logger.info("Searching Discord user for '$discordInput'")
                    }
                    searchUserByUsername(discordInput) ?: return@withContext DiscordCodeRequestResult.UserNotFound
                }

                val memberJson = fetchGuildMember(discordUser.id)
                    ?: return@withContext DiscordCodeRequestResult.NotServerMember

                if (requireRole && !requiredRoleId.isNullOrBlank()) {
                    val roles = memberJson.optJSONArray("roles")
                    val hasRole = roles != null && (0 until roles.length()).any { roles.getString(it) == requiredRoleId }
                    if (!hasRole) {
                        return@withContext DiscordCodeRequestResult.MissingRole
                    }
                }

                val existingLink = whitelistService.findPlayerByDiscordId(discordUser.id)
                if (existingLink != null && existingLink.uuid != playerUuid) {
                    return@withContext DiscordCodeRequestResult.AccountInUse
                }

                userCache[normalizedInput] = CachedDiscordUser(
                    discordId = discordUser.id,
                    actualUsername = discordUser.username,
                    isValid = true,
                    cacheTime = Instant.now()
                )

                val code = generateVerificationCode()
                val expiresAt = Instant.now().plusSeconds(verificationTimeoutMinutes * 60)
                val dmMessage = Messages.Commands.Discord.DM_CONTENT.format(code)

                val dmError = sendVerificationCode(discordUser.id, dmMessage)
                if (dmError != null) {
                    return@withContext DiscordCodeRequestResult.DmFailed(dmError)
                }

                pendingVerifications[playerUuid] = PendingVerification(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    discordId = discordUser.id,
                    discordUsername = discordUser.username,
                    code = code,
                    createdAt = Instant.now(),
                    expiresAt = expiresAt
                )

                if (debug) {
                    plugin.logger.info("Verification code dispatched to Discord user ${discordUser.username}")
                }

                DiscordCodeRequestResult.CodeSent(discordUser.id, discordUser.username)
            } catch (e: IOException) {
                plugin.logger.log(Level.WARNING, "Discord API connection failed for user: $discordInput", e)
                DiscordCodeRequestResult.ApiError("Connection failed: ${e.message}")
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Discord verification failed for user: $discordInput", e)
                DiscordCodeRequestResult.ApiError("Verification failed: ${e.message}")
            }
        }
    }

    /**
     * Validate a verification code entered by the player.
     */
    fun validateVerificationCode(
        playerUuid: UUID,
        enteredCode: String,
        whitelistService: WhitelistService
    ): DiscordCodeValidationResult {
        val pending = pendingVerifications[playerUuid] ?: return DiscordCodeValidationResult.NoPending

        val now = Instant.now()
        if (pending.isExpired(now)) {
            pendingVerifications.remove(playerUuid)
            return DiscordCodeValidationResult.CodeExpired
        }

        val sanitizedCode = enteredCode.trim()
        if (!pending.code.equals(sanitizedCode, ignoreCase = false)) {
            return DiscordCodeValidationResult.InvalidCode
        }

        val existingLink = whitelistService.findPlayerByDiscordId(pending.discordId)
        if (existingLink != null && existingLink.uuid != playerUuid) {
            pendingVerifications.remove(playerUuid)
            return DiscordCodeValidationResult.AccountInUse
        }

        pendingVerifications.remove(playerUuid)

        val connection = DiscordConnection(
            discordId = pending.discordId,
            discordUsername = pending.discordUsername,
            verified = true,
            connectedAt = now,
            verifiedAt = now
        )

        return DiscordCodeValidationResult.Success(connection)
    }

    /**
     * Check whether a player currently has an active verification code.
     */
    fun hasPendingVerification(uuid: UUID): Boolean {
        val pending = pendingVerifications[uuid]
        return pending != null && !pending.isExpired(Instant.now())
    }

    /**
     * Evaluate whether a previously linked Discord account is still valid.
     */
    suspend fun evaluateLinkedAccount(connection: DiscordConnection): DiscordLinkHealth {
        if (!isEnabled()) {
            return DiscordLinkHealth.ServiceDisabled
        }

        return withContext(Dispatchers.IO) {
            try {
                val memberJson = fetchGuildMember(connection.discordId)
                if (memberJson == null) {
                    return@withContext DiscordLinkHealth.NotServerMember
                }

                if (requireRole && !requiredRoleId.isNullOrBlank()) {
                    val roles = memberJson.optJSONArray("roles")
                    val hasRole = roles != null && (0 until roles.length()).any { roles.getString(it) == requiredRoleId }
                    if (!hasRole) {
                        return@withContext DiscordLinkHealth.MissingRole
                    }
                }

                DiscordLinkHealth.Valid
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Failed to validate Discord link for ${connection.discordUsername}", e)
                DiscordLinkHealth.ApiError(e.message ?: "Unknown error")
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
     * Retrieve the guild member JSON for the given Discord user ID, or null if not found.
     */
    private suspend fun fetchGuildMember(userId: String): JSONObject? {
        val client = httpClient ?: return null
        val token = botToken ?: return null
        val guildId = serverId ?: return null

        val request = Request.Builder()
            .url("https://discord.com/api/v10/guilds/$guildId/members/$userId")
            .header("Authorization", "Bot $token")
            .header("User-Agent", "CloudlyMC/1.0")
            .build()

        return withContext(Dispatchers.IO) {
            rateLimit {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use null
                    }
                    val responseBody = response.body?.string() ?: return@use null
                    JSONObject(responseBody)
                }
            }
        }
    }

    /**
     * Send the verification code to the Discord user via direct message.
     * @return null if successful, otherwise a human-readable error string.
     */
    private suspend fun sendVerificationCode(discordId: String, message: String): String? {
        val client = httpClient ?: return "HTTP client unavailable"
        val token = botToken ?: return "Bot token not configured"

        return withContext(Dispatchers.IO) {
            var channelId: String? = null

            val createError = rateLimit<String?> {
                val body = """{"recipient_id":"$discordId"}"""
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://discord.com/api/v10/users/@me/channels")
                    .header("Authorization", "Bot $token")
                    .header("User-Agent", "CloudlyMC/1.0")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use "Failed to open DM channel: ${response.code} ${response.message}"
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrBlank()) {
                        return@use "Empty response when creating DM channel"
                    }
                    val json = JSONObject(responseBody)
                    channelId = json.optString("id")
                    if (channelId.isNullOrBlank()) {
                        return@use "Discord response missing channel id"
                    }
                    null
                }
            }

            if (createError != null) {
                return@withContext createError
            }

            val payload = JSONObject().put("content", message).toString()

            val sendError = rateLimit<String?> {
                val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://discord.com/api/v10/channels/${channelId}/messages")
                    .header("Authorization", "Bot $token")
                    .header("User-Agent", "CloudlyMC/1.0")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use "Failed to send DM message: ${response.code} ${response.message}"
                    }
                    null
                }
            }

            sendError
        }
    }

    private fun generateVerificationCode(): String {
        val number = secureRandom.nextInt(1_000_000)
        return String.format("%06d", number)
    }
    
    /**
     * Shutdown the Discord service and clean up resources.
     */
    fun shutdown() {
        try {
            // Cancel the cache cleanup task
            cacheCleanupTask?.cancel()
            cacheCleanupTask = null
            
            httpClient?.dispatcher?.executorService?.shutdown()
            userCache.clear()
            pendingVerifications.clear()
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

private data class PendingVerification(
    val playerUuid: UUID,
    val playerName: String,
    val discordId: String,
    val discordUsername: String,
    val code: String,
    val createdAt: Instant,
    val expiresAt: Instant
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)
}

sealed class DiscordCodeRequestResult {
    data class CodeSent(val discordId: String, val discordUsername: String) : DiscordCodeRequestResult()
    object ServiceDisabled : DiscordCodeRequestResult()
    object UserNotFound : DiscordCodeRequestResult()
    object NotServerMember : DiscordCodeRequestResult()
    object MissingRole : DiscordCodeRequestResult()
    object AccountAlreadyLinked : DiscordCodeRequestResult()
    object AccountInUse : DiscordCodeRequestResult()
    object PendingAlreadyActive : DiscordCodeRequestResult()
    data class DmFailed(val reason: String) : DiscordCodeRequestResult()
    data class ApiError(val message: String) : DiscordCodeRequestResult()
}

sealed class DiscordCodeValidationResult {
    data class Success(val connection: DiscordConnection) : DiscordCodeValidationResult()
    object InvalidCode : DiscordCodeValidationResult()
    object CodeExpired : DiscordCodeValidationResult()
    object NoPending : DiscordCodeValidationResult()
    object AccountInUse : DiscordCodeValidationResult()
}

sealed class DiscordLinkHealth {
    object Valid : DiscordLinkHealth()
    object NotServerMember : DiscordLinkHealth()
    object MissingRole : DiscordLinkHealth()
    object ServiceDisabled : DiscordLinkHealth()
    data class ApiError(val message: String) : DiscordLinkHealth()
}
