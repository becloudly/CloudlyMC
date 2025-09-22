package de.cloudly.storage.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

/**
 * Generic configuration class for the storage system.
 * This class manages configuration for different storage types and can be used
 * by any part of the plugin that needs data storage.
 */
class StorageConfig(
    private val plugin: JavaPlugin,
    private val configSection: String = "storage"
) {
    
    // Default storage settings
    var defaultStorageType: StorageType = StorageType.JSON
    
    // JSON storage settings
    var jsonBasePath: String = "data"
    var jsonFileExtension: String = ".json"
    var jsonPrettyPrint: Boolean = true
    
    // SQLite storage settings
    var sqliteBasePath: String = "data"
    var sqliteFileExtension: String = ".db"
    var sqliteJournalMode: String = "WAL"
    var sqliteSynchronous: String = "NORMAL"
    
    // MySQL storage settings
    var mysqlHost: String = "localhost"
    var mysqlPort: Int = 3306
    var mysqlDatabase: String = "cloudly_plugin"
    var mysqlUsername: String = "root"
    var mysqlPassword: String = ""
    var mysqlTablePrefix: String = "cloudly_"
    var mysqlConnectionTimeout: Int = 30000
    var mysqlUseSSL: Boolean = false
    var mysqlPoolSize: Int = 10
    
    /**
     * Load storage configuration from the plugin's config file.
     */
    fun load() {
        val config = plugin.config
        val storageSection = config.getConfigurationSection(configSection)
        
        if (storageSection != null) {
            // Load default settings
            defaultStorageType = StorageType.fromString(storageSection.getString("default_type", "json"))
            
            // Load JSON settings
            loadJsonSettings(storageSection.getConfigurationSection("json"))
            
            // Load SQLite settings
            loadSqliteSettings(storageSection.getConfigurationSection("sqlite"))
            
            // Load MySQL settings
            loadMysqlSettings(storageSection.getConfigurationSection("mysql"))
        }
    }
    
    /**
     * Save the current configuration to the plugin's config file.
     */
    fun save() {
        val config = plugin.config
        
        // Save default settings
        config.set("$configSection.default_type", defaultStorageType.name.lowercase())
        
        // Save JSON settings
        config.set("$configSection.json.base_path", jsonBasePath)
        config.set("$configSection.json.file_extension", jsonFileExtension)
        config.set("$configSection.json.pretty_print", jsonPrettyPrint)
        
        // Save SQLite settings
        config.set("$configSection.sqlite.base_path", sqliteBasePath)
        config.set("$configSection.sqlite.file_extension", sqliteFileExtension)
        config.set("$configSection.sqlite.journal_mode", sqliteJournalMode)
        config.set("$configSection.sqlite.synchronous", sqliteSynchronous)
        
        // Save MySQL settings
        config.set("$configSection.mysql.host", mysqlHost)
        config.set("$configSection.mysql.port", mysqlPort)
        config.set("$configSection.mysql.database", mysqlDatabase)
        config.set("$configSection.mysql.username", mysqlUsername)
        config.set("$configSection.mysql.password", mysqlPassword)
        config.set("$configSection.mysql.table_prefix", mysqlTablePrefix)
        config.set("$configSection.mysql.connection_timeout", mysqlConnectionTimeout)
        config.set("$configSection.mysql.use_ssl", mysqlUseSSL)
        config.set("$configSection.mysql.pool_size", mysqlPoolSize)
        
        plugin.saveConfig()
    }
    
    /**
     * Get configuration for a specific repository.
     * @param repositoryName The name of the repository
     * @param storageType The storage type to use (defaults to default storage type)
     * @return A RepositoryConfig object with the specific settings
     */
    fun getRepositoryConfig(repositoryName: String, storageType: StorageType? = null): RepositoryConfig {
        val actualStorageType = storageType ?: defaultStorageType
        
        return when (actualStorageType) {
            StorageType.JSON -> RepositoryConfig(
                repositoryName = repositoryName,
                storageType = actualStorageType,
                properties = mapOf(
                    "filePath" to "$jsonBasePath/$repositoryName$jsonFileExtension",
                    "prettyPrint" to jsonPrettyPrint.toString()
                )
            )
            StorageType.SQLITE -> RepositoryConfig(
                repositoryName = repositoryName,
                storageType = actualStorageType,
                properties = mapOf(
                    "filePath" to "$sqliteBasePath/$repositoryName$sqliteFileExtension",
                    "journalMode" to sqliteJournalMode,
                    "synchronous" to sqliteSynchronous
                )
            )
            StorageType.MYSQL -> RepositoryConfig(
                repositoryName = repositoryName,
                storageType = actualStorageType,
                properties = mapOf(
                    "host" to mysqlHost,
                    "port" to mysqlPort.toString(),
                    "database" to mysqlDatabase,
                    "username" to mysqlUsername,
                    "password" to mysqlPassword,
                    "tableName" to "${mysqlTablePrefix}$repositoryName",
                    "connectionTimeout" to mysqlConnectionTimeout.toString(),
                    "useSSL" to mysqlUseSSL.toString(),
                    "poolSize" to mysqlPoolSize.toString()
                )
            )
        }
    }
    
    private fun loadJsonSettings(section: ConfigurationSection?) {
        section?.let {
            jsonBasePath = it.getString("base_path", jsonBasePath) ?: jsonBasePath
            jsonFileExtension = it.getString("file_extension", jsonFileExtension) ?: jsonFileExtension
            jsonPrettyPrint = it.getBoolean("pretty_print", jsonPrettyPrint)
        }
    }
    
    private fun loadSqliteSettings(section: ConfigurationSection?) {
        section?.let {
            sqliteBasePath = it.getString("base_path", sqliteBasePath) ?: sqliteBasePath
            sqliteFileExtension = it.getString("file_extension", sqliteFileExtension) ?: sqliteFileExtension
            sqliteJournalMode = it.getString("journal_mode", sqliteJournalMode) ?: sqliteJournalMode
            sqliteSynchronous = it.getString("synchronous", sqliteSynchronous) ?: sqliteSynchronous
        }
    }
    
    private fun loadMysqlSettings(section: ConfigurationSection?) {
        section?.let {
            mysqlHost = it.getString("host", mysqlHost) ?: mysqlHost
            mysqlPort = it.getInt("port", mysqlPort)
            mysqlDatabase = it.getString("database", mysqlDatabase) ?: mysqlDatabase
            mysqlUsername = it.getString("username", mysqlUsername) ?: mysqlUsername
            mysqlPassword = it.getString("password", mysqlPassword) ?: mysqlPassword
            mysqlTablePrefix = it.getString("table_prefix", mysqlTablePrefix) ?: mysqlTablePrefix
            mysqlConnectionTimeout = it.getInt("connection_timeout", mysqlConnectionTimeout)
            mysqlUseSSL = it.getBoolean("use_ssl", mysqlUseSSL)
            mysqlPoolSize = it.getInt("pool_size", mysqlPoolSize)
        }
    }
}

/**
 * Configuration for a specific repository.
 */
data class RepositoryConfig(
    val repositoryName: String,
    val storageType: StorageType,
    val properties: Map<String, String>
)
