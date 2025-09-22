package de.cloudly.permissions.storage

import de.cloudly.permissions.models.PermissionGroup
import de.cloudly.permissions.models.PermissionUser
import de.cloudly.permissions.models.PermissionNode
import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.factory.StorageFactory
import de.cloudly.storage.config.StorageConfig
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Storage layer for the permission system.
 * Manages persistence of groups, users, and permission nodes using the CloudlyMC storage framework.
 */
class PermissionStorage(private val plugin: JavaPlugin) {
    
    private lateinit var groupRepository: DataRepository<PermissionGroup>
    private lateinit var userRepository: DataRepository<PermissionUser>
    private lateinit var nodeRepository: DataRepository<PermissionNode>
    
    // Cache for quick lookups
    private val groupCache = ConcurrentHashMap<String, PermissionGroup>()
    private val userCache = ConcurrentHashMap<String, PermissionUser>() // UUID as string
    private val nodeCache = ConcurrentHashMap<String, PermissionNode>()
    
    private var initialized = false
    
    /**
     * Initialize the permission storage system.
     * 
     * @return true if initialization was successful
     */
    fun initialize(): Boolean {
        return try {
            // Get storage configuration from the plugin
            val storageConfig = StorageConfig(plugin)
            storageConfig.load()
            
            // Create storage factory
            val storageFactory = StorageFactory(plugin)
            
            // Create repositories using the factory
            groupRepository = storageFactory.createRepository(
                repositoryName = "permission_groups",
                storageConfig = storageConfig,
                deserializer = { data, _ -> PermissionGroup.deserialize(data) }
            )
            
            userRepository = storageFactory.createRepository(
                repositoryName = "permission_users", 
                storageConfig = storageConfig,
                deserializer = { data, _ -> PermissionUser.deserialize(data) }
            )
            
            nodeRepository = storageFactory.createRepository(
                repositoryName = "permission_nodes",
                storageConfig = storageConfig,
                deserializer = { data, _ -> PermissionNode.deserialize(data) }
            )
            
            // Initialize repositories
            if (!groupRepository.initialize()) {
                plugin.logger.severe("Failed to initialize group repository")
                return false
            }
            
            if (!userRepository.initialize()) {
                plugin.logger.severe("Failed to initialize user repository")
                return false
            }
            
            if (!nodeRepository.initialize()) {
                plugin.logger.severe("Failed to initialize node repository")
                return false
            }
            
            // Load data into caches
            loadCaches()
            
            // Set initialized flag before ensuring base group
            initialized = true
            
            // Ensure base group exists
            ensureBaseGroupExists()
            
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Permission storage initialized successfully")
            }
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize permission storage", e)
            false
        }
    }
    
    /**
     * Load all data into memory caches for performance.
     */
    private fun loadCaches() {
        try {
            // Load groups
            groupRepository.getAll().forEach { (key, group) ->
                groupCache[key] = group
            }
            
            // Load users
            userRepository.getAll().forEach { (key, user) ->
                userCache[key] = user
            }
            
            // Load nodes
            nodeRepository.getAll().forEach { (key, node) ->
                nodeCache[key] = node
            }
            
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Loaded ${groupCache.size} groups, ${userCache.size} users, ${nodeCache.size} nodes into cache")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to load permission data into cache", e)
        }
    }
    
    /**
     * Ensure the base group exists in the system.
     */
    private fun ensureBaseGroupExists() {
        val baseGroupName = "base"
        if (!groupCache.containsKey(baseGroupName)) {
            val baseGroup = PermissionGroup.createBaseGroup()
            saveGroup(baseGroup)
            val debugMode = plugin.config.getBoolean("plugin.debug", false)
            if (debugMode) {
                plugin.logger.info("Created default base group")
            }
        }
    }
    
    // Group operations
    
    /**
     * Save a permission group.
     */
    fun saveGroup(group: PermissionGroup): Boolean {
        checkInitialized()
        return try {
            if (groupRepository.store(group.name, group)) {
                groupCache[group.name] = group
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save group: ${group.name}", e)
            false
        }
    }
    
    /**
     * Get a permission group by name.
     */
    fun getGroup(name: String): PermissionGroup? {
        checkInitialized()
        return groupCache[name]
    }
    
    /**
     * Get all permission groups.
     */
    fun getAllGroups(): Collection<PermissionGroup> {
        checkInitialized()
        return groupCache.values
    }
    
    /**
     * Delete a permission group.
     */
    fun deleteGroup(name: String): Boolean {
        checkInitialized()
        
        // Prevent deletion of base group
        if (name == "base") {
            plugin.logger.warning("Attempted to delete base group - operation denied")
            return false
        }
        
        return try {
            if (groupRepository.remove(name)) {
                groupCache.remove(name)
                
                // Remove this group from all users
                userCache.values.forEach { user ->
                    user.removeGroup(name)
                    saveUser(user)
                }
                
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to delete group: $name", e)
            false
        }
    }
    
    /**
     * Check if a group exists.
     */
    fun groupExists(name: String): Boolean {
        checkInitialized()
        return groupCache.containsKey(name)
    }
    
    // User operations
    
    /**
     * Save a permission user.
     */
    fun saveUser(user: PermissionUser): Boolean {
        checkInitialized()
        return try {
            val key = user.uuid.toString()
            if (userRepository.store(key, user)) {
                userCache[key] = user
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save user: ${user.username}", e)
            false
        }
    }
    
    /**
     * Get a permission user by UUID.
     */
    fun getUser(uuid: java.util.UUID): PermissionUser? {
        checkInitialized()
        return userCache[uuid.toString()]
    }
    
    /**
     * Get a permission user by username.
     */
    fun getUserByName(username: String): PermissionUser? {
        checkInitialized()
        return userCache.values.find { it.username.equals(username, ignoreCase = true) }
    }
    
    /**
     * Get all permission users.
     */
    fun getAllUsers(): Collection<PermissionUser> {
        checkInitialized()
        return userCache.values
    }
    
    /**
     * Delete a permission user.
     */
    fun deleteUser(uuid: java.util.UUID): Boolean {
        checkInitialized()
        return try {
            val key = uuid.toString()
            if (userRepository.remove(key)) {
                userCache.remove(key)
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to delete user: $uuid", e)
            false
        }
    }
    
    /**
     * Check if a user exists.
     */
    fun userExists(uuid: java.util.UUID): Boolean {
        checkInitialized()
        return userCache.containsKey(uuid.toString())
    }
    
    // Permission node operations
    
    /**
     * Save a permission node.
     */
    fun saveNode(node: PermissionNode): Boolean {
        checkInitialized()
        return try {
            if (nodeRepository.store(node.node, node)) {
                nodeCache[node.node] = node
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save permission node: ${node.node}", e)
            false
        }
    }
    
    /**
     * Get a permission node.
     */
    fun getNode(node: String): PermissionNode? {
        checkInitialized()
        return nodeCache[node]
    }
    
    /**
     * Get all permission nodes.
     */
    fun getAllNodes(): Collection<PermissionNode> {
        checkInitialized()
        return nodeCache.values
    }
    
    /**
     * Get all known permission node strings.
     */
    fun getAllPermissionNodes(): Set<String> {
        checkInitialized()
        return nodeCache.keys.toSet()
    }
    
    /**
     * Delete a permission node.
     */
    fun deleteNode(node: String): Boolean {
        checkInitialized()
        return try {
            if (nodeRepository.remove(node)) {
                nodeCache.remove(node)
                true
            } else false
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to delete permission node: $node", e)
            false
        }
    }
    
    // Utility operations
    
    /**
     * Clean up expired temporary permissions and groups for all users.
     * 
     * @return Number of users that had expired items cleaned
     */
    fun cleanupExpiredPermissions(): Int {
        checkInitialized()
        var cleanedUsers = 0
        val currentTime = System.currentTimeMillis() / 1000
        
        userCache.values.forEach { user ->
            val removedCount = user.cleanupExpired(currentTime)
            if (removedCount > 0) {
                saveUser(user)
                cleanedUsers++
                plugin.logger.fine("Cleaned $removedCount expired items for user ${user.username}")
            }
        }
        
        return cleanedUsers
    }
    
    /**
     * Get users who belong to a specific group.
     */
    fun getUsersInGroup(groupName: String): List<PermissionUser> {
        checkInitialized()
        val currentTime = System.currentTimeMillis() / 1000
        return userCache.values.filter { user ->
            user.getActiveGroups(currentTime).contains(groupName)
        }
    }
    
    /**
     * Refresh all caches from storage.
     */
    fun refreshCaches() {
        checkInitialized()
        groupCache.clear()
        userCache.clear()
        nodeCache.clear()
        loadCaches()
        plugin.logger.info("Permission caches refreshed")
    }
    
    /**
     * Backup all permission data.
     */
    fun backup(backupPath: String): Boolean {
        checkInitialized()
        return try {
            val groupBackup = groupRepository.backup("$backupPath/groups")
            val userBackup = userRepository.backup("$backupPath/users")
            val nodeBackup = nodeRepository.backup("$backupPath/nodes")
            
            groupBackup && userBackup && nodeBackup
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to backup permission data", e)
            false
        }
    }
    
    /**
     * Close the storage system and clean up resources.
     */
    fun close() {
        if (initialized) {
            groupRepository.close()
            userRepository.close()
            nodeRepository.close()
            
            groupCache.clear()
            userCache.clear()
            nodeCache.clear()
            
            initialized = false
            plugin.logger.info("Permission storage closed")
        }
    }
    
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("Permission storage has not been initialized")
        }
    }
}
