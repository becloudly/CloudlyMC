package de.cloudly.permissions.services

import de.cloudly.permissions.models.PermissionGroup
import de.cloudly.permissions.models.PermissionUser
import de.cloudly.permissions.storage.PermissionStorage
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Service for resolving effective permissions for users.
 * Handles weight-based group hierarchy, permission inheritance, and caching.
 */
class PermissionResolver(
    private val plugin: JavaPlugin,
    private val storage: PermissionStorage,
    private val groupService: GroupService
) {
    
    // Caches for performance
    private val effectivePermissionCache = ConcurrentHashMap<UUID, CachedUserPermissions>()
    private val groupPermissionCache = ConcurrentHashMap<String, Set<String>>()
    private val cacheTimeout = 300000L // 5 minutes in milliseconds
    
    private var initialized = false
    
    /**
     * Initialize the permission resolver.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Permission resolver initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize permission resolver", e)
            false
        }
    }
    
    /**
     * Check if a user has a specific permission.
     * 
     * @param userUuid The UUID of the user
     * @param permission The permission to check
     * @return true if the user has the permission
     */
    fun hasPermission(userUuid: UUID, permission: String): Boolean {
        checkInitialized()
        
        val effectivePermissions = getEffectivePermissions(userUuid)
        
        // Check for exact permission
        if (effectivePermissions.contains(permission)) {
            return true
        }
        
        // Check for negative permission (explicit denial)
        if (effectivePermissions.contains("-$permission")) {
            return false
        }
        
        // Check for wildcard permissions
        return effectivePermissions.any { perm ->
            when {
                perm == "*" -> true
                perm.endsWith("*") -> {
                    val prefix = perm.substring(0, perm.length - 1)
                    permission.startsWith(prefix)
                }
                else -> false
            }
        }
    }
    
    /**
     * Get all effective permissions for a user.
     * Combines group permissions (weight-based) with individual permissions.
     * 
     * @param userUuid The UUID of the user
     * @return Set of all effective permissions
     */
    fun getEffectivePermissions(userUuid: UUID): Set<String> {
        checkInitialized()
        
        // Check cache first
        val cached = effectivePermissionCache[userUuid]
        if (cached != null && !cached.isExpired()) {
            return cached.permissions
        }
        
        val user = storage.getUser(userUuid)
        if (user == null) {
            effectivePermissionCache[userUuid] = CachedUserPermissions(emptySet(), System.currentTimeMillis() + cacheTimeout)
            return emptySet()
        }
        
        val effectivePermissions = mutableSetOf<String>()
        
        // Get user's active groups
        val activeGroups = user.getActiveGroups()
        
        // Get groups sorted by weight (lowest to highest, so higher weight overrides)
        val sortedGroups = activeGroups
            .mapNotNull { groupName -> storage.getGroup(groupName) }
            .sortedBy { it.weight }
        
        // Add permissions from groups (in weight order)
        for (group in sortedGroups) {
            val groupPermissions = getGroupEffectivePermissions(group)
            effectivePermissions.addAll(groupPermissions)
        }
        
        // Add individual user permissions (these override group permissions)
        val userPermissions = user.getActivePermissions()
        effectivePermissions.addAll(userPermissions)
        
        // Cache the result
        effectivePermissionCache[userUuid] = CachedUserPermissions(
            effectivePermissions.toSet(),
            System.currentTimeMillis() + cacheTimeout
        )
        
        return effectivePermissions
    }
    
    /**
     * Get effective permissions for a group, including wildcard expansion.
     * 
     * @param group The permission group
     * @return Set of effective permissions for the group
     */
    private fun getGroupEffectivePermissions(group: PermissionGroup): Set<String> {
        // Check cache first
        val cached = groupPermissionCache[group.name]
        if (cached != null) {
            return cached
        }
        
        val allKnownPermissions = storage.getAllPermissionNodes()
        val effectivePermissions = group.getEffectivePermissions(allKnownPermissions)
        
        // Cache the result
        groupPermissionCache[group.name] = effectivePermissions
        
        return effectivePermissions
    }
    
    /**
     * Get the primary group (highest weight) for a user.
     * 
     * @param userUuid The UUID of the user
     * @return The name of the primary group, or null if no groups
     */
    fun getPrimaryGroup(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        return groupService.getHighestWeightGroup(activeGroups)?.name
    }
    
    /**
     * Get the prefix for a user from their highest weight group.
     * 
     * @param userUuid The UUID of the user
     * @return The prefix string, or null if no prefix
     */
    fun getPlayerPrefix(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        val highestGroup = groupService.getHighestWeightGroup(activeGroups)
        return highestGroup?.prefix
    }
    
    /**
     * Get the suffix for a user from their highest weight group.
     * 
     * @param userUuid The UUID of the user
     * @return The suffix string, or null if no suffix
     */
    fun getPlayerSuffix(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        val highestGroup = groupService.getHighestWeightGroup(activeGroups)
        return highestGroup?.suffix
    }
    
    /**
     * Get the chat format for a user from their highest weight group.
     * 
     * @param userUuid The UUID of the user
     * @return The chat format string, or null if no format
     */
    fun getPlayerChatFormat(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        val highestGroup = groupService.getHighestWeightGroup(activeGroups)
        return highestGroup?.chatFormat
    }
    
    /**
     * Get the tablist format for a user from their highest weight group.
     * 
     * @param userUuid The UUID of the user
     * @return The tablist format string, or null if no format
     */
    fun getPlayerTablistFormat(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        val highestGroup = groupService.getHighestWeightGroup(activeGroups)
        return highestGroup?.tablistFormat
    }
    
    /**
     * Get the nametag format for a user from their highest weight group.
     * 
     * @param userUuid The UUID of the user
     * @return The nametag format string, or null if no format
     */
    fun getPlayerNametagFormat(userUuid: UUID): String? {
        checkInitialized()
        
        val user = storage.getUser(userUuid) ?: return null
        val activeGroups = user.getActiveGroups()
        
        val highestGroup = groupService.getHighestWeightGroup(activeGroups)
        return highestGroup?.nametagFormat
    }
    
    /**
     * Clear permission cache for a specific user.
     * 
     * @param userUuid The UUID of the user
     */
    fun clearUserCache(userUuid: UUID) {
        checkInitialized()
        effectivePermissionCache.remove(userUuid)
    }
    
    /**
     * Clear permission cache for a specific group.
     * This should be called when a group's permissions are modified.
     * 
     * @param groupName The name of the group
     */
    fun clearGroupCache(groupName: String) {
        checkInitialized()
        
        groupPermissionCache.remove(groupName)
        
        // Clear user caches for users in this group
        val usersInGroup = storage.getUsersInGroup(groupName)
        usersInGroup.forEach { user ->
            effectivePermissionCache.remove(user.uuid)
        }
    }
    
    /**
     * Clear all permission caches.
     */
    fun clearCache() {
        checkInitialized()
        
        effectivePermissionCache.clear()
        groupPermissionCache.clear()
        plugin.logger.fine("Cleared all permission caches")
    }
    
    /**
     * Clean up expired cache entries.
     */
    fun cleanupExpiredCache() {
        checkInitialized()
        
        val currentTime = System.currentTimeMillis()
        val expiredUsers = effectivePermissionCache.entries
            .filter { it.value.isExpired(currentTime) }
            .map { it.key }
        
        expiredUsers.forEach { effectivePermissionCache.remove(it) }
        
        if (expiredUsers.isNotEmpty()) {
            plugin.logger.fine("Cleaned ${expiredUsers.size} expired permission cache entries")
        }
    }
    
    /**
     * Get cache statistics for debugging.
     * 
     * @return Map with cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        checkInitialized()
        
        return mapOf(
            "userCacheSize" to effectivePermissionCache.size,
            "groupCacheSize" to groupPermissionCache.size,
            "cacheTimeoutMs" to cacheTimeout
        )
    }
    
    /**
     * Reload the permission resolver.
     */
    fun reload() {
        if (initialized) {
            clearCache()
            plugin.logger.info("Permission resolver reloaded")
        }
    }
    
    /**
     * Shutdown the permission resolver.
     */
    fun shutdown() {
        if (initialized) {
            clearCache()
            initialized = false
            plugin.logger.info("Permission resolver shutdown")
        }
    }
    
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("Permission resolver is not initialized")
        }
    }
    
    /**
     * Represents cached user permissions with expiry time.
     */
    private data class CachedUserPermissions(
        val permissions: Set<String>,
        val expiryTime: Long
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime > expiryTime
        }
    }
}
