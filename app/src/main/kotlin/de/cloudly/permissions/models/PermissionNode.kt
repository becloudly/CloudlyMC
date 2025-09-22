package de.cloudly.permissions.models

import de.cloudly.storage.core.Serializable
import org.json.JSONObject

/**
 * Represents a permission node that has been discovered on the server.
 * This is used for administrative purposes to show available permissions.
 *
 * @property node The permission node (e.g., "essential.fly", "cloudly.admin")
 * @property description Optional description of what this permission does
 * @property plugin The name of the plugin that registered this permission
 * @property isWildcard Whether this is a wildcard permission (ending with *)
 * @property category Optional category for grouping permissions (e.g., "admin", "player")
 */
data class PermissionNode(
    val node: String,
    val description: String? = null,
    val plugin: String? = null,
    val isWildcard: Boolean = false,
    val category: String? = null
) : Serializable {
    
    /**
     * Check if this permission node matches another node.
     * Supports wildcard matching.
     * 
     * @param otherNode The permission node to check against
     * @return true if this node grants the other node
     */
    fun matches(otherNode: String): Boolean {
        return when {
            node == otherNode -> true
            node == "*" -> true
            isWildcard && node.endsWith("*") -> {
                val prefix = node.substring(0, node.length - 1)
                otherNode.startsWith(prefix)
            }
            else -> false
        }
    }
    
    /**
     * Get the parent node of this permission.
     * For example, "plugin.command.subcommand" -> "plugin.command"
     * 
     * @return The parent node, or null if this is a root node
     */
    fun getParentNode(): String? {
        val lastDot = node.lastIndexOf('.')
        return if (lastDot > 0) {
            node.substring(0, lastDot)
        } else null
    }
    
    /**
     * Get all parent nodes of this permission.
     * For example, "plugin.command.subcommand" -> ["plugin", "plugin.command"]
     * 
     * @return List of parent nodes from root to immediate parent
     */
    fun getParentNodes(): List<String> {
        val parents = mutableListOf<String>()
        val parts = node.split('.')
        
        for (i in 1 until parts.size) {
            parents.add(parts.subList(0, i).joinToString("."))
        }
        
        return parents
    }
    
    /**
     * Check if this node is a child of another node.
     * 
     * @param parentNode The potential parent node
     * @return true if this node is a child of the parent node
     */
    fun isChildOf(parentNode: String): Boolean {
        return node.startsWith("$parentNode.") && node != parentNode
    }
    
    /**
     * Get the depth of this permission node.
     * Root nodes have depth 1, "plugin.command" has depth 2, etc.
     * 
     * @return The depth of this node
     */
    fun getDepth(): Int {
        return node.count { it == '.' } + 1
    }
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("node", node)
        json.put("description", description)
        json.put("plugin", plugin)
        json.put("isWildcard", isWildcard)
        json.put("category", category)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "permission_node"
    
    companion object {
        /**
         * Create a wildcard permission node.
         * 
         * @param baseNode The base node (without the *)
         * @param description Optional description
         * @param plugin The plugin name
         * @param category Optional category
         * @return A wildcard PermissionNode
         */
        fun createWildcard(
            baseNode: String, 
            description: String? = null, 
            plugin: String? = null,
            category: String? = null
        ): PermissionNode {
            return PermissionNode(
                node = "$baseNode*",
                description = description ?: "Grants all permissions under $baseNode",
                plugin = plugin,
                isWildcard = true,
                category = category
            )
        }
        
        /**
         * Extract the plugin name from a permission node.
         * Assumes the first part of the node is the plugin name.
         * 
         * @param node The permission node
         * @return The extracted plugin name, or null if not determinable
         */
        fun extractPluginName(node: String): String? {
            val firstDot = node.indexOf('.')
            return if (firstDot > 0) {
                node.substring(0, firstDot)
            } else null
        }
        
        /**
         * Categorize a permission node based on common patterns.
         * 
         * @param node The permission node
         * @return A suggested category
         */
        fun categorizeNode(node: String): String {
            return when {
                node.contains("admin") -> "admin"
                node.contains("mod") || node.contains("moderator") -> "moderation"
                node.contains("command") -> "commands"
                node.contains("fly") || node.contains("teleport") || node.contains("tp") -> "movement"
                node.contains("creative") || node.contains("gamemode") -> "gamemode"
                node.contains("give") || node.contains("item") -> "items"
                node.contains("world") || node.contains("worldedit") -> "world"
                node.contains("economy") || node.contains("money") -> "economy"
                node.contains("chat") || node.contains("message") -> "chat"
                else -> "general"
            }
        }
        
        /**
         * Deserialize a PermissionNode from a JSON string.
         * 
         * @param data The JSON string data
         * @return The deserialized PermissionNode, or null if deserialization failed
         */
        fun deserialize(data: String): PermissionNode? {
            return try {
                val json = JSONObject(data)
                
                val node = json.getString("node")
                
                val description = if (json.has("description") && !json.isNull("description")) {
                    json.getString("description")
                } else null
                
                val plugin = if (json.has("plugin") && !json.isNull("plugin")) {
                    json.getString("plugin")
                } else null
                
                val isWildcard = if (json.has("isWildcard")) {
                    json.getBoolean("isWildcard")
                } else node.endsWith("*")
                
                val category = if (json.has("category") && !json.isNull("category")) {
                    json.getString("category")
                } else null
                
                PermissionNode(
                    node = node,
                    description = description,
                    plugin = plugin,
                    isWildcard = isWildcard,
                    category = category
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
