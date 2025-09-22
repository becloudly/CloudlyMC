package de.cloudly.permissions

import de.cloudly.permissions.storage.PermissionStorage
import de.cloudly.permissions.services.GroupService
import de.cloudly.permissions.services.UserService
import de.cloudly.permissions.services.PermissionResolver
import de.cloudly.permissions.services.PermissionDiscoveryService
import de.cloudly.permissions.services.BukkitPermissionIntegration
import de.cloudly.permissions.services.FormattingService
import de.cloudly.permissions.services.TablistManager
import de.cloudly.permissions.services.NametagManager
import de.cloudly.permissions.listeners.PlayerPermissionListener
import de.cloudly.permissions.listeners.ChatFormattingListener
import de.cloudly.permissions.commands.PermissionCommand
import de.cloudly.permissions.commands.GroupCommand
import de.cloudly.permissions.commands.UserCommand
import de.cloudly.permissions.utils.PermissionUtils
import de.cloudly.utils.SchedulerUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Main permission manager that coordinates all permission-related operations.
 * This is the primary entry point for the permission system.
 */
class PermissionManager(private val plugin: JavaPlugin) {
    
    private lateinit var storage: PermissionStorage
    private lateinit var groupService: GroupService
    private lateinit var userService: UserService
    private lateinit var permissionResolver: PermissionResolver
    private lateinit var discoveryService: PermissionDiscoveryService
    private lateinit var bukkitIntegration: BukkitPermissionIntegration
    private lateinit var playerListener: PlayerPermissionListener
    private lateinit var formattingService: FormattingService
    private lateinit var tablistManager: TablistManager
    private lateinit var nametagManager: NametagManager
    private lateinit var chatListener: ChatFormattingListener
    
    // Permission cache with TTL
    private val permissionCache = ConcurrentHashMap<String, CachedPermission>()
    private val cacheTimeout = 300000L // 5 minutes in milliseconds
    
    private var initialized = false
    private var enabled = false
    
    /**
     * Initialize the permission system.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        try {
            val configManager = (plugin as de.cloudly.CloudlyPaper).getConfigManager()
            val debugMode = configManager.getBoolean("plugin.debug", false)
            
            if (debugMode) {
                plugin.logger.info("Initializing CloudlyMC Permission System...")
            }
            
            // Check if permission system is enabled in config
            enabled = configManager.getBoolean("permissions.enabled", true)
            
            if (!enabled) {
                if (debugMode) {
                    plugin.logger.info("Permission system is disabled in configuration")
                }
                return true // Return true as it's disabled by choice, not error
            }
            
            // Initialize storage
            storage = PermissionStorage(plugin)
            if (!storage.initialize()) {
                plugin.logger.severe("Failed to initialize permission storage")
                return false
            }
            
            // Initialize services
            groupService = GroupService(plugin, storage)
            userService = UserService(plugin, storage)
            permissionResolver = PermissionResolver(plugin, storage, groupService)
            
            if (!groupService.initialize()) {
                plugin.logger.severe("Failed to initialize group service")
                return false
            }
            
            if (!userService.initialize()) {
                plugin.logger.severe("Failed to initialize user service")
                return false
            }
            
            if (!permissionResolver.initialize()) {
                plugin.logger.severe("Failed to initialize permission resolver")
                return false
            }
            
            // Initialize Phase 3 components (Bukkit integration)
            bukkitIntegration = BukkitPermissionIntegration(plugin, this)
            if (!bukkitIntegration.initialize()) {
                plugin.logger.severe("Failed to initialize Bukkit permission integration")
                return false
            }
            
            discoveryService = PermissionDiscoveryService(plugin, storage)
            if (!discoveryService.initialize()) {
                plugin.logger.severe("Failed to initialize permission discovery service")
                return false
            }
            
            // Initialize player listener for permission management
            playerListener = PlayerPermissionListener(plugin, this)
            plugin.server.pluginManager.registerEvents(playerListener, plugin)
            
            // Initialize Phase 4 components (Formatting System)
            formattingService = FormattingService(plugin, this)
            if (!formattingService.initialize()) {
                plugin.logger.severe("Failed to initialize formatting service")
                return false
            }
            
            tablistManager = TablistManager(plugin, this, formattingService)
            if (!tablistManager.initialize()) {
                plugin.logger.severe("Failed to initialize tablist manager")
                return false
            }
            
            nametagManager = NametagManager(plugin, this, formattingService)
            if (!nametagManager.initialize()) {
                plugin.logger.severe("Failed to initialize nametag manager")
                return false
            }
            
            // Initialize chat formatting listener
            chatListener = ChatFormattingListener(plugin, this, formattingService)
            plugin.server.pluginManager.registerEvents(chatListener, plugin)
            
            // Initialize Phase 5 components (Commands)
            registerCommands()
            
            // Start background tasks
            startBackgroundTasks()
            
            // Sync permissions for any already online players
            bukkitIntegration.syncAllPlayerPermissions()
            
            // Apply formatting to all online players
            tablistManager.updateAllPlayerTablists()
            nametagManager.updateAllPlayerNametags()
            
            initialized = true
            if (debugMode) {
                plugin.logger.info("CloudlyMC Permission System initialized successfully")
            }
            return true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize permission system", e)
            return false
        }
    }
    
    /**
     * Check if the permission system is enabled and initialized.
     */
    fun isEnabled(): Boolean = enabled && initialized
    
    /**
     * Get the group service instance.
     */
    fun getGroupService(): GroupService {
        checkEnabled()
        return groupService
    }
    
    /**
     * Get the user service instance.
     */
    fun getUserService(): UserService {
        checkEnabled()
        return userService
    }
    
    /**
     * Get the permission resolver instance.
     */
    fun getPermissionResolver(): PermissionResolver {
        checkEnabled()
        return permissionResolver
    }
    
    /**
     * Get the discovery service instance.
     */
    fun getDiscoveryService(): PermissionDiscoveryService {
        checkEnabled()
        return discoveryService
    }
    
    /**
     * Get the Bukkit integration service instance.
     */
    fun getBukkitIntegration(): BukkitPermissionIntegration {
        checkEnabled()
        return bukkitIntegration
    }
    
    /**
     * Get the storage instance.
     */
    fun getStorage(): PermissionStorage {
        checkEnabled()
        return storage
    }
    
    /**
     * Get the formatting service instance.
     */
    fun getFormattingService(): FormattingService {
        checkEnabled()
        return formattingService
    }
    
    /**
     * Get the tablist manager instance.
     */
    fun getTablistManager(): TablistManager {
        checkEnabled()
        return tablistManager
    }
    
    /**
     * Get the nametag manager instance.
     */
    fun getNametagManager(): NametagManager {
        checkEnabled()
        return nametagManager
    }
    
    /**
     * Check if a player has a specific permission.
     * This is the main permission check method with caching.
     * 
     * @param player The player to check
     * @param permission The permission node to check
     * @return true if the player has the permission
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        if (!isEnabled()) {
            // Fallback to Bukkit's permission system if disabled
            return player.hasPermission(permission)
        }
        
        return hasPermission(player.uniqueId, permission)
    }
    
    /**
     * Check if a player has a specific permission by UUID.
     * 
     * @param playerUuid The UUID of the player
     * @param permission The permission node to check
     * @return true if the player has the permission
     */
    fun hasPermission(playerUuid: UUID, permission: String): Boolean {
        if (!isEnabled()) {
            return false
        }
        
        val cacheKey = "$playerUuid:$permission"
        
        // Check cache first
        val cached = permissionCache[cacheKey]
        if (cached != null && !cached.isExpired()) {
            return cached.hasPermission
        }
        
        // Calculate permission
        val hasPermission = permissionResolver.hasPermission(playerUuid, permission)
        
        // Cache result
        permissionCache[cacheKey] = CachedPermission(hasPermission, System.currentTimeMillis() + cacheTimeout)
        
        return hasPermission
    }
    
    /**
     * Get all effective permissions for a player.
     * 
     * @param player The player
     * @return Set of all permissions the player has
     */
    fun getPlayerPermissions(player: Player): Set<String> {
        if (!isEnabled()) {
            return emptySet()
        }
        
        return permissionResolver.getEffectivePermissions(player.uniqueId)
    }
    
    /**
     * Get the highest weight group for a player.
     * 
     * @param player The player
     * @return The highest weight group name, or null if no groups
     */
    fun getPlayerPrimaryGroup(player: Player): String? {
        if (!isEnabled()) {
            return null
        }
        
        return permissionResolver.getPrimaryGroup(player.uniqueId)
    }
    
    /**
     * Get the prefix for a player based on their highest weight group.
     * 
     * @param player The player
     * @return The prefix string, or null if no prefix
     */
    fun getPlayerPrefix(player: Player): String? {
        if (!isEnabled()) {
            return null
        }
        
        return permissionResolver.getPlayerPrefix(player.uniqueId)
    }
    
    /**
     * Get the suffix for a player based on their highest weight group.
     * 
     * @param player The player
     * @return The suffix string, or null if no suffix
     */
    fun getPlayerSuffix(player: Player): String? {
        if (!isEnabled()) {
            return null
        }
        
        return permissionResolver.getPlayerSuffix(player.uniqueId)
    }
    
    /**
     * Clear permission cache for a specific player.
     * 
     * @param playerUuid The UUID of the player
     */
    fun clearPlayerCache(playerUuid: UUID) {
        if (!isEnabled()) return
        
        val keysToRemove = permissionCache.keys.filter { it.startsWith("$playerUuid:") }
        keysToRemove.forEach { permissionCache.remove(it) }
    }
    
    /**
     * Clear all permission caches.
     */
    fun clearAllCaches() {
        if (!isEnabled()) return
        
        permissionCache.clear()
        permissionResolver.clearCache()
    }
    
    /**
     * Refresh permissions for a specific player.
     * Should be called when a player's permissions change.
     * 
     * @param player The player to refresh
     */
    fun refreshPlayerPermissions(player: Player) {
        if (!isEnabled()) return
        
        clearPlayerCache(player.uniqueId)
        bukkitIntegration.syncPlayerPermissions(player)
        playerListener.refreshPlayerPermissions(player)
        
        // Refresh formatting for the player
        formattingService.clearPlayerCache(player)
        tablistManager.updatePlayerTablist(player)
        nametagManager.updatePlayerNametag(player)
    }
    
    /**
     * Refresh permissions for all online players.
     * Should be called when group permissions change.
     */
    fun refreshAllPlayerPermissions() {
        if (!isEnabled()) return
        
        clearAllCaches()
        bukkitIntegration.syncAllPlayerPermissions()
        playerListener.refreshAllPlayerPermissions()
        
        // Refresh formatting for all players
        formattingService.clearAllCaches()
        tablistManager.updateAllPlayerTablists()
        nametagManager.updateAllPlayerNametags()
    }
    
    /**
     * Get comprehensive system information for debugging.
     * 
     * @return Map with system information
     */
    fun getSystemInfo(): Map<String, Any> {
        if (!isEnabled()) {
            return mapOf("enabled" to false)
        }
        
        return mapOf(
            "enabled" to true,
            "initialized" to initialized,
            "onlinePlayers" to plugin.server.onlinePlayers.size,
            "permissionCacheSize" to permissionCache.size,
            "groups" to storage.getAllGroups().size,
            "users" to storage.getAllUsers().size,
            "discoveredPermissions" to storage.getAllNodes().size,
            "cacheStats" to permissionResolver.getCacheStats(),
            "discoveryStats" to discoveryService.getDiscoveryStats(),
            "bukkitIntegrationStats" to bukkitIntegration.getIntegrationStats(),
            "formattingStats" to formattingService.getFormattingStats(),
            "tablistStats" to tablistManager.getTablistStats(),
            "nametagStats" to nametagManager.getNametagStats()
        )
    }
    
    /**
     * Reload the permission system.
     * 
     * @return true if reload was successful
     */
    fun reload(): Boolean {
        if (!isEnabled()) return false
        
        try {
            plugin.logger.info("Reloading permission system...")
            
            // Clear caches
            clearAllCaches()
            
            // Refresh storage caches
            storage.refreshCaches()
            
            // Reinitialize services
            groupService.reload()
            userService.reload()
            permissionResolver.reload()
            
            // Refresh Bukkit integration
            bukkitIntegration.syncAllPlayerPermissions()
            
            // Trigger permission rediscovery
            discoveryService.triggerFullDiscovery()
            
            plugin.logger.info("Permission system reloaded successfully")
            return true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload permission system", e)
            return false
        }
    }
    
    /**
     * Handle player join - set up permissions and ensure base group.
     * 
     * @param player The player who joined
     */
    fun onPlayerJoin(player: Player) {
        if (!isEnabled()) return
        
        SchedulerUtils.runTaskAsynchronously(plugin, Runnable {
            try {
                // Ensure user exists and has base group
                userService.ensureUserExists(player.uniqueId, player.name)
                
                // Sync permissions with Bukkit system
                bukkitIntegration.syncPlayerPermissions(player)
                
                // Update formatting for the player
                formattingService.clearPlayerCache(player)
                tablistManager.updatePlayerTablist(player)
                nametagManager.updatePlayerNametag(player)
                
                // Pre-warm cache with common permissions
                val commonPermissions = listOf(
                    "minecraft.command.me",
                    "minecraft.command.tell",
                    "minecraft.command.help"
                )
                
                commonPermissions.forEach { permission ->
                    hasPermission(player, permission)
                }
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error handling player join for ${player.name}", e)
            }
        })
    }
    
    /**
     * Handle player quit - clean up caches.
     * 
     * @param player The player who quit
     */
    fun onPlayerQuit(player: Player) {
        if (!isEnabled()) return
        
        // Clear player's permission cache
        clearPlayerCache(player.uniqueId)
        
        // Clear formatting data
        formattingService.clearPlayerCache(player)
        tablistManager.removePlayer(player)
        nametagManager.removePlayerFromTeam(player)
    }
    
    /**
     * Start background tasks for maintenance.
     */
    private fun startBackgroundTasks() {
        // Cache cleanup task (every 10 minutes)
        SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
            cleanupExpiredCache()
        }, 12000L, 12000L) // 12000 ticks = 10 minutes
        
        // Expired permission cleanup task (every hour)
        SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
            storage.cleanupExpiredPermissions()
        }, 72000L, 72000L) // 72000 ticks = 1 hour
    }
    
    /**
     * Clean up expired entries from the permission cache.
     */
    private fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = permissionCache.entries
            .filter { it.value.isExpired(currentTime) }
            .map { it.key }
        
        expiredKeys.forEach { permissionCache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            plugin.logger.fine("Cleaned ${expiredKeys.size} expired permission cache entries")
        }
    }
    
    /**
     * Discover permissions from all loaded plugins.
     */
    private fun discoverServerPermissions() {
        // This is now handled by the PermissionDiscoveryService
        // during its initialization and plugin enable/disable events
        plugin.logger.fine("Permission discovery is handled by DiscoveryService")
    }
    
    /**
     * Register permission system commands.
     * Commands are now handled through the main /cloudly command.
     */
    private fun registerCommands() {
        try {
            // Permission commands are now handled through /cloudly perms
            plugin.logger.info("Permission system commands integrated with /cloudly perms")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error registering permission commands", e)
        }
    }
    
    /**
     * Shutdown the permission system.
     */
    fun shutdown() {
        if (initialized) {
            try {
                // Clear caches
                clearAllCaches()
                
                // Close Phase 4 services (Formatting)
                if (::formattingService.isInitialized) {
                    formattingService.shutdown()
                }
                
                if (::tablistManager.isInitialized) {
                    tablistManager.shutdown()
                }
                
                if (::nametagManager.isInitialized) {
                    nametagManager.shutdown()
                }
                
                // Close Phase 3 services
                if (::discoveryService.isInitialized) {
                    discoveryService.shutdown()
                }
                
                if (::bukkitIntegration.isInitialized) {
                    bukkitIntegration.shutdown()
                }
                
                // Close core services
                if (::permissionResolver.isInitialized) {
                    permissionResolver.shutdown()
                }
                
                if (::userService.isInitialized) {
                    userService.shutdown()
                }
                
                if (::groupService.isInitialized) {
                    groupService.shutdown()
                }
                
                // Close storage
                if (::storage.isInitialized) {
                    storage.close()
                }
                
                initialized = false
                plugin.logger.info("Permission system shutdown complete")
                
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error during permission system shutdown", e)
            }
        }
    }
    
    private fun checkEnabled() {
        if (!isEnabled()) {
            throw IllegalStateException("Permission system is not enabled or initialized")
        }
    }
    
    /**
     * Represents a cached permission result with expiry time.
     */
    private data class CachedPermission(
        val hasPermission: Boolean,
        val expiryTime: Long
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime > expiryTime
        }
    }
}
