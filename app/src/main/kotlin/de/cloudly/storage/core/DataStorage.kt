package de.cloudly.storage.core

/**
 * Generic interface for data storage implementations.
 * This interface defines the operations that any data storage implementation must support.
 * 
 * @param T The type of data being stored
 */
interface DataStorage<T : Any> {
    
    /**
     * Initialize the storage system.
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean
    
    /**
     * Store or update an item in the storage.
     * @param key The unique identifier for the item
     * @param item The item to store
     * @return true if the item was stored successfully, false otherwise
     */
    fun store(key: String, item: T): Boolean
    
    /**
     * Retrieve an item from the storage by its key.
     * @param key The unique identifier for the item
     * @return The item if found, null otherwise
     */
    fun retrieve(key: String): T?
    
    /**
     * Remove an item from the storage.
     * @param key The unique identifier for the item to remove
     * @return true if the item was removed successfully, false otherwise
     */
    fun remove(key: String): Boolean
    
    /**
     * Check if an item exists in the storage.
     * @param key The unique identifier for the item
     * @return true if the item exists, false otherwise
     */
    fun exists(key: String): Boolean
    
    /**
     * Get all items from the storage.
     * @return A map of all items with their keys
     */
    fun getAll(): Map<String, T>
    
    /**
     * Get all keys from the storage.
     * @return A set of all keys
     */
    fun getAllKeys(): Set<String>
    
    /**
     * Get the count of items in the storage.
     * @return The number of items stored
     */
    fun count(): Long
    
    /**
     * Clear all items from the storage.
     * @return true if the storage was cleared successfully, false otherwise
     */
    fun clear(): Boolean
    
    /**
     * Store or update multiple items in the storage.
     * @param items A map of key-value pairs to store
     * @return true if all items were stored successfully, false otherwise
     */
    fun storeAll(items: Map<String, T>): Boolean
    
    /**
     * Remove multiple items from the storage.
     * @param keys A set of keys to remove
     * @return true if all items were removed successfully, false otherwise
     */
    fun removeAll(keys: Set<String>): Boolean
    
    /**
     * Close the storage connection and clean up resources.
     */
    fun close()
    
    /**
     * Perform a backup of the storage to the specified location.
     * @param backupPath The path where the backup should be stored
     * @return true if the backup was successful, false otherwise
     */
    fun backup(backupPath: String): Boolean
    
    /**
     * Restore the storage from a backup file.
     * @param backupPath The path to the backup file
     * @return true if the restore was successful, false otherwise
     */
    fun restore(backupPath: String): Boolean
}
