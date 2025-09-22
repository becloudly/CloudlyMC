package de.cloudly.permissions.services

import de.cloudly.permissions.PermissionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Service for integrating with Bukkit's permission system.
 * Ensures CloudlyMC permissions take precedence over default Bukkit permissions.
 */
class BukkitPermissionIntegration(
    private val plugin: JavaPlugin,
    private val permissionManager: PermissionManager
) {
    
    private var initialized = false
    private val registeredPermissions = mutableSetOf<String>()
    
    /**
     * Initialize the Bukkit permission integration.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Register essential permissions that we want to manage
            registerEssentialPermissions()
            
            // Set up permission defaults
            setupPermissionDefaults()
            
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Bukkit permission integration initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize Bukkit permission integration", e)
            false
        }
    }
    
    /**
     * Register essential permissions with Bukkit.
     * These are permissions that our system should always manage.
     */
    private fun registerEssentialPermissions() {
        val essentialPermissions = listOf(
            "cloudly.permissions.admin" to "Access to all permission management commands",
            "cloudly.permissions.group.create" to "Create new permission groups",
            "cloudly.permissions.group.delete" to "Delete permission groups",
            "cloudly.permissions.group.edit" to "Edit permission groups",
            "cloudly.permissions.group.list" to "List all permission groups",
            "cloudly.permissions.user.edit" to "Edit user permissions and groups",
            "cloudly.permissions.user.view" to "View user permissions and groups",
            "cloudly.permissions.reload" to "Reload the permission system",
            "cloudly.permissions.info" to "View permission system information"
        )
        
        essentialPermissions.forEach { (permissionNode, description) ->
            registerPermission(permissionNode, description, PermissionDefault.OP)
        }
    }
    
    /**
     * Register a permission with Bukkit if it doesn't already exist.
     * 
     * @param name The permission name
     * @param description The permission description
     * @param defaultValue The default permission value
     */
    private fun registerPermission(
        name: String, 
        description: String, 
        defaultValue: PermissionDefault = PermissionDefault.FALSE
    ) {
        try {
            val pluginManager = Bukkit.getPluginManager()
            
            // Check if permission already exists
            if (pluginManager.getPermission(name) != null) {
                plugin.logger.fine("Permission already registered: $name")
                return
            }
            
            // Create and register the permission
            val permission = Permission(name, description, defaultValue)
            pluginManager.addPermission(permission)
            registeredPermissions.add(name)
            
            plugin.logger.fine("Registered permission: $name")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to register permission: $name", e)
        }
    }
    
    /**
     * Set up default permission values for various scenarios.
     */
    private fun setupPermissionDefaults() {
        try {
            // Set up default permissions for ops
            val pluginManager = Bukkit.getPluginManager()
            
            // Ensure ops have access to all cloudly permission commands
            registeredPermissions.forEach { permission ->
                val perm = pluginManager.getPermission(permission)
                if (perm != null && perm.default == PermissionDefault.FALSE) {
                    // Create a child permission for ops
                    val opPermission = Permission("$permission.op", "Op access to $permission", PermissionDefault.OP)
                    perm.children[opPermission.name] = true
                    pluginManager.addPermission(opPermission)
                }
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error setting up permission defaults", e)
        }
    }
    
    /**
     * Override a player's permission check to use our system.
     * This method can be used by other parts of the system to ensure
     * our permission system takes precedence.
     * 
     * @param player The player to check
     * @param permission The permission to check
     * @return The permission result from our system
     */
    fun checkPlayerPermission(player: Player, permission: String): Boolean {
        if (!initialized || !permissionManager.isEnabled()) {
            // Fallback to Bukkit's default behavior
            return player.hasPermission(permission)
        }
        
        return permissionManager.hasPermission(player, permission)
    }
    
    /**
     * Sync a player's permissions with Bukkit's permission system.
     * This applies all effective permissions to the player's permission attachments.
     * 
     * @param player The player to sync
     */
    fun syncPlayerPermissions(player: Player) {
        if (!initialized || !permissionManager.isEnabled()) {
            return
        }
        
        try {
            // Get all effective permissions for this player
            val effectivePermissions = permissionManager.getPlayerPermissions(player)
            
            // Remove existing attachments from our plugin
            player.effectivePermissions
                .filter { it.attachment?.plugin == plugin }
                .forEach { it.attachment?.remove() }
            
            // Create new attachment with all permissions
            val attachment = player.addAttachment(plugin)
            
            effectivePermissions.forEach { permission ->
                when {
                    permission.startsWith("-") -> {
                        // Negative permission
                        val actualPermission = permission.substring(1)
                        attachment.setPermission(actualPermission, false)
                    }
                    permission == "*" -> {
                        // Wildcard permission
                        attachment.setPermission("*", true)
                    }
                    else -> {
                        // Regular permission
                        attachment.setPermission(permission, true)
                    }
                }
            }
            
            // Recalculate player's permissions
            player.recalculatePermissions()
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error syncing permissions for player ${player.name}", e)
        }
    }
    
    /**
     * Sync permissions for all online players.
     */
    fun syncAllPlayerPermissions() {
        if (!initialized || !permissionManager.isEnabled()) {
            return
        }
        
        Bukkit.getOnlinePlayers().forEach { player ->
            syncPlayerPermissions(player)
        }
    }
    
    /**
     * Register additional permissions discovered during runtime.
     * 
     * @param permissions List of permission names to register
     */
    fun registerDiscoveredPermissions(permissions: Collection<String>) {
        if (!initialized) return
        
        permissions.forEach { permission ->
            registerPermission(
                name = permission,
                description = "Auto-discovered permission",
                defaultValue = PermissionDefault.FALSE
            )
        }
    }
    
    /**
     * Unregister permissions when plugins are disabled.
     * 
     * @param permissions List of permission names to unregister
     */
    fun unregisterPermissions(permissions: Collection<String>) {
        if (!initialized) return
        
        try {
            val pluginManager = Bukkit.getPluginManager()
            
            permissions.forEach { permission ->
                val perm = pluginManager.getPermission(permission)
                if (perm != null && registeredPermissions.contains(permission)) {
                    pluginManager.removePermission(perm)
                    registeredPermissions.remove(permission)
                    plugin.logger.fine("Unregistered permission: $permission")
                }
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error unregistering permissions", e)
        }
    }
    
    /**
     * Get all permissions registered by this integration.
     * 
     * @return Set of registered permission names
     */
    fun getRegisteredPermissions(): Set<String> {
        return registeredPermissions.toSet()
    }
    
    /**
     * Check if the integration is properly connected to Bukkit's permission system.
     * 
     * @return true if integration is working correctly
     */
    fun isIntegrationHealthy(): Boolean {
        if (!initialized) return false
        
        try {
            // Check if our essential permissions are still registered
            val pluginManager = Bukkit.getPluginManager()
            val missingPermissions = registeredPermissions.count { permission ->
                pluginManager.getPermission(permission) == null
            }
            
            if (missingPermissions > 0) {
                plugin.logger.warning("$missingPermissions registered permissions are missing from Bukkit")
                return false
            }
            
            return true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error checking integration health", e)
            return false
        }
    }
    
    /**
     * Get integration statistics.
     * 
     * @return Map with integration statistics
     */
    fun getIntegrationStats(): Map<String, Any> {
        return mapOf(
            "initialized" to initialized,
            "registeredPermissions" to registeredPermissions.size,
            "healthy" to isIntegrationHealthy(),
            "onlinePlayers" to Bukkit.getOnlinePlayers().size
        )
    }
    
    /**
     * Shutdown the Bukkit permission integration.
     */
    fun shutdown() {
        if (initialized) {
            try {
                // Remove all permissions we registered
                unregisterPermissions(registeredPermissions.toList())
                
                // Clean up any remaining attachments
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.effectivePermissions
                        .filter { it.attachment?.plugin == plugin }
                        .forEach { it.attachment?.remove() }
                    player.recalculatePermissions()
                }
                
                registeredPermissions.clear()
                initialized = false
                
                plugin.logger.info("Bukkit permission integration shutdown complete")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error during Bukkit integration shutdown", e)
            }
        }
    }
}
