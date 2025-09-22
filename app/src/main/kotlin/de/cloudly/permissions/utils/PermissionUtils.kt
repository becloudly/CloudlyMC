package de.cloudly.permissions.utils

import de.cloudly.permissions.models.PermissionNode
import org.bukkit.Bukkit
import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin

/**
 * Utility functions for permission management.
 */
object PermissionUtils {
    
    /**
     * Discover all permission nodes from loaded plugins.
     * 
     * @param plugin The plugin instance
     * @return Set of discovered permission nodes
     */
    fun discoverServerPermissions(plugin: JavaPlugin): Set<PermissionNode> {
        val discoveredNodes = mutableSetOf<PermissionNode>()
        
        try {
            // Get all registered permissions from Bukkit
            val pluginManager = Bukkit.getPluginManager()
            
            // Get permissions from all plugins
            pluginManager.plugins.forEach { loadedPlugin ->
                loadedPlugin.description.permissions.forEach { permission ->
                    val node = PermissionNode(
                        node = permission.name,
                        description = permission.description,
                        plugin = loadedPlugin.name,
                        isWildcard = permission.name.endsWith("*"),
                        category = PermissionNode.categorizeNode(permission.name)
                    )
                    discoveredNodes.add(node)
                }
            }
            
            // Get permissions registered directly with Bukkit
            pluginManager.permissions.forEach { permission ->
                val pluginName = findPluginForPermission(permission)
                val node = PermissionNode(
                    node = permission.name,
                    description = permission.description,
                    plugin = pluginName,
                    isWildcard = permission.name.endsWith("*"),
                    category = PermissionNode.categorizeNode(permission.name)
                )
                discoveredNodes.add(node)
            }
            
            // Add common Minecraft permissions that might not be registered
            addMinecraftPermissions(discoveredNodes)
            
            plugin.logger.info("Discovered ${discoveredNodes.size} permission nodes")
            
        } catch (e: Exception) {
            plugin.logger.warning("Error discovering permissions: ${e.message}")
        }
        
        return discoveredNodes
    }
    
    /**
     * Find which plugin registered a specific permission.
     * 
     * @param permission The permission to check
     * @return The plugin name, or "unknown" if not found
     */
    private fun findPluginForPermission(permission: Permission): String {
        // Try to extract plugin name from permission node
        val extractedName = PermissionNode.extractPluginName(permission.name)
        if (extractedName != null) {
            val plugin = Bukkit.getPluginManager().getPlugin(extractedName)
            if (plugin != null) {
                return plugin.name
            }
        }
        
        // Check if any plugin description contains this permission
        Bukkit.getPluginManager().plugins.forEach { plugin ->
            if (plugin.description.permissions.any { it.name == permission.name }) {
                return plugin.name
            }
        }
        
        return "unknown"
    }
    
    /**
     * Add common Minecraft permissions that might not be automatically registered.
     * 
     * @param nodes The set to add permissions to
     */
    private fun addMinecraftPermissions(nodes: MutableSet<PermissionNode>) {
        val minecraftPermissions = listOf(
            "minecraft.command.me" to "Use /me command",
            "minecraft.command.tell" to "Use /tell command",
            "minecraft.command.help" to "Use /help command",
            "minecraft.command.list" to "Use /list command",
            "minecraft.command.seed" to "Use /seed command",
            "minecraft.command.time" to "Use /time command",
            "minecraft.command.weather" to "Use /weather command",
            "minecraft.command.gamemode" to "Use /gamemode command",
            "minecraft.command.give" to "Use /give command",
            "minecraft.command.tp" to "Use /tp command",
            "minecraft.command.teleport" to "Use /teleport command",
            "minecraft.command.kill" to "Use /kill command",
            "minecraft.command.kick" to "Use /kick command",
            "minecraft.command.ban" to "Use /ban command",
            "minecraft.command.op" to "Use /op command",
            "minecraft.command.deop" to "Use /deop command",
            "minecraft.command.whitelist" to "Use /whitelist command",
            "minecraft.command.stop" to "Use /stop command",
            "minecraft.command.reload" to "Use /reload command"
        )
        
        minecraftPermissions.forEach { (permission, description) ->
            // Only add if not already present
            if (nodes.none { it.node == permission }) {
                nodes.add(
                    PermissionNode(
                        node = permission,
                        description = description,
                        plugin = "minecraft",
                        isWildcard = false,
                        category = "minecraft"
                    )
                )
            }
        }
    }
    
    /**
     * Parse a duration string into seconds.
     * Supports formats like: 1d, 2h, 30m, 45s
     * 
     * @param durationStr The duration string
     * @return Duration in seconds, or null if invalid
     */
    fun parseDuration(durationStr: String): Long? {
        if (durationStr.isBlank()) return null
        
        return try {
            val regex = Regex("(\\d+)([dhms])")
            val matches = regex.findAll(durationStr.lowercase())
            
            var totalSeconds = 0L
            
            for (match in matches) {
                val amount = match.groupValues[1].toLong()
                val unit = match.groupValues[2]
                
                totalSeconds += when (unit) {
                    "d" -> amount * 86400 // days to seconds
                    "h" -> amount * 3600  // hours to seconds
                    "m" -> amount * 60    // minutes to seconds
                    "s" -> amount         // seconds
                    else -> 0
                }
            }
            
            if (totalSeconds > 0) totalSeconds else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format a duration in seconds to a human-readable string.
     * 
     * @param seconds The duration in seconds
     * @return Formatted duration string
     */
    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0s"
        
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        
        val parts = mutableListOf<String>()
        
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (remainingSeconds > 0) parts.add("${remainingSeconds}s")
        
        return parts.joinToString(" ")
    }
    
    /**
     * Check if a permission node is valid.
     * 
     * @param permission The permission node to validate
     * @return true if the permission is valid
     */
    fun isValidPermission(permission: String): Boolean {
        if (permission.isBlank()) return false
        
        // Basic validation - no spaces, must contain at least one dot (except for *)
        if (permission.contains(" ")) return false
        if (permission == "*") return true
        if (!permission.contains(".") && !permission.endsWith("*")) return false
        
        // Check for valid characters (alphanumeric, dots, dashes, underscores)
        val validChars = Regex("^[a-zA-Z0-9._\\-*]+$")
        return validChars.matches(permission)
    }
    
    /**
     * Check if a group name is valid.
     * 
     * @param groupName The group name to validate
     * @return true if the group name is valid
     */
    fun isValidGroupName(groupName: String): Boolean {
        if (groupName.isBlank()) return false
        if (groupName.length > 50) return false // Reasonable length limit
        
        // Check for valid characters (alphanumeric, dashes, underscores)
        val validChars = Regex("^[a-zA-Z0-9_\\-]+$")
        return validChars.matches(groupName)
    }
    
    /**
     * Normalize a permission node (lowercase, trim).
     * 
     * @param permission The permission to normalize
     * @return The normalized permission
     */
    fun normalizePermission(permission: String): String {
        return permission.lowercase().trim()
    }
    
    /**
     * Normalize a group name (lowercase, trim).
     * 
     * @param groupName The group name to normalize
     * @return The normalized group name
     */
    fun normalizeGroupName(groupName: String): String {
        return groupName.lowercase().trim()
    }
    
    /**
     * Calculate the expiry timestamp for a duration from now.
     * 
     * @param durationStr The duration string (e.g., "1d", "2h")
     * @return The expiry timestamp in epoch seconds, or null if invalid
     */
    fun calculateExpiryTimestamp(durationStr: String): Long? {
        val durationSeconds = parseDuration(durationStr) ?: return null
        return System.currentTimeMillis() / 1000 + durationSeconds
    }
}
