package de.cloudly.storage.impl.mysql

import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.core.StorageException
import de.cloudly.storage.core.StorageConnectionException
import de.cloudly.storage.core.StorageOperationException
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MySQL implementation of the DataStorage interface.
 * Stores data in a MySQL database with a key-value table structure.
 */
class MysqlDataStorage(
    private val plugin: JavaPlugin,
    private val host: String,
    private val port: Int,
    private val database: String,
    private val username: String,
    private val password: String,
    private val tableName: String,
    private val connectionTimeout: Int = 30000,
    private val useSSL: Boolean = false
) : DataStorage<String> {
    
    private var connection: Connection? = null
    private val initialized = AtomicBoolean(false)
    
    override fun initialize(): Boolean {
        return try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver")
            
            // Build connection URL
            val url = buildConnectionUrl()
            
            // Create the connection
            connection = DriverManager.getConnection(url, username, password)
            
            // Test the connection
            if (connection?.isValid(5) != true) {
                throw StorageConnectionException("MySQL connection is not valid")
            }
            
            // Create the data table if it doesn't exist
            createTable()
            
            initialized.set(true)
            plugin.logger.info("MySQL storage initialized: $host:$port/$database")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize MySQL storage: $host:$port/$database", e)
            false
        }
    }
    
    override fun store(key: String, item: String): Boolean {
        checkInitialized()
        
        val sql = """
            INSERT INTO $tableName (key_name, value) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE value = VALUES(value), updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                statement.setString(2, item)
                statement.executeUpdate()
                true
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to store item with key '$key' in MySQL storage", e)
            false
        }
    }
    
    override fun retrieve(key: String): String? {
        checkInitialized()
        
        val sql = "SELECT value FROM $tableName WHERE key_name = ?"
        
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
            plugin.logger.log(Level.SEVERE, "Failed to retrieve item with key '$key' from MySQL storage", e)
            null
        }
    }
    
    override fun remove(key: String): Boolean {
        checkInitialized()
        
        val sql = "DELETE FROM $tableName WHERE key_name = ?"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                val rowsAffected = statement.executeUpdate()
                rowsAffected > 0
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to remove item with key '$key' from MySQL storage", e)
            false
        }
    }
    
    override fun exists(key: String): Boolean {
        checkInitialized()
        
        val sql = "SELECT 1 FROM $tableName WHERE key_name = ? LIMIT 1"
        
        return try {
            connection?.prepareStatement(sql)?.use { statement ->
                statement.setString(1, key)
                val resultSet = statement.executeQuery()
                resultSet.next()
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to check existence of key '$key' in MySQL storage", e)
            false
        }
    }
    
    override fun getAll(): Map<String, String> {
        checkInitialized()
        
        val sql = "SELECT key_name, value FROM $tableName"
        
        return try {
            val result = mutableMapOf<String, String>()
            
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                while (resultSet.next()) {
                    val key = resultSet.getString("key_name")
                    val value = resultSet.getString("value")
                    result[key] = value
                }
            }
            
            result
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all items from MySQL storage", e)
            emptyMap()
        }
    }
    
    override fun getAllKeys(): Set<String> {
        checkInitialized()
        
        val sql = "SELECT key_name FROM $tableName"
        
        return try {
            val result = mutableSetOf<String>()
            
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                while (resultSet.next()) {
                    result.add(resultSet.getString("key_name"))
                }
            }
            
            result
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to get all keys from MySQL storage", e)
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
            plugin.logger.log(Level.SEVERE, "Failed to count items in MySQL storage", e)
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
            plugin.logger.log(Level.SEVERE, "Failed to clear MySQL storage", e)
            false
        }
    }
    
    override fun close() {
        try {
            connection?.close()
            connection = null
            initialized.set(false)
            plugin.logger.fine("MySQL storage connection closed")
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Error closing MySQL storage connection", e)
        }
    }
    
    override fun backup(backupPath: String): Boolean {
        checkInitialized()
        
        // For MySQL, we'll create a SQL dump
        return try {
            val backupFile = java.io.File(plugin.dataFolder, backupPath)
            backupFile.parentFile?.mkdirs()
            
            val sql = "SELECT key_name, value FROM $tableName"
            val backupData = StringBuilder()
            
            // Add table creation statement
            backupData.append("-- MySQL backup for table $tableName\n")
            backupData.append("-- Generated at ${java.time.Instant.now()}\n\n")
            
            connection?.createStatement()?.use { statement ->
                val resultSet = statement.executeQuery(sql)
                
                while (resultSet.next()) {
                    val key = resultSet.getString("key_name").replace("'", "''")
                    val value = resultSet.getString("value").replace("'", "''")
                    
                    backupData.append("INSERT INTO $tableName (key_name, value) VALUES ('$key', '$value');\n")
                }
            }
            
            java.nio.file.Files.write(backupFile.toPath(), backupData.toString().toByteArray())
            plugin.logger.info("MySQL storage backed up to: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to backup MySQL storage to '$backupPath'", e)
            false
        }
    }
    
    override fun restore(backupPath: String): Boolean {
        checkInitialized()
        
        return try {
            val backupFile = java.io.File(plugin.dataFolder, backupPath)
            if (!backupFile.exists()) {
                plugin.logger.warning("Backup file does not exist: ${backupFile.absolutePath}")
                return false
            }
            
            // Clear existing data
            clear()
            
            // Read and execute backup SQL
            val backupContent = java.nio.file.Files.readString(backupFile.toPath())
            val statements = backupContent.split(";").filter { it.trim().isNotEmpty() && !it.trim().startsWith("--") }
            
            connection?.createStatement()?.use { statement ->
                for (sql in statements) {
                    if (sql.trim().isNotEmpty()) {
                        statement.execute(sql.trim())
                    }
                }
            }
            
            plugin.logger.info("MySQL storage restored from: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to restore MySQL storage from '$backupPath'", e)
            false
        }
    }
    
    /**
     * Build the MySQL connection URL with appropriate parameters.
     */
    private fun buildConnectionUrl(): String {
        val params = mutableListOf<String>()
        
        // Basic connection parameters
        params.add("useSSL=$useSSL")
        params.add("serverTimezone=UTC")
        params.add("connectTimeout=$connectionTimeout")
        params.add("socketTimeout=60000")
        params.add("autoReconnect=true")
        params.add("useUnicode=true")
        params.add("characterEncoding=UTF-8")
        
        return "jdbc:mysql://$host:$port/$database?${params.joinToString("&")}"
    }
    
    /**
     * Create the data storage table if it doesn't exist.
     */
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS $tableName (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                key_name VARCHAR(255) NOT NULL UNIQUE,
                value LONGTEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_key_name (key_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """.trimIndent()
        
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(sql)
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to create MySQL table", e)
            throw StorageConnectionException("Failed to create MySQL table", e)
        }
    }
    
    /**
     * Check if the storage has been initialized.
     * @throws StorageException if not initialized
     */
    private fun checkInitialized() {
        if (!initialized.get() || connection == null) {
            throw StorageException("MySQL storage has not been initialized")
        }
        
        // Check if connection is still valid
        try {
            if (connection?.isValid(5) != true) {
                plugin.logger.warning("MySQL connection is no longer valid, attempting to reconnect...")
                initialize()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to validate MySQL connection", e)
        }
    }
}
