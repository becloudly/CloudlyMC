package de.cloudly.permissions.services

import de.cloudly.permissions.models.PermissionNode
import de.cloudly.permissions.storage.PermissionStorage
import de.cloudly.permissions.utils.PermissionUtils
import de.cloudly.utils.SchedulerUtils
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Service for discovering and managing permission nodes from all plugins.
 * Automatically discovers permissions when plugins are loaded/unloaded.
 */
class PermissionDiscoveryService(
    private val plugin: JavaPlugin,
    private val storage: PermissionStorage
) : Listener {
    
    private val discoveredPermissions = ConcurrentHashMap<String, PermissionNode>()
    private var initialized = false
    
    /**
     * Initialize the permission discovery service.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Register event listeners for plugin enable/disable
            plugin.server.pluginManager.registerEvents(this, plugin)
            
            // Perform initial discovery of all currently loaded plugins
            performInitialDiscovery()
            
            // Schedule periodic rediscovery (every 30 minutes)
            SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
                performFullRediscovery()
            }, 36000L, 36000L) // 36000 ticks = 30 minutes
            
            initialized = true
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Permission discovery service initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize permission discovery service", e)
            false
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPluginEnable(event: PluginEnableEvent) {
        if (!initialized) return
        
        // Discover permissions from the newly enabled plugin
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                discoverPluginPermissions(event.plugin.name)
                plugin.logger.fine("Discovered permissions from newly enabled plugin: ${event.plugin.name}")
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error discovering permissions from plugin ${event.plugin.name}", e)
            }
        })
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPluginDisable(event: PluginDisableEvent) {
        if (!initialized) return
        
        // Remove permissions from the disabled plugin
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                removePluginPermissions(event.plugin.name)
                plugin.logger.fine("Removed permissions from disabled plugin: ${event.plugin.name}")
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error removing permissions from plugin ${event.plugin.name}", e)
            }
        })
    }
    
    /**
     * Perform initial discovery of permissions from all loaded plugins.
     */
    private fun performInitialDiscovery() {
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                val discoveredNodes = PermissionUtils.discoverServerPermissions(plugin)
                
                discoveredNodes.forEach { node ->
                    discoveredPermissions[node.node] = node
                    storage.saveNode(node)
                }
                
                plugin.logger.info("Initial permission discovery completed: ${discoveredNodes.size} permissions found")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error during initial permission discovery", e)
            }
        })
    }
    
    /**
     * Perform full rediscovery of all permissions.
     * This should be called periodically to catch any changes.
     */
    private fun performFullRediscovery() {
        try {
            val newDiscoveredNodes = PermissionUtils.discoverServerPermissions(plugin)
            var newPermissions = 0
            
            newDiscoveredNodes.forEach { node ->
                if (!discoveredPermissions.containsKey(node.node)) {
                    discoveredPermissions[node.node] = node
                    storage.saveNode(node)
                    newPermissions++
                }
            }
            
            if (newPermissions > 0) {
                plugin.logger.info("Rediscovery found $newPermissions new permissions")
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error during permission rediscovery", e)
        }
    }
    
    /**
     * Discover permissions from a specific plugin.
     * 
     * @param pluginName The name of the plugin
     */
    private fun discoverPluginPermissions(pluginName: String) {
        try {
            val targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName) ?: return
            var newPermissions = 0
            
            // Get permissions from plugin description
            targetPlugin.description.permissions.forEach { permission ->
                val node = PermissionNode(
                    node = permission.name,
                    description = permission.description,
                    plugin = pluginName,
                    isWildcard = permission.name.endsWith("*"),
                    category = PermissionNode.categorizeNode(permission.name)
                )
                
                if (!discoveredPermissions.containsKey(node.node)) {
                    discoveredPermissions[node.node] = node
                    storage.saveNode(node)
                    newPermissions++
                }
            }
            
            // Check for permissions registered directly with Bukkit by this plugin
            Bukkit.getPluginManager().permissions
                .filter { perm ->
                    // Try to determine if this permission belongs to the plugin
                    val extractedName = PermissionNode.extractPluginName(perm.name)
                    extractedName?.equals(pluginName, ignoreCase = true) == true
                }
                .forEach { permission ->
                    val node = PermissionNode(
                        node = permission.name,
                        description = permission.description,
                        plugin = pluginName,
                        isWildcard = permission.name.endsWith("*"),
                        category = PermissionNode.categorizeNode(permission.name)
                    )
                    
                    if (!discoveredPermissions.containsKey(node.node)) {
                        discoveredPermissions[node.node] = node
                        storage.saveNode(node)
                        newPermissions++
                    }
                }
            
            if (newPermissions > 0) {
                plugin.logger.fine("Discovered $newPermissions new permissions from plugin $pluginName")
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error discovering permissions from plugin $pluginName", e)
        }
    }
    
    /**
     * Remove permissions associated with a specific plugin.
     * 
     * @param pluginName The name of the plugin
     */
    private fun removePluginPermissions(pluginName: String) {
        try {
            val toRemove = discoveredPermissions.values
                .filter { it.plugin?.equals(pluginName, ignoreCase = true) == true }
                .map { it.node }
            
            toRemove.forEach { node ->
                discoveredPermissions.remove(node)
                storage.deleteNode(node)
            }
            
            if (toRemove.isNotEmpty()) {
                plugin.logger.fine("Removed ${toRemove.size} permissions from plugin $pluginName")
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error removing permissions from plugin $pluginName", e)
        }
    }
    
    /**
     * Get all discovered permission nodes.
     * 
     * @return Collection of all discovered permission nodes
     */
    fun getAllDiscoveredPermissions(): Collection<PermissionNode> {
        return discoveredPermissions.values.toList()
    }
    
    /**
     * Get discovered permissions for a specific plugin.
     * 
     * @param pluginName The name of the plugin
     * @return Collection of permissions for the plugin
     */
    fun getPluginPermissions(pluginName: String): Collection<PermissionNode> {
        return discoveredPermissions.values
            .filter { it.plugin?.equals(pluginName, ignoreCase = true) == true }
    }
    
    /**
     * Get discovered permissions by category.
     * 
     * @param category The category to filter by
     * @return Collection of permissions in the category
     */
    fun getPermissionsByCategory(category: String): Collection<PermissionNode> {
        return discoveredPermissions.values
            .filter { it.category?.equals(category, ignoreCase = true) == true }
    }
    
    /**
     * Search for permissions by name pattern.
     * 
     * @param pattern The pattern to search for
     * @return Collection of matching permissions
     */
    fun searchPermissions(pattern: String): Collection<PermissionNode> {
        val regex = pattern.replace("*", ".*").toRegex(RegexOption.IGNORE_CASE)
        return discoveredPermissions.values
            .filter { regex.matches(it.node) }
    }
    
    /**
     * Get statistics about discovered permissions.
     * 
     * @return Map with discovery statistics
     */
    fun getDiscoveryStats(): Map<String, Any> {
        val pluginCounts = discoveredPermissions.values
            .groupingBy { it.plugin ?: "unknown" }
            .eachCount()
        
        val categoryCounts = discoveredPermissions.values
            .groupingBy { it.category ?: "general" }
            .eachCount()
        
        return mapOf(
            "totalPermissions" to discoveredPermissions.size,
            "wildcardPermissions" to discoveredPermissions.values.count { it.isWildcard },
            "pluginCounts" to pluginCounts,
            "categoryCounts" to categoryCounts,
            "uniquePlugins" to pluginCounts.size
        )
    }
    
    /**
     * Manually trigger discovery for all plugins.
     * This can be called from commands or admin interfaces.
     */
    fun triggerFullDiscovery() {
        if (!initialized) return
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            performFullRediscovery()
        })
    }
    
    /**
     * Shutdown the discovery service.
     */
    fun shutdown() {
        if (initialized) {
            discoveredPermissions.clear()
            initialized = false
            plugin.logger.info("Permission discovery service shutdown")
        }
    }
}
