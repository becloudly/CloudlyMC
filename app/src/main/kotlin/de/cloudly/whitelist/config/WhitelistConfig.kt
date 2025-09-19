package de.cloudly.whitelist.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

/**
 * Configuration class for the whitelist system.
 * Loads and provides access to whitelist-related configuration options.
 */
class WhitelistConfig(private val plugin: JavaPlugin) {
    var enabled: Boolean = true
    var storageType: StorageType = StorageType.JSON
    
    // JSON storage settings
    var jsonFilePath: String = "whitelist.json"
    
    // SQLite storage settings
    var sqliteFilePath: String = "whitelist.db"
    
    // MySQL storage settings
    var mysqlHost: String = "localhost"
    var mysqlPort: Int = 3306
    var mysqlDatabase: String = "minecraft_whitelist"
    var mysqlUsername: String = "root"
    var mysqlPassword: String = ""
    var mysqlTablePrefix: String = "cloudly_"
    
    /**
     * Load whitelist configuration from the plugin's config.
     */
    fun load() {
        val config = plugin.config
        
        // Load main whitelist settings
        val whitelistSection = config.getConfigurationSection("whitelist")
        if (whitelistSection != null) {
            enabled = whitelistSection.getBoolean("enabled", true)
            storageType = StorageType.fromString(whitelistSection.getString("storage_type", "json"))
            
            // Load JSON settings
            loadJsonSettings(whitelistSection.getConfigurationSection("json"))
            
            // Load SQLite settings
            loadSqliteSettings(whitelistSection.getConfigurationSection("sqlite"))
            
            // Load MySQL settings
            loadMysqlSettings(whitelistSection.getConfigurationSection("mysql"))
        }
    }
    
    private fun loadJsonSettings(section: ConfigurationSection?) {
        section?.let {
            jsonFilePath = it.getString("file_path", jsonFilePath) ?: jsonFilePath
        }
    }
    
    private fun loadSqliteSettings(section: ConfigurationSection?) {
        section?.let {
            sqliteFilePath = it.getString("file_path", sqliteFilePath) ?: sqliteFilePath
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
        }
    }
    
    /**
     * Save the current configuration to the plugin's config file.
     */
    fun save() {
        val config = plugin.config
        
        // Save main whitelist settings
        config.set("whitelist.enabled", enabled)
        config.set("whitelist.storage_type", storageType.name.lowercase())
        
        // Save JSON settings
        config.set("whitelist.json.file_path", jsonFilePath)
        
        // Save SQLite settings
        config.set("whitelist.sqlite.file_path", sqliteFilePath)
        
        // Save MySQL settings
        config.set("whitelist.mysql.host", mysqlHost)
        config.set("whitelist.mysql.port", mysqlPort)
        config.set("whitelist.mysql.database", mysqlDatabase)
        config.set("whitelist.mysql.username", mysqlUsername)
        config.set("whitelist.mysql.password", mysqlPassword)
        config.set("whitelist.mysql.table_prefix", mysqlTablePrefix)
        
        plugin.saveConfig()
    }
    
    /**
     * Enum representing the available storage types for the whitelist.
     */
    enum class StorageType {
        JSON, SQLITE, MYSQL;
        
        companion object {
            /**
             * Convert a string to a StorageType.
             * @param type The string representation of the storage type
             * @return The corresponding StorageType, or JSON if the string is invalid
             */
            fun fromString(type: String?): StorageType {
                return when (type?.lowercase()) {
                    "sqlite" -> SQLITE
                    "mysql" -> MYSQL
                    else -> JSON
                }
            }
        }
    }
}