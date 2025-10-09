package de.cloudly.storage.impl.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.core.StorageException
import de.cloudly.storage.core.StorageConnectionException
import de.cloudly.storage.core.StorageOperationException
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
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
    private val useSSL: Boolean = false,
    private val poolSize: Int = 10
) : DataStorage<String> {
    
    private var dataSource: HikariDataSource? = null
    private val initialized = AtomicBoolean(false)
    
    override fun initialize(): Boolean {
        return try {
            // Configure HikariCP connection pool
            val config = HikariConfig().apply {
                jdbcUrl = buildConnectionUrl()
                username = this@MysqlDataStorage.username
                password = this@MysqlDataStorage.password
                maximumPoolSize = poolSize
                minimumIdle = minOf(2, poolSize)
                connectionTimeout = this@MysqlDataStorage.connectionTimeout.toLong()
                
                // Performance and reliability settings
                poolName = "CloudlyMC-MySQL-Pool"
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            }
            
            // Create the HikariCP datasource
            dataSource = HikariDataSource(config)
            
            // Test the connection
            dataSource?.connection?.use { conn ->
                if (!conn.isValid(5)) {
                    throw StorageConnectionException("MySQL connection is not valid")
                }
            }
            
            // Create the data table if it doesn't exist
            createTable()
            
            initialized.set(true)
            plugin.logger.info("MySQL storage initialized with connection pool (size: $poolSize): $host:$port/$database")
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
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1, key)
                    statement.setString(2, item)
                    statement.executeUpdate()
                    true
                }
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
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1, key)
                    val resultSet = statement.executeQuery()
                    
                    if (resultSet.next()) {
                        resultSet.getString("value")
                    } else {
                        null
                    }
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
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1, key)
                    val rowsAffected = statement.executeUpdate()
                    rowsAffected > 0
                }
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
            dataSource?.connection?.use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1, key)
                    val resultSet = statement.executeQuery()
                    resultSet.next()
                }
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
            
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(sql)
                    
                    while (resultSet.next()) {
                        val key = resultSet.getString("key_name")
                        val value = resultSet.getString("value")
                        result[key] = value
                    }
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
            
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(sql)
                    
                    while (resultSet.next()) {
                        result.add(resultSet.getString("key_name"))
                    }
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
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(sql)
                    
                    if (resultSet.next()) {
                        resultSet.getLong("count")
                    } else {
                        0L
                    }
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
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    statement.executeUpdate(sql)
                    true
                }
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to clear MySQL storage", e)
            false
        }
    }
    
    override fun storeAll(items: Map<String, String>): Boolean {
        checkInitialized()
        
        if (items.isEmpty()) {
            return true
        }
        
        val sql = """
            INSERT INTO $tableName (key_name, value) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE value = VALUES(value), updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
        
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
                    plugin.logger.fine("Stored ${items.size} items in MySQL storage")
                    true
                } catch (e: SQLException) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = autoCommit
                }
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to store ${items.size} items in MySQL storage", e)
            false
        }
    }
    
    override fun removeAll(keys: Set<String>): Boolean {
        checkInitialized()
        
        if (keys.isEmpty()) {
            return true
        }
        
        val sql = "DELETE FROM $tableName WHERE key_name = ?"
        
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
                    plugin.logger.fine("Removed ${keys.size} items from MySQL storage")
                    true
                } catch (e: SQLException) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = autoCommit
                }
            } ?: false
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to remove ${keys.size} items from MySQL storage", e)
            false
        }
    }
    
    override fun close() {
        try {
            dataSource?.close()
            dataSource = null
            initialized.set(false)
            plugin.logger.fine("MySQL storage connection pool closed")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error closing MySQL storage connection pool", e)
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
            
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(sql)
                    
                    while (resultSet.next()) {
                        val key = resultSet.getString("key_name").replace("'", "''")
                        val value = resultSet.getString("value").replace("'", "''")
                        
                        backupData.append("INSERT INTO $tableName (key_name, value) VALUES ('$key', '$value');\n")
                    }
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
            
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    for (sql in statements) {
                        if (sql.trim().isNotEmpty()) {
                            statement.execute(sql.trim())
                        }
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
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { statement ->
                    statement.execute(sql)
                }
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
        if (!initialized.get() || dataSource == null) {
            throw StorageException("MySQL storage has not been initialized")
        }
        
        // HikariCP handles connection validation and reconnection automatically
        if (dataSource?.isClosed == true) {
            plugin.logger.warning("MySQL connection pool is closed, attempting to reinitialize...")
            initialize()
        }
    }
}
