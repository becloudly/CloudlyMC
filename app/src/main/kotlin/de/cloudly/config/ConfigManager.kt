package de.cloudly.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.util.logging.Level

/**
 * Manages configuration files for the Cloudly plugin.
 * Creates and manages config files in the plugins/Cloudly/ directory.
 */
class ConfigManager(private val plugin: JavaPlugin) {
    
    private var config: FileConfiguration? = null
    private var configFile: File? = null
    
    /**
     * Initialize the configuration system.
     * Creates the cloudly folder and config file if they don't exist.
     */
    fun initialize() {
        // Create plugins/cloudly directory
        val pluginFolder = File(plugin.dataFolder.parentFile, "Cloudly")
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs()
            plugin.logger.info("Created cloudly configuration directory")
        }
        
        // Set up config file path
        configFile = File(pluginFolder, "config.yml")
        
        // Load or create config
        loadConfig()
    }
    
    /**
     * Load the configuration file.
     * If the file doesn't exist, it will be created with default values.
     */
    private fun loadConfig() {
        configFile?.let { file ->
            if (!file.exists()) {
                // Copy default config from plugin resources
                try {
                    plugin.saveResource("config.yml", false)
                    val defaultConfig = File(plugin.dataFolder, "config.yml")
                    if (defaultConfig.exists()) {
                        defaultConfig.copyTo(file, overwrite = true)
                        defaultConfig.delete()
                        plugin.logger.info("Created default config.yml in cloudly folder")
                    }
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Could not copy default config", e)
                    createDefaultConfig(file)
                }
            }
            
            config = YamlConfiguration.loadConfiguration(file)
            plugin.logger.info("Configuration loaded successfully")
        }
    }
    
    /**
     * Create a default config file with basic settings.
     */
    private fun createDefaultConfig(file: File) {
        try {
            file.createNewFile()
            val defaultConfig = YamlConfiguration()
            defaultConfig.set("plugin.debug", false)
            defaultConfig.save(file)
            config = defaultConfig
            plugin.logger.info("Created default configuration file")
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not create default config file", e)
        }
    }
    
    /**
     * Save the current configuration to file.
     */
    fun saveConfig() {
        configFile?.let { file ->
            config?.let { cfg ->
                try {
                    cfg.save(file)
                    plugin.logger.info("Configuration saved successfully")
                } catch (e: IOException) {
                    plugin.logger.log(Level.SEVERE, "Could not save config file", e)
                }
            }
        }
    }
    
    /**
     * Reload the configuration from file.
     */
    fun reloadConfig() {
        loadConfig()
        plugin.logger.info("Configuration reloaded")
    }
    
    /**
     * Get a boolean value from the config.
     */
    fun getBoolean(path: String, defaultValue: Boolean = false): Boolean {
        return config?.getBoolean(path, defaultValue) ?: defaultValue
    }
    
    /**
     * Get a string value from the config.
     */
    fun getString(path: String, defaultValue: String = ""): String {
        return config?.getString(path, defaultValue) ?: defaultValue
    }
    
    /**
     * Get an integer value from the config.
     */
    fun getInt(path: String, defaultValue: Int = 0): Int {
        return config?.getInt(path, defaultValue) ?: defaultValue
    }
    
    /**
     * Get a double value from the config.
     */
    fun getDouble(path: String, defaultValue: Double = 0.0): Double {
        return config?.getDouble(path, defaultValue) ?: defaultValue
    }
    
    /**
     * Set a value in the config.
     */
    fun set(path: String, value: Any?) {
        config?.set(path, value)
    }
    
    /**
     * Check if a path exists in the config.
     */
    fun contains(path: String): Boolean {
        return config?.contains(path) ?: false
    }
    
    /**
     * Get the raw FileConfiguration object.
     */
    fun getConfig(): FileConfiguration? {
        return config
    }
}