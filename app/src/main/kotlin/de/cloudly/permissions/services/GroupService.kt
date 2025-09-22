package de.cloudly.permissions.services

import de.cloudly.permissions.models.PermissionGroup
import de.cloudly.permissions.storage.PermissionStorage
import de.cloudly.permissions.utils.PermissionUtils
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Service for managing permission groups.
 * Handles CRUD operations, validation, and business logic for groups.
 */
class GroupService(
    private val plugin: JavaPlugin,
    private val storage: PermissionStorage
) {
    
    private var initialized = false
    
    /**
     * Initialize the group service.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Ensure base group exists
            if (!storage.groupExists("base")) {
                val baseGroup = PermissionGroup.createBaseGroup()
                storage.saveGroup(baseGroup)
                val debugMode = plugin.config.getBoolean("plugin.debug", false)
                if (debugMode) {
                    plugin.logger.info("Created default base group")
                }
            }
            
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Group service initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize group service", e)
            false
        }
    }
    
    /**
     * Create a new permission group.
     * 
     * @param name The name of the group
     * @param weight The weight of the group (higher = higher priority)
     * @param prefix Optional prefix for the group
     * @param suffix Optional suffix for the group
     * @return true if the group was created successfully
     */
    fun createGroup(
        name: String, 
        weight: Int, 
        prefix: String? = null, 
        suffix: String? = null
    ): Boolean {
        checkInitialized()
        
        // Validate group name
        if (!PermissionUtils.isValidGroupName(name)) {
            plugin.logger.warning("Invalid group name: $name")
            return false
        }
        
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        
        // Check if group already exists
        if (storage.groupExists(normalizedName)) {
            plugin.logger.warning("Group already exists: $normalizedName")
            return false
        }
        
        // Validate weight
        if (weight < 1) {
            plugin.logger.warning("Group weight must be at least 1")
            return false
        }
        
        // Create the group
        val group = PermissionGroup(
            name = normalizedName,
            weight = weight,
            prefix = prefix,
            suffix = suffix
        )
        
        return if (storage.saveGroup(group)) {
            plugin.logger.info("Created group '$normalizedName' with weight $weight")
            true
        } else {
            plugin.logger.warning("Failed to save group: $normalizedName")
            false
        }
    }
    
    /**
     * Delete a permission group.
     * 
     * @param name The name of the group to delete
     * @return true if the group was deleted successfully
     */
    fun deleteGroup(name: String): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        
        // Prevent deletion of base group
        if (normalizedName == "base") {
            plugin.logger.warning("Cannot delete the base group")
            return false
        }
        
        // Check if group exists
        if (!storage.groupExists(normalizedName)) {
            plugin.logger.warning("Group does not exist: $normalizedName")
            return false
        }
        
        return if (storage.deleteGroup(normalizedName)) {
            plugin.logger.info("Deleted group: $normalizedName")
            true
        } else {
            plugin.logger.warning("Failed to delete group: $normalizedName")
            false
        }
    }
    
    /**
     * Get a permission group by name.
     * 
     * @param name The name of the group
     * @return The group, or null if not found
     */
    fun getGroup(name: String): PermissionGroup? {
        checkInitialized()
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        return storage.getGroup(normalizedName)
    }
    
    /**
     * Get all permission groups.
     * 
     * @return Collection of all groups
     */
    fun getAllGroups(): Collection<PermissionGroup> {
        checkInitialized()
        return storage.getAllGroups()
    }
    
    /**
     * Check if a group exists.
     * 
     * @param name The name of the group
     * @return true if the group exists
     */
    fun groupExists(name: String): Boolean {
        checkInitialized()
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        return storage.groupExists(normalizedName)
    }
    
    /**
     * Update the weight of a group.
     * 
     * @param name The name of the group
     * @param newWeight The new weight value
     * @return true if the weight was updated successfully
     */
    fun setGroupWeight(name: String, newWeight: Int): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        val group = storage.getGroup(normalizedName) ?: return false
        
        // Validate weight
        if (newWeight < 1) {
            plugin.logger.warning("Group weight must be at least 1")
            return false
        }
        
        // Special handling for base group - must remain weight 1
        if (normalizedName == "base" && newWeight != 1) {
            plugin.logger.warning("Base group weight cannot be changed from 1")
            return false
        }
        
        val updatedGroup = group.copy(weight = newWeight)
        return if (storage.saveGroup(updatedGroup)) {
            plugin.logger.info("Updated weight for group '$normalizedName' to $newWeight")
            true
        } else {
            plugin.logger.warning("Failed to update weight for group: $normalizedName")
            false
        }
    }
    
    /**
     * Set the prefix for a group.
     * 
     * @param name The name of the group
     * @param prefix The new prefix (can be null to remove)
     * @return true if the prefix was updated successfully
     */
    fun setGroupPrefix(name: String, prefix: String?): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        val group = storage.getGroup(normalizedName) ?: return false
        
        val updatedGroup = group.copy(prefix = prefix)
        return if (storage.saveGroup(updatedGroup)) {
            plugin.logger.info("Updated prefix for group '$normalizedName'")
            true
        } else {
            plugin.logger.warning("Failed to update prefix for group: $normalizedName")
            false
        }
    }
    
    /**
     * Set the suffix for a group.
     * 
     * @param name The name of the group
     * @param suffix The new suffix (can be null to remove)
     * @return true if the suffix was updated successfully
     */
    fun setGroupSuffix(name: String, suffix: String?): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(name)
        val group = storage.getGroup(normalizedName) ?: return false
        
        val updatedGroup = group.copy(suffix = suffix)
        return if (storage.saveGroup(updatedGroup)) {
            plugin.logger.info("Updated suffix for group '$normalizedName'")
            true
        } else {
            plugin.logger.warning("Failed to update suffix for group: $normalizedName")
            false
        }
    }
    
    /**
     * Add a permission to a group.
     * 
     * @param groupName The name of the group
     * @param permission The permission to add
     * @return true if the permission was added successfully
     */
    fun addPermission(groupName: String, permission: String): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(groupName)
        val normalizedPermission = PermissionUtils.normalizePermission(permission)
        
        // Validate permission
        if (!PermissionUtils.isValidPermission(normalizedPermission)) {
            plugin.logger.warning("Invalid permission: $normalizedPermission")
            return false
        }
        
        val group = storage.getGroup(normalizedName) ?: return false
        
        // Add permission to group
        group.addPermission(normalizedPermission)
        
        return if (storage.saveGroup(group)) {
            plugin.logger.info("Added permission '$normalizedPermission' to group '$normalizedName'")
            true
        } else {
            plugin.logger.warning("Failed to add permission to group: $normalizedName")
            false
        }
    }
    
    /**
     * Remove a permission from a group.
     * 
     * @param groupName The name of the group
     * @param permission The permission to remove
     * @return true if the permission was removed successfully
     */
    fun removePermission(groupName: String, permission: String): Boolean {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(groupName)
        val normalizedPermission = PermissionUtils.normalizePermission(permission)
        
        val group = storage.getGroup(normalizedName) ?: return false
        
        // Remove permission from group
        group.removePermission(normalizedPermission)
        
        return if (storage.saveGroup(group)) {
            plugin.logger.info("Removed permission '$normalizedPermission' from group '$normalizedName'")
            true
        } else {
            plugin.logger.warning("Failed to remove permission from group: $normalizedName")
            false
        }
    }
    
    /**
     * Get all permissions for a group.
     * 
     * @param groupName The name of the group
     * @return Set of permissions, or empty set if group not found
     */
    fun getGroupPermissions(groupName: String): Set<String> {
        checkInitialized()
        
        val normalizedName = PermissionUtils.normalizeGroupName(groupName)
        val group = storage.getGroup(normalizedName)
        return group?.permissions?.toSet() ?: emptySet()
    }
    
    /**
     * Get groups sorted by weight (highest first).
     * 
     * @return List of groups sorted by weight descending
     */
    fun getGroupsByWeight(): List<PermissionGroup> {
        checkInitialized()
        return storage.getAllGroups().sortedByDescending { it.weight }
    }
    
    /**
     * Get the highest weight group from a set of group names.
     * 
     * @param groupNames The group names to check
     * @return The highest weight group, or null if none found
     */
    fun getHighestWeightGroup(groupNames: Set<String>): PermissionGroup? {
        checkInitialized()
        
        return groupNames
            .mapNotNull { storage.getGroup(it) }
            .maxByOrNull { it.weight }
    }
    
    /**
     * Reload the group service.
     */
    fun reload() {
        if (initialized) {
            plugin.logger.info("Group service reloaded")
        }
    }
    
    /**
     * Shutdown the group service.
     */
    fun shutdown() {
        if (initialized) {
            initialized = false
            plugin.logger.info("Group service shutdown")
        }
    }
    
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("Group service is not initialized")
        }
    }
}
