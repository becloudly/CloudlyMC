package de.cloudly.whitelist.storage

import de.cloudly.whitelist.config.WhitelistConfig
import de.cloudly.whitelist.storage.impl.JsonWhitelistStorage
import de.cloudly.whitelist.storage.impl.SqliteWhitelistStorage
import de.cloudly.whitelist.storage.impl.MysqlWhitelistStorage
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Factory for creating WhitelistStorage implementations based on configuration.
 */
class WhitelistStorageFactory(private val plugin: JavaPlugin) {
    
    /**
     * Create a WhitelistStorage implementation based on the provided configuration.
     * @param config The whitelist configuration
     * @return The appropriate WhitelistStorage implementation
     */
    fun createStorage(config: WhitelistConfig): WhitelistStorage {
        return when (config.storageType) {
            WhitelistConfig.StorageType.JSON -> {
                plugin.logger.info("Using JSON storage for whitelist")
                JsonWhitelistStorage(plugin, config.jsonFilePath)
            }
            WhitelistConfig.StorageType.SQLITE -> {
                plugin.logger.info("Using SQLite storage for whitelist")
                SqliteWhitelistStorage(plugin, config.sqliteFilePath)
            }
            WhitelistConfig.StorageType.MYSQL -> {
                plugin.logger.info("Using MySQL storage for whitelist")
                MysqlWhitelistStorage(
                    plugin,
                    config.mysqlHost,
                    config.mysqlPort,
                    config.mysqlDatabase,
                    config.mysqlUsername,
                    config.mysqlPassword,
                    config.mysqlTablePrefix
                )
            }
        }
    }
}