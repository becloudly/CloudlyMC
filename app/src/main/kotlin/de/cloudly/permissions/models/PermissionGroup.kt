package de.cloudly.permissions.models

import de.cloudly.storage.core.Serializable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a permission group in the permission system.
 *
 * @property name The unique name of the group
 * @property weight The weight of the group (higher values have higher priority)
 * @property permissions Set of permission nodes this group has
 * @property prefix Optional prefix for chat formatting (supports color codes)
 * @property suffix Optional suffix for chat formatting (supports color codes)
 * @property chatFormat Optional custom chat format string
 * @property tablistFormat Optional custom tablist format string
 * @property nametagFormat Optional custom nametag format string
 * @property isDefault Whether this is the default group (base group)
 */
data class PermissionGroup(
    val name: String,
    val weight: Int,
    val permissions: MutableSet<String> = mutableSetOf(),
    val prefix: String? = null,
    val suffix: String? = null,
    val chatFormat: String? = null,
    val tablistFormat: String? = null,
    val nametagFormat: String? = null,
    val isDefault: Boolean = false
) : Serializable {
    
    /**
     * Check if this group has a specific permission.
     * Supports wildcard permissions (e.g., "plugin.*" grants "plugin.command").
     * 
     * @param permission The permission node to check
     * @return true if the group has the permission, false otherwise
     */
    fun hasPermission(permission: String): Boolean {
        // Direct permission check
        if (permissions.contains(permission)) {
            return true
        }
        
        // Check for negative permission (explicit denial)
        if (permissions.contains("-$permission")) {
            return false
        }
        
        // Check wildcard permissions
        return permissions.any { perm ->
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
     * Add a permission to this group.
     * 
     * @param permission The permission node to add
     */
    fun addPermission(permission: String) {
        permissions.add(permission)
    }
    
    /**
     * Remove a permission from this group.
     * 
     * @param permission The permission node to remove
     */
    fun removePermission(permission: String) {
        permissions.remove(permission)
    }
    
    /**
     * Get all permissions for this group (including effective wildcard permissions).
     * 
     * @param allKnownPermissions Set of all known permission nodes in the server
     * @return Set of all effective permissions
     */
    fun getEffectivePermissions(allKnownPermissions: Set<String>): Set<String> {
        val effective = mutableSetOf<String>()
        
        for (permission in permissions) {
            when {
                permission == "*" -> effective.addAll(allKnownPermissions)
                permission.endsWith("*") -> {
                    val prefix = permission.substring(0, permission.length - 1)
                    effective.addAll(allKnownPermissions.filter { it.startsWith(prefix) })
                }
                else -> effective.add(permission)
            }
        }
        
        return effective
    }
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("name", name)
        json.put("weight", weight)
        json.put("permissions", JSONArray(permissions))
        json.put("prefix", prefix)
        json.put("suffix", suffix)
        json.put("chatFormat", chatFormat)
        json.put("tablistFormat", tablistFormat)
        json.put("nametagFormat", nametagFormat)
        json.put("isDefault", isDefault)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "permission_group"
    
    companion object {
        /**
         * Create the default base group.
         * 
         * @return A default base group with weight 1 and no permissions
         */
        fun createBaseGroup(): PermissionGroup {
            return PermissionGroup(
                name = "base",
                weight = 1,
                permissions = mutableSetOf(),
                isDefault = true
            )
        }
        
        /**
         * Deserialize a PermissionGroup from a JSON string.
         * 
         * @param data The JSON string data
         * @return The deserialized PermissionGroup, or null if deserialization failed
         */
        fun deserialize(data: String): PermissionGroup? {
            return try {
                val json = JSONObject(data)
                
                val name = json.getString("name")
                val weight = json.getInt("weight")
                
                val permissions = mutableSetOf<String>()
                if (json.has("permissions") && !json.isNull("permissions")) {
                    val permArray = json.getJSONArray("permissions")
                    for (i in 0 until permArray.length()) {
                        permissions.add(permArray.getString(i))
                    }
                }
                
                val prefix = if (json.has("prefix") && !json.isNull("prefix")) {
                    json.getString("prefix")
                } else null
                
                val suffix = if (json.has("suffix") && !json.isNull("suffix")) {
                    json.getString("suffix")
                } else null
                
                val chatFormat = if (json.has("chatFormat") && !json.isNull("chatFormat")) {
                    json.getString("chatFormat")
                } else null
                
                val tablistFormat = if (json.has("tablistFormat") && !json.isNull("tablistFormat")) {
                    json.getString("tablistFormat")
                } else null
                
                val nametagFormat = if (json.has("nametagFormat") && !json.isNull("nametagFormat")) {
                    json.getString("nametagFormat")
                } else null
                
                val isDefault = if (json.has("isDefault")) {
                    json.getBoolean("isDefault")
                } else false
                
                PermissionGroup(
                    name = name,
                    weight = weight,
                    permissions = permissions,
                    prefix = prefix,
                    suffix = suffix,
                    chatFormat = chatFormat,
                    tablistFormat = tablistFormat,
                    nametagFormat = nametagFormat,
                    isDefault = isDefault
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
