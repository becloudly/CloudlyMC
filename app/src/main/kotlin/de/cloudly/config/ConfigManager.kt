package de.cloudly.config

import de.cloudly.Messages
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
            if (getBoolean("plugin.debug", false)) {
                plugin.logger.info(Messages.Config.DIRECTORY_CREATED)
            }
        }
        
        // Set up config file path
        configFile = File(pluginFolder, "config.yml")
        
        // Load or create config
        loadConfig()
    }
    
    /**
     * Load the configuration file.
     * If the file doesn't exist, it will be created by copying the resource config.yml.
     */
    private fun loadConfig() {
        configFile?.let { file ->
            if (!file.exists()) {
                // Copy default config directly from plugin resources to user location
                copyResourceConfig(file)
            }
            
            config = YamlConfiguration.loadConfiguration(file)
            if (getBoolean("plugin.debug", false)) {
                plugin.logger.info(Messages.Config.LOADED_SUCCESSFULLY)
            }
        }
    }
    
    /**
     * Copy the config.yml from plugin resources to the target file.
     */
    private fun copyResourceConfig(targetFile: File) {
        try {
            val resourceStream = plugin.getResource("config.yml")
            if (resourceStream != null) {
                targetFile.outputStream().use { output ->
                    resourceStream.copyTo(output)
                }
                if (getBoolean("plugin.debug", false)) {
                    plugin.logger.info(Messages.Config.DEFAULT_CREATED)
                }
            } else {
                // Fallback if resource is not found
                createDefaultConfig(targetFile)
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, Messages.Config.COPY_FAILED, e)
            createDefaultConfig(targetFile)
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
            plugin.logger.info(Messages.Config.DEFAULT_FILE_CREATED)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, Messages.Config.CREATE_FAILED, e)
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
                    plugin.logger.info(Messages.Config.SAVED_SUCCESSFULLY)
                } catch (e: IOException) {
                    plugin.logger.log(Level.SEVERE, Messages.Config.SAVE_FAILED, e)
                }
            }
        }
    }
    
    /**
     * Reload the configuration from file.
     */
    fun reloadConfig() {
        loadConfig()
        plugin.logger.info(Messages.Config.RELOADED)
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