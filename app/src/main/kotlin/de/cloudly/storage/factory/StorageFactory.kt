package de.cloudly.storage.factory

import de.cloudly.storage.config.RepositoryConfig
import de.cloudly.storage.config.StorageConfig
import de.cloudly.storage.config.StorageType
import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.core.DataStorage
import de.cloudly.storage.core.Serializable
import de.cloudly.storage.core.StorageConfigurationException
import de.cloudly.storage.impl.json.JsonDataStorage
import de.cloudly.storage.impl.mysql.MysqlDataStorage
import de.cloudly.storage.impl.sqlite.SqliteDataStorage
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Factory for creating storage instances based on configuration.
 * This factory handles the creation of both low-level DataStorage instances
 * and high-level DataRepository instances.
 */
class StorageFactory(private val plugin: JavaPlugin) {
    
    /**
     * Create a DataStorage instance based on the provided configuration.
     * @param config The repository configuration
     * @return The appropriate DataStorage implementation
     * @throws StorageConfigurationException if the configuration is invalid
     */
    fun createDataStorage(config: RepositoryConfig): DataStorage<String> {
        return when (config.storageType) {
            StorageType.JSON -> createJsonStorage(config)
            StorageType.SQLITE -> createSqliteStorage(config)
            StorageType.MYSQL -> createMysqlStorage(config)
        }
    }
    
    /**
     * Create a DataRepository instance for a specific data type.
     * @param repositoryName The name of the repository
     * @param storageConfig The global storage configuration
     * @param deserializer Function to deserialize data from string format
     * @param storageType Optional override for storage type
     * @return A configured DataRepository instance
     */
    fun <T> createRepository(
        repositoryName: String,
        storageConfig: StorageConfig,
        deserializer: (String, String) -> T?,
        storageType: StorageType? = null
    ): DataRepository<T> where T : Any, T : Serializable {
        
        val repoConfig = storageConfig.getRepositoryConfig(repositoryName, storageType)
        val dataStorage = createDataStorage(repoConfig)
        
        return DataRepository(plugin, dataStorage, deserializer, repositoryName)
    }
    
    /**
     * Create a JSON storage instance.
     */
    private fun createJsonStorage(config: RepositoryConfig): JsonDataStorage {
        val filePath = config.properties["filePath"] 
            ?: throw StorageConfigurationException("JSON storage requires 'filePath' property")
        
        val prettyPrint = config.properties["prettyPrint"]?.toBoolean() ?: true
        
        plugin.logger.info("Creating JSON storage for '${config.repositoryName}' at: $filePath")
        return JsonDataStorage(plugin, filePath, prettyPrint)
    }
    
    /**
     * Create a SQLite storage instance.
     */
    private fun createSqliteStorage(config: RepositoryConfig): SqliteDataStorage {
        val filePath = config.properties["filePath"] 
            ?: throw StorageConfigurationException("SQLite storage requires 'filePath' property")
        
        val journalMode = config.properties["journalMode"] ?: "WAL"
        val synchronous = config.properties["synchronous"] ?: "NORMAL"
        val tableName = "data_${config.repositoryName.lowercase().replace("[^a-z0-9_]".toRegex(), "_")}"
        
        plugin.logger.info("Creating SQLite storage for '${config.repositoryName}' at: $filePath")
        return SqliteDataStorage(plugin, filePath, tableName, journalMode, synchronous)
    }
    
    /**
     * Create a MySQL storage instance.
     */
    private fun createMysqlStorage(config: RepositoryConfig): MysqlDataStorage {
        val host = config.properties["host"] 
            ?: throw StorageConfigurationException("MySQL storage requires 'host' property")
        
        val port = config.properties["port"]?.toIntOrNull() 
            ?: throw StorageConfigurationException("MySQL storage requires valid 'port' property")
        
        val database = config.properties["database"] 
            ?: throw StorageConfigurationException("MySQL storage requires 'database' property")
        
        val username = config.properties["username"] 
            ?: throw StorageConfigurationException("MySQL storage requires 'username' property")
        
        val password = config.properties["password"] ?: ""
        
        val tableName = config.properties["tableName"] 
            ?: "data_${config.repositoryName.lowercase().replace("[^a-z0-9_]".toRegex(), "_")}"
        
        val connectionTimeout = config.properties["connectionTimeout"]?.toIntOrNull() ?: 30000
        val useSSL = config.properties["useSSL"]?.toBoolean() ?: false
        val poolSize = config.properties["poolSize"]?.toIntOrNull() ?: 10
        
        plugin.logger.info("Creating MySQL storage for '${config.repositoryName}' at: $host:$port/$database with pool size: $poolSize")
        return MysqlDataStorage(plugin, host, port, database, username, password, tableName, connectionTimeout, useSSL, poolSize)
    }
    
    /**
     * Create multiple repositories based on a list of configurations.
     * This is useful for bulk initialization of storage systems.
     * 
     * @param repositories Map of repository name to deserializer function
     * @param storageConfig The global storage configuration
     * @param defaultStorageType Optional default storage type for all repositories
     * @return Map of repository name to initialized DataRepository
     */
    fun <T> createRepositories(
        repositories: Map<String, (String, String) -> T?>,
        storageConfig: StorageConfig,
        defaultStorageType: StorageType? = null
    ): Map<String, DataRepository<T>> where T : Any, T : Serializable {
        
        val result = mutableMapOf<String, DataRepository<T>>()
        
        repositories.forEach { (name, deserializer) ->
            try {
                val repository = createRepository(name, storageConfig, deserializer, defaultStorageType)
                if (repository.initialize()) {
                    result[name] = repository
                    plugin.logger.info("Successfully initialized repository: $name")
                } else {
                    plugin.logger.severe("Failed to initialize repository: $name")
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Error creating repository '$name'", e)
            }
        }
        
        return result
    }
    
    /**
     * Utility method to validate a storage configuration.
     * @param config The repository configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    fun validateConfig(config: RepositoryConfig): Boolean {
        return try {
            when (config.storageType) {
                StorageType.JSON -> {
                    config.properties["filePath"] != null
                }
                StorageType.SQLITE -> {
                    config.properties["filePath"] != null
                }
                StorageType.MYSQL -> {
                    config.properties["host"] != null &&
                    config.properties["port"]?.toIntOrNull() != null &&
                    config.properties["database"] != null &&
                    config.properties["username"] != null
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Invalid storage configuration for ${config.repositoryName}", e)
            false
        }
    }
}
