package de.cloudly.storage.impl.json

import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.core.StorageException
import de.cloudly.storage.core.StorageOperationException
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level

/**
 * JSON implementation of the DataStorage interface.
 * Stores data as JSON objects in a single JSON file.
 */
class JsonDataStorage(
    private val plugin: JavaPlugin,
    private val filePath: String,
    private val prettyPrint: Boolean = true
) : DataStorage<String> {
    
    private val data = ConcurrentHashMap<String, String>()
    private val file: File
    private var initialized = false
    private val pendingWrites = AtomicBoolean(false)
    private val writeLock = ReentrantReadWriteLock()
    private var flushTask: BukkitTask? = null
    
    init {
        // Resolve the file path relative to the plugin's data folder
        file = File(plugin.dataFolder, filePath)
    }
    
    override fun initialize(): Boolean {
        return try {
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            
            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile()
                // Write an empty JSON object to the file
                Files.write(file.toPath(), "{}".toByteArray())
            }
            
            // Load existing data
            loadData()
            initialized = true
            
            // Start periodic flush task
            startPeriodicFlush()
            
            plugin.logger.info("JSON storage initialized: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize JSON storage: ${file.absolutePath}", e)
            false
        }
    }
    
    override fun store(key: String, item: String): Boolean {
        checkInitialized()
        
        return try {
            data[key] = item
            pendingWrites.set(true)
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to store item with key '$key' in JSON storage", e)
            false
        }
    }
    
    override fun retrieve(key: String): String? {
        checkInitialized()
        return data[key]
    }
    
    override fun remove(key: String): Boolean {
        checkInitialized()
        
        return try {
            val removed = data.remove(key) != null
            if (removed) {
                pendingWrites.set(true)
            }
            removed
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to remove item with key '$key' from JSON storage", e)
            false
        }
    }
    
    override fun exists(key: String): Boolean {
        checkInitialized()
        return data.containsKey(key)
    }
    
    override fun getAll(): Map<String, String> {
        checkInitialized()
        return data.toMap()
    }
    
    override fun getAllKeys(): Set<String> {
        checkInitialized()
        return data.keys.toSet()
    }
    
    override fun count(): Long {
        checkInitialized()
        return data.size.toLong()
    }
    
    override fun clear(): Boolean {
        checkInitialized()
        
        return try {
            data.clear()
            pendingWrites.set(true)
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to clear JSON storage", e)
            false
        }
    }
    
    override fun storeAll(items: Map<String, String>): Boolean {
        checkInitialized()
        
        return try {
            data.putAll(items)
            saveData()
            plugin.logger.fine("Stored ${items.size} items in JSON storage")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to store ${items.size} items in JSON storage", e)
            false
        }
    }
    
    override fun removeAll(keys: Set<String>): Boolean {
        checkInitialized()
        
        return try {
            var removedCount = 0
            keys.forEach { key ->
                if (data.remove(key) != null) {
                    removedCount++
                }
            }
            if (removedCount > 0) {
                saveData()
            }
            plugin.logger.fine("Removed $removedCount items from JSON storage")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to remove ${keys.size} items from JSON storage", e)
            false
        }
    }
    
    override fun close() {
        // Cancel periodic flush task
        flushTask?.cancel()
        flushTask = null
        
        // Save data before closing if there are pending writes
        if (initialized) {
            try {
                if (pendingWrites.compareAndSet(true, false)) {
                    saveData()
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Failed to save data during JSON storage close", e)
            }
        }
        
        data.clear()
        initialized = false
    }
    
    override fun backup(backupPath: String): Boolean {
        checkInitialized()
        
        return try {
            // Flush pending writes before backup
            if (pendingWrites.compareAndSet(true, false)) {
                saveData()
            }
            
            val backupFile = File(plugin.dataFolder, backupPath)
            backupFile.parentFile?.mkdirs()
            
            // Use read lock to ensure we don't backup during a write
            writeLock.readLock().lock()
            try {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                plugin.logger.info("JSON storage backed up to: ${backupFile.absolutePath}")
                true
            } finally {
                writeLock.readLock().unlock()
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to backup JSON storage to '$backupPath'", e)
            false
        }
    }
    
    override fun restore(backupPath: String): Boolean {
        checkInitialized()
        
        return try {
            val backupFile = File(plugin.dataFolder, backupPath)
            if (!backupFile.exists()) {
                plugin.logger.warning("Backup file does not exist: ${backupFile.absolutePath}")
                return false
            }
            
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            loadData() // Reload data from the restored file
            
            plugin.logger.info("JSON storage restored from: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to restore JSON storage from '$backupPath'", e)
            false
        }
    }
    
    /**
     * Start periodic flush task to save pending writes to disk.
     * Flushes every 5 seconds if there are pending writes.
     */
    private fun startPeriodicFlush() {
        flushTask = org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            if (pendingWrites.compareAndSet(true, false)) {
                try {
                    saveData()
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Failed to flush pending writes", e)
                    // Reset flag so we can retry on next flush
                    pendingWrites.set(true)
                }
            }
        }, 20 * 5, 20 * 5) // Every 5 seconds (20 ticks = 1 second)
    }
    
    /**
     * Load data from the JSON file into memory.
     */
    private fun loadData() {
        if (!file.exists()) {
            return
        }
        
        writeLock.readLock().lock()
        try {
            val content = Files.readString(file.toPath())
            if (content.isBlank()) {
                return
            }
            
            val jsonObject = JSONObject(content)
            data.clear()
            
            // Load all key-value pairs from the JSON object
            jsonObject.keys().forEach { key ->
                val value = jsonObject.getString(key)
                data[key] = value
            }
            
            plugin.logger.fine("Loaded ${data.size} items from JSON storage")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load data from JSON storage", e)
            throw StorageOperationException("Failed to load JSON data", e)
        } finally {
            writeLock.readLock().unlock()
        }
    }
    
    /**
     * Save data from memory to the JSON file.
     * Uses write lock to prevent concurrent writes.
     */
    private fun saveData() {
        writeLock.writeLock().lock()
        try {
            val jsonObject = JSONObject()
            
            // Add all data to the JSON object
            data.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            
            // Convert to string with optional pretty printing
            val jsonString = if (prettyPrint) {
                jsonObject.toString(2)
            } else {
                jsonObject.toString()
            }
            
            Files.write(file.toPath(), jsonString.toByteArray())
            plugin.logger.fine("Saved ${data.size} items to JSON storage")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save data to JSON storage", e)
            throw StorageOperationException("Failed to save JSON data", e)
        } finally {
            writeLock.writeLock().unlock()
        }
    }
    
    /**
     * Check if the storage has been initialized.
     * @throws StorageException if not initialized
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw StorageException("JSON storage has not been initialized")
        }
    }
}
