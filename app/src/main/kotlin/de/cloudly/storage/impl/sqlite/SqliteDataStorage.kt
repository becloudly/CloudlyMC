package de.cloudly.storage.impl.sqlite

import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.core.StorageException
import de.cloudly.storage.core.StorageConnectionException
import de.cloudly.storage.core.StorageOperationException
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level

/**
 * SQLite implementation of the DataStorage interface.
 * Stores data in a SQLite database with a key-value table structure.
 */
class SqliteDataStorage(
    private val plugin: JavaPlugin,
    private val filePath: String,
    private val tableName: String = "data_storage",
    private val journalMode: String = "WAL",
    private val synchronous: String = "NORMAL"
) : DataStorage<String> {
    
    private var connection: Connection? = null
    private var initialized = false
    
    override fun initialize(): Boolean {
        return try {
            // Ensure the parent directory exists
            val file = File(plugin.dataFolder, filePath)
            file.parentFile?.mkdirs()
            
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC")
            
            // Create the connection
            connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            
            // Configure SQLite settings
            configureSqlite()
            
            // Create the data table if it doesn't exist
            createTable()
            
            initialized = true
            plugin.logger.info("SQLite storage initialized: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize SQLite storage: $filePath", e)
            false
        }
    }
    
    override fun store(key: String, item: String): Boolean {
        checkInitialized()
        
        val sql = "INSERT OR REPLACE INTO $tableName (key, value) VALUES (?, ?)"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                statement.setString(2, item)
                statement.executeUpdate()
                true
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to store item with key '$key' in SQLite storage", e)
            false
        }
    }
    
    override fun retrieve(key: String): String? {
        checkInitialized()
        
        val sql = "SELECT value FROM $tableName WHERE key = ?"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                val resultSet = statement.executeQuery()
                
                if (resultSet.next()) {
                    resultSet.getString("value")
                } else {
                    null
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to retrieve item with key '$key' from SQLite storage", e)
            null
        }
    }
    
    override fun remove(key: String): Boolean {
        checkInitialized()
        
        val sql = "DELETE FROM $tableName WHERE key = ?"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                val rowsAffected = statement.executeUpdate()
                rowsAffected > 0
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to remove item with key '$key' from SQLite storage", e)
            false
        }
    }
    
    override fun exists(key: String): Boolean {
        checkInitialized()
        
        val sql = "SELECT 1 FROM $tableName WHERE key = ?"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                val resultSet = statement.executeQuery()
                resultSet.next()
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to check existence of key '$key' in SQLite storage", e)
            false
        }
    }
    
    override fun getAll(): Map<String, String> {
        checkInitialized()
        
        val sql = "SELECT key, value FROM $tableName"
        
        return try {
            val result = mutableMapOf<String, String>()
            
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                while (resultSet.next()) {
                    val key = resultSet.getString("key")
                    val value = resultSet.getString("value")
                    result[key] = value
                }
            }
            
            result
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all items from SQLite storage", e)
            emptyMap()
        }
    }
    
    override fun getAllKeys(): Set<String> {
        checkInitialized()
        
        val sql = "SELECT key FROM $tableName"
        
        return try {
            val result = mutableSetOf<String>()
            
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                while (resultSet.next()) {
                    result.add(resultSet.getString("key"))
                }
            }
            
            result
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all keys from SQLite storage", e)
            emptySet()
        }
    }
    
    override fun count(): Long {
        checkInitialized()
        
        val sql = "SELECT COUNT(*) as count FROM $tableName"
        
        return try {
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                if (resultSet.next()) {
                    resultSet.getLong("count")
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to count items in SQLite storage", e)
            0L
        }
    }
    
    override fun clear(): Boolean {
        checkInitialized()
        
        val sql = "DELETE FROM $tableName"
        
        return try {
            connection?.createStatement()?.use { statement ->
                statement.executeUpdate(sql)
                true
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to clear SQLite storage", e)
            false
        }
    }
    
    override fun storeAll(items: Map<String, String>): Boolean {
        checkInitialized()
        
        if (items.isEmpty()) {
            return true
        }
        
        val sql = "INSERT OR REPLACE INTO $tableName (key, value) VALUES (?, ?)"
        
        return try {
            connection?.let { conn ->
                val autoCommit = conn.autoCommit
                try {
                    conn.autoCommit = false
                    
                    conn.prepareStatement(sql).use { statement ->
                        items.forEach { (key, value) ->
                            statement.setString(1, key)
                            statement.setString(2, value)
                            statement.addBatch()
                        }
                        statement.executeBatch()
                    }
                    
                    conn.commit()
                    plugin.logger.fine("Stored ${items.size} items in SQLite storage")
                    true
                } catch (e: SQLException) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = autoCommit
                }
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to store ${items.size} items in SQLite storage", e)
            false
        }
    }
    
    override fun removeAll(keys: Set<String>): Boolean {
        checkInitialized()
        
        if (keys.isEmpty()) {
            return true
        }
        
        val sql = "DELETE FROM $tableName WHERE key = ?"
        
        return try {
            connection?.let { conn ->
                val autoCommit = conn.autoCommit
                try {
                    conn.autoCommit = false
                    
                    conn.prepareStatement(sql).use { statement ->
                        keys.forEach { key ->
                            statement.setString(1, key)
                            statement.addBatch()
                        }
                        statement.executeBatch()
                    }
                    
                    conn.commit()
                    plugin.logger.fine("Removed ${keys.size} items from SQLite storage")
                    true
                } catch (e: SQLException) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = autoCommit
                }
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to remove ${keys.size} items from SQLite storage", e)
            false
        }
    }
    
    override fun close() {
        try {
            connection?.close()
            connection = null
            initialized = false
            plugin.logger.fine("SQLite storage connection closed")
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Error closing SQLite storage connection", e)
        }
    }
    
    override fun backup(backupPath: String): Boolean {
        checkInitialized()
        
        return try {
            val sourceFile = File(plugin.dataFolder, filePath)
            val backupFile = File(plugin.dataFolder, backupPath)
            backupFile.parentFile?.mkdirs()
            
            // Close current connection temporarily for backup
            val wasInitialized = initialized
            close()
            
            try {
                Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                plugin.logger.info("SQLite storage backed up to: ${backupFile.absolutePath}")
                true
            } finally {
                // Reinitialize if it was initialized before
                if (wasInitialized) {
                    initialize()
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to backup SQLite storage to '$backupPath'", e)
            false
        }
    }
    
    override fun restore(backupPath: String): Boolean {
        checkInitialized()
        
        return try {
            val sourceFile = File(plugin.dataFolder, filePath)
            val backupFile = File(plugin.dataFolder, backupPath)
            
            if (!backupFile.exists()) {
                plugin.logger.warning("Backup file does not exist: ${backupFile.absolutePath}")
                return false
            }
            
            // Close current connection for restore
            close()
            
            try {
                Files.copy(backupFile.toPath(), sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                plugin.logger.info("SQLite storage restored from: ${backupFile.absolutePath}")
                
                // Reinitialize with the restored file
                initialize()
            } catch (e: Exception) {
                // Try to reinitialize even if restore failed
                initialize()
                throw e
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to restore SQLite storage from '$backupPath'", e)
            false
        }
    }
    
    /**
     * Configure SQLite-specific settings for optimal performance.
     */
    private fun configureSqlite() {
        try {
            connection?.createStatement()?.use { statement ->
                // Set journal mode for better performance and concurrency
                statement.execute("PRAGMA journal_mode = $journalMode")
                
                // Set synchronous mode for better performance
                statement.execute("PRAGMA synchronous = $synchronous")
                
                // Enable foreign key support
                statement.execute("PRAGMA foreign_keys = ON")
                
                plugin.logger.fine("SQLite configured with journal_mode=$journalMode, synchronous=$synchronous")
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to configure SQLite settings", e)
        }
    }
    
    /**
     * Create the data storage table if it doesn't exist.
     */
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS $tableName (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(sql)
                
                // Create an index on the key column for faster lookups
                statement.execute("CREATE INDEX IF NOT EXISTS idx_${tableName}_key ON $tableName(key)")
                
                // Create a trigger to update the updated_at column
                val triggerSql = """
                    CREATE TRIGGER IF NOT EXISTS tr_${tableName}_update_timestamp
                    AFTER UPDATE ON $tableName
                    FOR EACH ROW
                    BEGIN
                        UPDATE $tableName SET updated_at = CURRENT_TIMESTAMP WHERE key = NEW.key;
                    END
                """.trimIndent()
                
                statement.execute(triggerSql)
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to create SQLite table", e)
            throw StorageConnectionException("Failed to create SQLite table", e)
        }
    }
    
    /**
     * Check if the storage has been initialized.
     * @throws StorageException if not initialized
     */
    private fun checkInitialized() {
        if (!initialized || connection == null) {
            throw StorageException("SQLite storage has not been initialized")
        }
    }
}
