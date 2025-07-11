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
    private var languageManager: LanguageManager? = null
    
    /**
     * Set the language manager for translated messages.
     */
    fun setLanguageManager(languageManager: LanguageManager) {
        this.languageManager = languageManager
    }
    
    /**
     * Get a translated message, fallback to English if language manager is not set.
     */
    private fun getMessage(key: String, vararg placeholders: Pair<String, Any>): String {
        return languageManager?.getMessage(key, *placeholders) ?: when (key) {
            "config.directory_created" -> "Created cloudly configuration directory"
            "config.default_created" -> "Created default config.yml in cloudly folder"
            "config.copy_failed" -> "Could not copy default config"
            "config.loaded_successfully" -> "Configuration loaded successfully"
            "config.default_file_created" -> "Created default configuration file"
            "config.create_failed" -> "Could not create default config file"
            "config.saved_successfully" -> "Configuration saved successfully"
            "config.save_failed" -> "Could not save config file"
            "config.reloaded" -> "Configuration reloaded"
            else -> key
        }
    }
    
    /**
     * Initialize the configuration system.
     * Creates the cloudly folder and config file if they don't exist.
     */
    fun initialize() {
        // Create plugins/cloudly directory
        val pluginFolder = File(plugin.dataFolder.parentFile, "Cloudly")
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs()
            plugin.logger.info(getMessage("config.directory_created"))
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
            plugin.logger.info(getMessage("config.loaded_successfully"))
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
                plugin.logger.info(getMessage("config.default_created"))
            } else {
                // Fallback if resource is not found
                createDefaultConfig(targetFile)
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, getMessage("config.copy_failed"), e)
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
            defaultConfig.set("plugin.language", "en")
            defaultConfig.save(file)
            config = defaultConfig
            plugin.logger.info(getMessage("config.default_file_created"))
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, getMessage("config.create_failed"), e)
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
                    plugin.logger.info(getMessage("config.saved_successfully"))
                } catch (e: IOException) {
                    plugin.logger.log(Level.SEVERE, getMessage("config.save_failed"), e)
                }
            }
        }
    }
    
    /**
     * Reload the configuration from file.
     */
    fun reloadConfig() {
        loadConfig()
        plugin.logger.info(getMessage("config.reloaded"))
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