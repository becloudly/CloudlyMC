package de.cloudly.permissions.models

import de.cloudly.storage.core.Serializable
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Represents a user in the permission system.
 *
 * @property uuid The UUID of the player
 * @property username The current username of the player
 * @property groups Set of permanent group names this user belongs to
 * @property temporaryGroups Map of temporary group names to their expiry timestamps
 * @property permissions Set of individual permissions granted to this user
 * @property temporaryPermissions Map of temporary permissions to their expiry timestamps
 * @property lastUpdated When this user's permissions were last modified
 */
data class PermissionUser(
    val uuid: UUID,
    val username: String,
    val groups: MutableSet<String> = mutableSetOf(),
    val temporaryGroups: MutableMap<String, Long> = mutableMapOf(),
    val permissions: MutableSet<String> = mutableSetOf(),
    val temporaryPermissions: MutableMap<String, Long> = mutableMapOf(),
    val lastUpdated: Instant = Instant.now()
) : Serializable {
    
    /**
     * Add a permanent group to this user.
     * 
     * @param groupName The name of the group to add
     */
    fun addGroup(groupName: String) {
        groups.add(groupName)
        // Remove from temporary groups if it exists there
        temporaryGroups.remove(groupName)
    }
    
    /**
     * Add a temporary group to this user.
     * 
     * @param groupName The name of the group to add
     * @param expiryTimestamp The timestamp when this group expires (epoch seconds)
     */
    fun addTemporaryGroup(groupName: String, expiryTimestamp: Long) {
        temporaryGroups[groupName] = expiryTimestamp
        // Remove from permanent groups if it exists there
        groups.remove(groupName)
    }
    
    /**
     * Remove a group from this user (both permanent and temporary).
     * 
     * @param groupName The name of the group to remove
     */
    fun removeGroup(groupName: String) {
        groups.remove(groupName)
        temporaryGroups.remove(groupName)
    }
    
    /**
     * Add a permanent permission to this user.
     * 
     * @param permission The permission node to add
     */
    fun addPermission(permission: String) {
        permissions.add(permission)
        // Remove from temporary permissions if it exists there
        temporaryPermissions.remove(permission)
    }
    
    /**
     * Add a temporary permission to this user.
     * 
     * @param permission The permission node to add
     * @param expiryTimestamp The timestamp when this permission expires (epoch seconds)
     */
    fun addTemporaryPermission(permission: String, expiryTimestamp: Long) {
        temporaryPermissions[permission] = expiryTimestamp
        // Remove from permanent permissions if it exists there
        permissions.remove(permission)
    }
    
    /**
     * Remove a permission from this user (both permanent and temporary).
     * 
     * @param permission The permission node to remove
     */
    fun removePermission(permission: String) {
        permissions.remove(permission)
        temporaryPermissions.remove(permission)
    }
    
    /**
     * Get all groups this user belongs to (permanent + non-expired temporary).
     * 
     * @param currentTime The current timestamp to check against
     * @return Set of all active group names
     */
    fun getActiveGroups(currentTime: Long = Instant.now().epochSecond): Set<String> {
        val activeGroups = mutableSetOf<String>()
        activeGroups.addAll(groups)
        
        // Add non-expired temporary groups
        temporaryGroups.forEach { (groupName, expiry) ->
            if (expiry > currentTime) {
                activeGroups.add(groupName)
            }
        }
        
        return activeGroups
    }
    
    /**
     * Get all permissions this user has (permanent + non-expired temporary).
     * 
     * @param currentTime The current timestamp to check against
     * @return Set of all active permission nodes
     */
    fun getActivePermissions(currentTime: Long = Instant.now().epochSecond): Set<String> {
        val activePermissions = mutableSetOf<String>()
        activePermissions.addAll(permissions)
        
        // Add non-expired temporary permissions
        temporaryPermissions.forEach { (permission, expiry) ->
            if (expiry > currentTime) {
                activePermissions.add(permission)
            }
        }
        
        return activePermissions
    }
    
    /**
     * Clean up expired temporary groups and permissions.
     * 
     * @param currentTime The current timestamp to check against
     * @return Number of expired items removed
     */
    fun cleanupExpired(currentTime: Long = Instant.now().epochSecond): Int {
        var removedCount = 0
        
        // Remove expired temporary groups
        val expiredGroups = temporaryGroups.filter { (_, expiry) -> expiry <= currentTime }
        expiredGroups.keys.forEach { groupName ->
            temporaryGroups.remove(groupName)
            removedCount++
        }
        
        // Remove expired temporary permissions
        val expiredPermissions = temporaryPermissions.filter { (_, expiry) -> expiry <= currentTime }
        expiredPermissions.keys.forEach { permission ->
            temporaryPermissions.remove(permission)
            removedCount++
        }
        
        return removedCount
    }
    
    /**
     * Check if the user has the base group.
     * Every user must have the base group for security.
     * 
     * @return true if user has the base group
     */
    fun hasBaseGroup(): Boolean {
        return groups.contains("base") || temporaryGroups.containsKey("base")
    }
    
    /**
     * Ensure the user has the base group.
     * This should be called when the user is first created or when base group is missing.
     */
    fun ensureBaseGroup() {
        if (!hasBaseGroup()) {
            addGroup("base")
        }
    }
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("uuid", uuid.toString())
        json.put("username", username)
        json.put("groups", JSONArray(groups))
        
        // Serialize temporary groups as JSON object
        val tempGroupsJson = JSONObject()
        temporaryGroups.forEach { (group, expiry) ->
            tempGroupsJson.put(group, expiry)
        }
        json.put("temporaryGroups", tempGroupsJson)
        
        json.put("permissions", JSONArray(permissions))
        
        // Serialize temporary permissions as JSON object
        val tempPermsJson = JSONObject()
        temporaryPermissions.forEach { (permission, expiry) ->
            tempPermsJson.put(permission, expiry)
        }
        json.put("temporaryPermissions", tempPermsJson)
        
        json.put("lastUpdated", lastUpdated.epochSecond)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "permission_user"
    
    companion object {
        /**
         * Create a new user with the base group assigned.
         * 
         * @param uuid The UUID of the player
         * @param username The username of the player
         * @return A new PermissionUser with the base group
         */
        fun createNewUser(uuid: UUID, username: String): PermissionUser {
            val user = PermissionUser(uuid = uuid, username = username)
            user.ensureBaseGroup()
            return user
        }
        
        /**
         * Deserialize a PermissionUser from a JSON string.
         * 
         * @param data The JSON string data
         * @return The deserialized PermissionUser, or null if deserialization failed
         */
        fun deserialize(data: String): PermissionUser? {
            return try {
                val json = JSONObject(data)
                
                val uuid = UUID.fromString(json.getString("uuid"))
                val username = json.getString("username")
                
                val groups = mutableSetOf<String>()
                if (json.has("groups") && !json.isNull("groups")) {
                    val groupArray = json.getJSONArray("groups")
                    for (i in 0 until groupArray.length()) {
                        groups.add(groupArray.getString(i))
                    }
                }
                
                val temporaryGroups = mutableMapOf<String, Long>()
                if (json.has("temporaryGroups") && !json.isNull("temporaryGroups")) {
                    val tempGroupsJson = json.getJSONObject("temporaryGroups")
                    tempGroupsJson.keys().forEach { group ->
                        temporaryGroups[group] = tempGroupsJson.getLong(group)
                    }
                }
                
                val permissions = mutableSetOf<String>()
                if (json.has("permissions") && !json.isNull("permissions")) {
                    val permArray = json.getJSONArray("permissions")
                    for (i in 0 until permArray.length()) {
                        permissions.add(permArray.getString(i))
                    }
                }
                
                val temporaryPermissions = mutableMapOf<String, Long>()
                if (json.has("temporaryPermissions") && !json.isNull("temporaryPermissions")) {
                    val tempPermsJson = json.getJSONObject("temporaryPermissions")
                    tempPermsJson.keys().forEach { permission ->
                        temporaryPermissions[permission] = tempPermsJson.getLong(permission)
                    }
                }
                
                val lastUpdated = if (json.has("lastUpdated")) {
                    Instant.ofEpochSecond(json.getLong("lastUpdated"))
                } else Instant.now()
                
                val user = PermissionUser(
                    uuid = uuid,
                    username = username,
                    groups = groups,
                    temporaryGroups = temporaryGroups,
                    permissions = permissions,
                    temporaryPermissions = temporaryPermissions,
                    lastUpdated = lastUpdated
                )
                
                // Ensure user has base group for security
                user.ensureBaseGroup()
                
                user
            } catch (e: Exception) {
                null
            }
        }
    }
}
