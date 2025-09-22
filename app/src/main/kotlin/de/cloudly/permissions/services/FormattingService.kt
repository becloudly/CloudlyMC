package de.cloudly.permissions.services

import de.cloudly.permissions.PermissionManager
import de.cloudly.permissions.utils.ColorUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Service for handling chat, tablist, and nametag formatting.
 * Applies prefixes, suffixes, and custom formats from permission groups.
 */
class FormattingService(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager
) {
    
    // Cache formatted strings to avoid repeated formatting
    private val formatCache = ConcurrentHashMap<String, CachedFormat>()
    private val cacheTimeout = 60000L // 1 minute cache
    
    private var initialized = false
    private var chatEnabled = true
    private var tablistEnabled = true
    private var nametagEnabled = true
    private var colorCodesEnabled = true
    private var hexColorsEnabled = true
    
    /**
     * Initialize the formatting service.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Use default formatting settings (no longer configurable)
            chatEnabled = true
            tablistEnabled = true
            nametagEnabled = true
            colorCodesEnabled = true
            hexColorsEnabled = true
            
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Formatting service initialized successfully with default settings")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize formatting service", e)
            false
        }
    }
    
    /**
     * Format a chat message for a player.
     * 
     * @param player The player sending the message
     * @param message The original message
     * @return The formatted message
     */
    fun formatChatMessage(player: Player, message: String): String? {
        if (!initialized || !chatEnabled || !permissionManager.isEnabled()) {
            return null // Return null to use default formatting
        }
        
        try {
            val cacheKey = "chat:${player.uniqueId}:${message.hashCode()}"
            val cached = formatCache[cacheKey]
            if (cached != null && !cached.isExpired()) {
                return cached.formattedText
            }
            
            // Get player's formatting information
            val prefix = permissionManager.getPlayerPrefix(player)
            val suffix = permissionManager.getPlayerSuffix(player)
            val chatFormat = permissionManager.getPermissionResolver().getPlayerChatFormat(player.uniqueId)
            
            val formattedMessage = if (chatFormat != null) {
                // Use custom chat format
                formatWithTemplate(chatFormat, player, message, prefix, suffix)
            } else {
                // Use default prefix/suffix formatting
                ColorUtils.formatChatMessage(
                    prefix = prefix,
                    message = message,
                    suffix = suffix,
                    playerName = player.displayName,
                    supportHex = hexColorsEnabled,
                    supportLegacy = colorCodesEnabled
                )
            }
            
            // Cache the result
            formatCache[cacheKey] = CachedFormat(formattedMessage, System.currentTimeMillis() + cacheTimeout)
            
            return formattedMessage
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error formatting chat message for ${player.name}", e)
            return null
        }
    }
    
    /**
     * Format a tablist entry for a player.
     * 
     * @param player The player
     * @return The formatted tablist entry
     */
    fun formatTablistEntry(player: Player): String? {
        if (!initialized || !tablistEnabled || !permissionManager.isEnabled()) {
            return null
        }
        
        try {
            val cacheKey = "tablist:${player.uniqueId}"
            val cached = formatCache[cacheKey]
            if (cached != null && !cached.isExpired()) {
                return cached.formattedText
            }
            
            // Get player's formatting information
            val prefix = permissionManager.getPlayerPrefix(player)
            val suffix = permissionManager.getPlayerSuffix(player)
            val tablistFormat = permissionManager.getPermissionResolver().getPlayerTablistFormat(player.uniqueId)
            
            val formattedEntry = if (tablistFormat != null) {
                // Use custom tablist format
                formatWithTemplate(tablistFormat, player, "", prefix, suffix)
            } else {
                // Use default prefix/suffix formatting
                ColorUtils.formatTablistEntry(
                    prefix = prefix,
                    playerName = player.displayName,
                    suffix = suffix,
                    supportHex = hexColorsEnabled,
                    supportLegacy = colorCodesEnabled
                )
            }
            
            // Cache the result
            formatCache[cacheKey] = CachedFormat(formattedEntry, System.currentTimeMillis() + cacheTimeout)
            
            return formattedEntry
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error formatting tablist entry for ${player.name}", e)
            return null
        }
    }
    
    /**
     * Format a nametag for a player.
     * 
     * @param player The player
     * @return The formatted nametag
     */
    fun formatNametag(player: Player): String? {
        if (!initialized || !nametagEnabled || !permissionManager.isEnabled()) {
            return null
        }
        
        try {
            val cacheKey = "nametag:${player.uniqueId}"
            val cached = formatCache[cacheKey]
            if (cached != null && !cached.isExpired()) {
                return cached.formattedText
            }
            
            // Get player's formatting information
            val prefix = permissionManager.getPlayerPrefix(player)
            val suffix = permissionManager.getPlayerSuffix(player)
            val nametagFormat = permissionManager.getPermissionResolver().getPlayerNametagFormat(player.uniqueId)
            
            val formattedNametag = if (nametagFormat != null) {
                // Use custom nametag format
                formatWithTemplate(nametagFormat, player, "", prefix, suffix)
            } else {
                // Use default prefix/suffix formatting
                ColorUtils.formatNametag(
                    prefix = prefix,
                    playerName = player.displayName,
                    suffix = suffix,
                    maxLength = 16, // Minecraft nametag limit
                    supportHex = hexColorsEnabled,
                    supportLegacy = colorCodesEnabled
                )
            }
            
            // Cache the result
            formatCache[cacheKey] = CachedFormat(formattedNametag, System.currentTimeMillis() + cacheTimeout)
            
            return formattedNametag
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error formatting nametag for ${player.name}", e)
            return null
        }
    }
    
    /**
     * Format text using a template with placeholders.
     * 
     * @param template The format template
     * @param player The player
     * @param message The message (for chat formatting)
     * @param prefix The player's prefix
     * @param suffix The player's suffix
     * @return The formatted text
     */
    private fun formatWithTemplate(
        template: String,
        player: Player,
        message: String,
        prefix: String?,
        suffix: String?
    ): String {
        val placeholders = mapOf(
            "player" to player.name,
            "displayname" to player.displayName,
            "prefix" to (prefix ?: ""),
            "suffix" to (suffix ?: ""),
            "message" to message,
            "world" to (player.world?.name ?: "unknown"),
            "group" to (permissionManager.getPlayerPrimaryGroup(player) ?: "base"),
            "server" to plugin.server.name
        )
        
        return ColorUtils.applyTemplate(
            template = template,
            placeholders = placeholders,
            supportHex = hexColorsEnabled,
            supportLegacy = colorCodesEnabled
        )
    }
    
    /**
     * Clear formatting cache for a specific player.
     * Should be called when a player's permissions change.
     * 
     * @param player The player
     */
    fun clearPlayerCache(player: Player) {
        if (!initialized) return
        
        val keysToRemove = formatCache.keys.filter { it.contains(player.uniqueId.toString()) }
        keysToRemove.forEach { formatCache.remove(it) }
    }
    
    /**
     * Clear all formatting caches.
     */
    fun clearAllCaches() {
        if (!initialized) return
        
        formatCache.clear()
    }
    
    /**
     * Clean up expired cache entries.
     */
    fun cleanupExpiredCache() {
        if (!initialized) return
        
        val currentTime = System.currentTimeMillis()
        val expiredKeys = formatCache.entries
            .filter { it.value.isExpired(currentTime) }
            .map { it.key }
        
        expiredKeys.forEach { formatCache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            plugin.logger.fine("Cleaned ${expiredKeys.size} expired formatting cache entries")
        }
    }
    
    /**
     * Check if chat formatting is enabled.
     */
    fun isChatFormattingEnabled(): Boolean = initialized && chatEnabled
    
    /**
     * Check if tablist formatting is enabled.
     */
    fun isTablistFormattingEnabled(): Boolean = initialized && tablistEnabled
    
    /**
     * Check if nametag formatting is enabled.
     */
    fun isNametagFormattingEnabled(): Boolean = initialized && nametagEnabled
    
    /**
     * Get formatting statistics.
     * 
     * @return Map with formatting statistics
     */
    fun getFormattingStats(): Map<String, Any> {
        return mapOf(
            "initialized" to initialized,
            "chatEnabled" to chatEnabled,
            "tablistEnabled" to tablistEnabled,
            "nametagEnabled" to nametagEnabled,
            "colorCodesEnabled" to colorCodesEnabled,
            "hexColorsEnabled" to hexColorsEnabled,
            "cacheSize" to formatCache.size,
            "cacheTimeoutMs" to cacheTimeout
        )
    }
    
    /**
     * Reload the formatting service configuration.
     */
    fun reload() {
        if (initialized) {
            // Reload configuration
            initialize()
            
            // Clear caches to apply new settings
            clearAllCaches()
            
            plugin.logger.info("Formatting service reloaded")
        }
    }
    
    /**
     * Shutdown the formatting service.
     */
    fun shutdown() {
        if (initialized) {
            clearAllCaches()
            initialized = false
            plugin.logger.info("Formatting service shutdown")
        }
    }
    
    /**
     * Represents a cached formatted string with expiry time.
     */
    private data class CachedFormat(
        val formattedText: String,
        val expiryTime: Long
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime > expiryTime
        }
    }
}
