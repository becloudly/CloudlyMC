package de.cloudly.storage.core

import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Higher-level repository pattern implementation for managing collections of data.
 * This class provides a convenient interface for working with storage while handling
 * serialization, error handling, and caching.
 * 
 * @param T The type of data being managed (must implement Serializable)
 */
class DataRepository<T>(
    private val plugin: JavaPlugin,
    private val storage: DataStorage<String>,
    private val deserializer: (String, String) -> T?,
    private val repositoryName: String
) where T : Any, T : Serializable {
    
    private val cache = ConcurrentHashMap<String, T>()
    private var initialized = false
    
    /**
     * Initialize the repository.
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean {
        try {
            if (!storage.initialize()) {
                plugin.logger.severe("Failed to initialize storage for repository: $repositoryName")
                return false
            }
            
            // Load existing data into cache
            loadCache()
            initialized = true
            
            plugin.logger.info("Repository '$repositoryName' initialized successfully with ${cache.size} items")
            return true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize repository: $repositoryName", e)
            return false
        }
    }
    
    /**
     * Store an item in the repository.
     * @param key The unique identifier for the item
     * @param item The item to store
     * @return true if the item was stored successfully, false otherwise
     */
    fun store(key: String, item: T): Boolean {
        checkInitialized()
        
        try {
            val serializedData = item.serialize()
            if (storage.store(key, serializedData)) {
                cache[key] = item
                return true
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to store item with key '$key' in repository '$repositoryName'", e)
        }
        
        return false
    }
    
    /**
     * Retrieve an item from the repository.
     * @param key The unique identifier for the item
     * @return The item if found, null otherwise
     */
    fun retrieve(key: String): T? {
        checkInitialized()
        
        // Check cache first
        cache[key]?.let { return it }
        
        // Fallback to storage
        try {
            val serializedData = storage.retrieve(key) ?: return null
            val item = deserializer(serializedData, getTypeId()) ?: return null
            cache[key] = item
            return item
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to retrieve item with key '$key' from repository '$repositoryName'", e)
            return null
        }
    }
    
    /**
     * Remove an item from the repository.
     * @param key The unique identifier for the item to remove
     * @return true if the item was removed successfully, false otherwise
     */
    fun remove(key: String): Boolean {
        checkInitialized()
        
        try {
            if (storage.remove(key)) {
                cache.remove(key)
                return true
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to remove item with key '$key' from repository '$repositoryName'", e)
        }
        
        return false
    }
    
    /**
     * Check if an item exists in the repository.
     * @param key The unique identifier for the item
     * @return true if the item exists, false otherwise
     */
    fun exists(key: String): Boolean {
        checkInitialized()
        return cache.containsKey(key) || storage.exists(key)
    }
    
    /**
     * Get all items from the repository.
     * @return A map of all items with their keys
     */
    fun getAll(): Map<String, T> {
        checkInitialized()
        return cache.toMap()
    }
    
    /**
     * Get all keys from the repository.
     * @return A set of all keys
     */
    fun getAllKeys(): Set<String> {
        checkInitialized()
        return cache.keys.toSet()
    }
    
    /**
     * Get the count of items in the repository.
     * @return The number of items stored
     */
    fun count(): Long {
        checkInitialized()
        return cache.size.toLong()
    }
    
    /**
     * Clear all items from the repository.
     * @return true if the repository was cleared successfully, false otherwise
     */
    fun clear(): Boolean {
        checkInitialized()
        
        try {
            if (storage.clear()) {
                cache.clear()
                return true
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to clear repository '$repositoryName'", e)
        }
        
        return false
    }
    
    /**
     * Refresh the cache from storage.
     * This can be useful if data might have been modified externally.
     */
    fun refreshCache() {
        checkInitialized()
        cache.clear()
        loadCache()
    }
    
    /**
     * Close the repository and clean up resources.
     */
    fun close() {
        cache.clear()
        storage.close()
        initialized = false
    }
    
    /**
     * Perform a backup of the repository.
     * @param backupPath The path where the backup should be stored
     * @return true if the backup was successful, false otherwise
     */
    fun backup(backupPath: String): Boolean {
        checkInitialized()
        return storage.backup(backupPath)
    }
    
    /**
     * Restore the repository from a backup.
     * @param backupPath The path to the backup file
     * @return true if the restore was successful, false otherwise
     */
    fun restore(backupPath: String): Boolean {
        checkInitialized()
        
        try {
            if (storage.restore(backupPath)) {
                refreshCache()
                return true
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to restore repository '$repositoryName' from backup", e)
        }
        
        return false
    }
    
    /**
     * Load data from storage into the cache.
     */
    private fun loadCache() {
        try {
            val allData = storage.getAll()
            cache.clear()
            
            allData.forEach { (key, serializedData) ->
                try {
                    val item = deserializer(serializedData, getTypeId())
                    if (item != null) {
                        cache[key] = item
                    } else {
                        plugin.logger.warning("Failed to deserialize item with key '$key' in repository '$repositoryName'")
                    }
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Failed to deserialize item with key '$key' in repository '$repositoryName'", e)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load cache for repository '$repositoryName'", e)
        }
    }
    
    /**
     * Check if the repository has been initialized.
     * @throws StorageNotInitializedException if not initialized
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw StorageNotInitializedException("Repository '$repositoryName' has not been initialized")
        }
    }
    
    /**
     * Get the type ID for the data type being managed.
     * This is used for deserialization.
     */
    private fun getTypeId(): String {
        // This is a bit of a hack, but we need to get the type ID somehow
        // In practice, this would be passed to the constructor or determined differently
        return repositoryName
    }
}
