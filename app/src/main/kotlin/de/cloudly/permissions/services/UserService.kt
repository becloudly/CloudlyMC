package de.cloudly.permissions.services

import de.cloudly.permissions.models.PermissionUser
import de.cloudly.permissions.storage.PermissionStorage
import de.cloudly.permissions.utils.PermissionUtils
import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID
import java.util.logging.Level
import org.bukkit.plugin.java.JavaPlugin

/**
 * Service for managing permission users.
 * Handles user operations, group assignments, and individual permissions.
 */
class UserService(
    private val plugin: JavaPlugin,
    private val storage: PermissionStorage
) {
    
    private var initialized = false
    
    /**
     * Initialize the user service.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("User service initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize user service", e)
            false
        }
    }
    
    /**
     * Ensure a user exists in the system with the base group.
     * Called when a player joins the server.
     * 
     * @param uuid The UUID of the player
     * @param username The current username of the player
     * @return The user object
     */
    fun ensureUserExists(uuid: UUID, username: String): PermissionUser {
        checkInitialized()
        
        var user = storage.getUser(uuid)
        
        if (user == null) {
            // Create new user with base group
            user = PermissionUser.createNewUser(uuid, username)
            storage.saveUser(user)
            plugin.logger.info("Created new user: $username ($uuid)")
        } else {
            // Update username if changed and ensure base group
            if (user.username != username) {
                user = user.copy(username = username, lastUpdated = Instant.now())
            }
            user.ensureBaseGroup()
            storage.saveUser(user)
        }
        
        return user
    }
    
    /**
     * Get a user by UUID.
     * 
     * @param uuid The UUID of the player
     * @return The user, or null if not found
     */
    fun getUser(uuid: UUID): PermissionUser? {
        checkInitialized()
        return storage.getUser(uuid)
    }
    
    /**
     * Get a user by username.
     * 
     * @param username The username of the player
     * @return The user, or null if not found
     */
    fun getUserByName(username: String): PermissionUser? {
        checkInitialized()
        return storage.getUserByName(username)
    }
    
    /**
     * Get all users.
     * 
     * @return Collection of all users
     */
    fun getAllUsers(): Collection<PermissionUser> {
        checkInitialized()
        return storage.getAllUsers()
    }
    
    /**
     * Add a permanent group to a user.
     * 
     * @param uuid The UUID of the player
     * @param groupName The name of the group to add
     * @return true if the group was added successfully
     */
    fun addUserToGroup(uuid: UUID, groupName: String): Boolean {
        checkInitialized()
        
        val normalizedGroupName = PermissionUtils.normalizeGroupName(groupName)
        
        // Check if group exists
        if (!storage.groupExists(normalizedGroupName)) {
            plugin.logger.warning("Group does not exist: $normalizedGroupName")
            return false
        }
        
        val user = storage.getUser(uuid) ?: return false
        
        user.addGroup(normalizedGroupName)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Added user ${user.username} to group '$normalizedGroupName'")
            true
        } else {
            plugin.logger.warning("Failed to add user to group: ${user.username}")
            false
        }
    }
    
    /**
     * Add a temporary group to a user.
     * 
     * @param uuid The UUID of the player
     * @param groupName The name of the group to add
     * @param durationStr The duration string (e.g., "1d", "2h", "30m")
     * @return true if the group was added successfully
     */
    fun addUserToTemporaryGroup(uuid: UUID, groupName: String, durationStr: String): Boolean {
        checkInitialized()
        
        val normalizedGroupName = PermissionUtils.normalizeGroupName(groupName)
        
        // Check if group exists
        if (!storage.groupExists(normalizedGroupName)) {
            plugin.logger.warning("Group does not exist: $normalizedGroupName")
            return false
        }
        
        // Parse duration
        val expiryTimestamp = PermissionUtils.calculateExpiryTimestamp(durationStr)
        if (expiryTimestamp == null) {
            plugin.logger.warning("Invalid duration format: $durationStr")
            return false
        }
        
        val user = storage.getUser(uuid) ?: return false
        
        user.addTemporaryGroup(normalizedGroupName, expiryTimestamp)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Added user ${user.username} to temporary group '$normalizedGroupName' for $durationStr")
            true
        } else {
            plugin.logger.warning("Failed to add user to temporary group: ${user.username}")
            false
        }
    }
    
    /**
     * Remove a group from a user (both permanent and temporary).
     * 
     * @param uuid The UUID of the player
     * @param groupName The name of the group to remove
     * @return true if the group was removed successfully
     */
    fun removeUserFromGroup(uuid: UUID, groupName: String): Boolean {
        checkInitialized()
        
        val normalizedGroupName = PermissionUtils.normalizeGroupName(groupName)
        
        // Prevent removal of base group
        if (normalizedGroupName == "base") {
            plugin.logger.warning("Cannot remove user from base group")
            return false
        }
        
        val user = storage.getUser(uuid) ?: return false
        
        user.removeGroup(normalizedGroupName)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Removed user ${user.username} from group '$normalizedGroupName'")
            true
        } else {
            plugin.logger.warning("Failed to remove user from group: ${user.username}")
            false
        }
    }
    
    /**
     * Add a permanent permission to a user.
     * 
     * @param uuid The UUID of the player
     * @param permission The permission to add
     * @return true if the permission was added successfully
     */
    fun addUserPermission(uuid: UUID, permission: String): Boolean {
        checkInitialized()
        
        val normalizedPermission = PermissionUtils.normalizePermission(permission)
        
        // Validate permission
        if (!PermissionUtils.isValidPermission(normalizedPermission)) {
            plugin.logger.warning("Invalid permission: $normalizedPermission")
            return false
        }
        
        val user = storage.getUser(uuid) ?: return false
        
        user.addPermission(normalizedPermission)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Added permission '$normalizedPermission' to user ${user.username}")
            true
        } else {
            plugin.logger.warning("Failed to add permission to user: ${user.username}")
            false
        }
    }
    
    /**
     * Add a temporary permission to a user.
     * 
     * @param uuid The UUID of the player
     * @param permission The permission to add
     * @param durationStr The duration string (e.g., "1d", "2h", "30m")
     * @return true if the permission was added successfully
     */
    fun addUserTemporaryPermission(uuid: UUID, permission: String, durationStr: String): Boolean {
        checkInitialized()
        
        val normalizedPermission = PermissionUtils.normalizePermission(permission)
        
        // Validate permission
        if (!PermissionUtils.isValidPermission(normalizedPermission)) {
            plugin.logger.warning("Invalid permission: $normalizedPermission")
            return false
        }
        
        // Parse duration
        val expiryTimestamp = PermissionUtils.calculateExpiryTimestamp(durationStr)
        if (expiryTimestamp == null) {
            plugin.logger.warning("Invalid duration format: $durationStr")
            return false
        }
        
        val user = storage.getUser(uuid) ?: return false
        
        user.addTemporaryPermission(normalizedPermission, expiryTimestamp)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Added temporary permission '$normalizedPermission' to user ${user.username} for $durationStr")
            true
        } else {
            plugin.logger.warning("Failed to add temporary permission to user: ${user.username}")
            false
        }
    }
    
    /**
     * Remove a permission from a user (both permanent and temporary).
     * 
     * @param uuid The UUID of the player
     * @param permission The permission to remove
     * @return true if the permission was removed successfully
     */
    fun removeUserPermission(uuid: UUID, permission: String): Boolean {
        checkInitialized()
        
        val normalizedPermission = PermissionUtils.normalizePermission(permission)
        
        val user = storage.getUser(uuid) ?: return false
        
        user.removePermission(normalizedPermission)
        
        return if (storage.saveUser(user)) {
            plugin.logger.info("Removed permission '$normalizedPermission' from user ${user.username}")
            true
        } else {
            plugin.logger.warning("Failed to remove permission from user: ${user.username}")
            false
        }
    }
    
    /**
     * Get all active groups for a user (permanent + non-expired temporary).
     * 
     * @param uuid The UUID of the player
     * @return Set of active group names
     */
    fun getUserGroups(uuid: UUID): Set<String> {
        checkInitialized()
        
        val user = storage.getUser(uuid) ?: return emptySet()
        return user.getActiveGroups()
    }
    
    /**
     * Get all active permissions for a user (permanent + non-expired temporary).
     * 
     * @param uuid The UUID of the player
     * @return Set of active permission nodes
     */
    fun getUserPermissions(uuid: UUID): Set<String> {
        checkInitialized()
        
        val user = storage.getUser(uuid) ?: return emptySet()
        return user.getActivePermissions()
    }
    
    /**
     * Clean up expired temporary permissions and groups for a user.
     * 
     * @param uuid The UUID of the player
     * @return Number of expired items removed
     */
    fun cleanupUserExpiredPermissions(uuid: UUID): Int {
        checkInitialized()
        
        val user = storage.getUser(uuid) ?: return 0
        val removedCount = user.cleanupExpired()
        
        if (removedCount > 0) {
            storage.saveUser(user)
            plugin.logger.fine("Cleaned $removedCount expired items for user ${user.username}")
        }
        
        return removedCount
    }
    
    /**
     * Clean up expired temporary permissions and groups for all users.
     * 
     * @return Number of users that had expired items cleaned
     */
    fun cleanupAllExpiredPermissions(): Int {
        checkInitialized()
        return storage.cleanupExpiredPermissions()
    }
    
    /**
     * Get users who belong to a specific group.
     * 
     * @param groupName The name of the group
     * @return List of users in the group
     */
    fun getUsersInGroup(groupName: String): List<PermissionUser> {
        checkInitialized()
        
        val normalizedGroupName = PermissionUtils.normalizeGroupName(groupName)
        return storage.getUsersInGroup(normalizedGroupName)
    }
    
    /**
     * Update a user's username.
     * 
     * @param uuid The UUID of the player
     * @param newUsername The new username
     * @return true if the username was updated successfully
     */
    fun updateUsername(uuid: UUID, newUsername: String): Boolean {
        checkInitialized()
        
        val user = storage.getUser(uuid) ?: return false
        
        if (user.username != newUsername) {
            val updatedUser = user.copy(
                username = newUsername,
                lastUpdated = Instant.now()
            )
            
            return if (storage.saveUser(updatedUser)) {
                plugin.logger.info("Updated username for $uuid: ${user.username} -> $newUsername")
                true
            } else {
                plugin.logger.warning("Failed to update username for user: $uuid")
                false
            }
        }
        
        return true // No change needed
    }
    
    /**
     * Delete a user from the system.
     * 
     * @param uuid The UUID of the player
     * @return true if the user was deleted successfully
     */
    fun deleteUser(uuid: UUID): Boolean {
        checkInitialized()
        
        return if (storage.deleteUser(uuid)) {
            plugin.logger.info("Deleted user: $uuid")
            true
        } else {
            plugin.logger.warning("Failed to delete user: $uuid")
            false
        }
    }
    
    /**
     * Check if a user exists.
     * 
     * @param uuid The UUID of the player
     * @return true if the user exists
     */
    fun userExists(uuid: UUID): Boolean {
        checkInitialized()
        return storage.userExists(uuid)
    }
    
    /**
     * Reload the user service.
     */
    fun reload() {
        if (initialized) {
            plugin.logger.info("User service reloaded")
        }
    }
    
    /**
     * Shutdown the user service.
     */
    fun shutdown() {
        if (initialized) {
            initialized = false
            plugin.logger.info("User service shutdown")
        }
    }
    
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("User service is not initialized")
        }
    }
}
