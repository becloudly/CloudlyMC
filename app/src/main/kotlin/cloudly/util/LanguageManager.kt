/*
 * Cloudly - Language Manager
 * 
 * Handles multi-language support by loading messages from language files
 * and providing a centralized way to access translated messages.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.MessageFormat
import java.util.logging.Level

/**
 * Manages language files and provides access to translated messages
 */
object LanguageManager {
    
    private var messages: YamlConfiguration? = null
    private var currentLanguage: String = "en"
    private val defaultLanguage: String = "en"
    private var pluginInstance: JavaPlugin? = null
      /**
     * Initialize the language manager with the plugin instance
     * Loads the configured language file
     */
    fun initialize(plugin: JavaPlugin) {
        try {
            pluginInstance = plugin
            
            // Get language from config safely
            currentLanguage = plugin.config?.getString("plugin.language", defaultLanguage) ?: defaultLanguage
            
            // Load language file
            loadLanguage(plugin)
            
            // Log language loading safely
            safeLog(plugin, "Loaded language: $currentLanguage")
        } catch (e: Exception) {
            // If initialization fails, use default values and log error
            safeLog(plugin, "Failed to initialize LanguageManager: ${e.message}", Level.SEVERE)
            messages = YamlConfiguration()
            currentLanguage = defaultLanguage
        }
    }
      /**
     * Load the language file for the current language
     * Falls back to English if the requested language file doesn't exist
     */
    private fun loadLanguage(plugin: JavaPlugin) {
        try {
            // Create lang directory if it doesn't exist
            val langDir = File(plugin.dataFolder, "lang")
            if (!langDir.exists()) {
                langDir.mkdirs()
                  // Save default language files safely
                val languageFiles = listOf("en.yml", "de.yml", "fr.yml", "es.yml", "pt.yml", "pl.yml", "ru.yml", "zh.yml")
                for (langFile in languageFiles) {
                    try {
                        plugin.saveResource("lang/$langFile", false)
                    } catch (e: Exception) {
                        safeLog(plugin, "Could not save language file $langFile: ${e.message}", Level.FINE)
                    }
                }
            }
            
            // Try to load the configured language
            val langFile = File(langDir, "$currentLanguage.yml")
            
            // If the language file doesn't exist, try to create it from the jar
            if (!langFile.exists()) {
                try {
                    plugin.saveResource("lang/$currentLanguage.yml", false)
                } catch (e: Exception) {
                    // Resource might not exist in jar - this is expected for unsupported languages
                    safeLog(plugin, "Language file '$currentLanguage.yml' not found in resources", Level.FINE)
                }
            }
            
            // If the file still doesn't exist, fall back to English
            if (!langFile.exists()) {
                safeLog(plugin, "Language file for '$currentLanguage' not found, falling back to English", Level.WARNING)
                currentLanguage = defaultLanguage
                
                // Try to load English file
                val defaultLangFile = File(langDir, "$defaultLanguage.yml")
                if (!defaultLangFile.exists()) {
                    try {
                        plugin.saveResource("lang/$defaultLanguage.yml", false)
                    } catch (e: Exception) {
                        safeLog(plugin, "Could not create default language file: ${e.message}", Level.SEVERE)
                    }
                }
                
                if (defaultLangFile.exists()) {
                    messages = YamlConfiguration.loadConfiguration(defaultLangFile)
                } else {
                    safeLog(plugin, "Default language file not found! Messages will not be available.", Level.SEVERE)
                    messages = YamlConfiguration()
                }
            } else {
                // Load the language file
                messages = YamlConfiguration.loadConfiguration(langFile)
            }
            
        } catch (e: Exception) {
            safeLog(plugin, "Failed to load language file: ${e.message}", Level.SEVERE)
            messages = YamlConfiguration()
        }
    }
      /**
     * Get a message from the language file with the given key
     * Optionally format the message with the provided arguments
     * Falls back to the key itself if the message is not found
     */
    fun getMessage(key: String, vararg args: Any): String {
        try {
            val message = messages?.getString(key) ?: return getDefaultMessage(key)
            
            // Format message with arguments if provided
            return if (args.isEmpty()) {
                colorize(message)
            } else {
                try {
                    colorize(MessageFormat.format(message, *args))
                } catch (e: Exception) {
                    safeLog("Error formatting message '$key': ${e.message}", Level.WARNING)
                    colorize(message)
                }
            }
        } catch (e: Exception) {
            safeLog("Error retrieving message '$key': ${e.message}", Level.WARNING)
            return getDefaultMessage(key)
        }
    }
      /**
     * Get a default message for common keys
     * This is a fallback mechanism in case a message is not found in the language file
     */
    private fun getDefaultMessage(key: String): String {
        return when (key) {
            "common.no-permission" -> "&cYou don't have permission to use this command."
            "common.player-only" -> "&cThis command can only be used by players."
            "common.error-occurred" -> "&cAn error occurred while executing this command."
            "common.prefix" -> "&8[&bCloudly&8]&r "
            "system.startup" -> "&aCloudly v{0} is starting up..."
            "system.running-on" -> "&7Running on {0} {1}"
            "system.enabled-success" -> "&aCloudly has been enabled successfully!"
            "system.enable-failed" -> "&cFailed to enable Cloudly"
            "system.shutdown" -> "&aCloudly has been disabled successfully!"
            "system.shutdown-error" -> "&cError during plugin shutdown"
            "system.reloading" -> "&7Reloading Cloudly..."
            "common.reload-success" -> "&aCloudly has been reloaded successfully!"            "common.reload-failed" -> "&cFailed to reload Cloudly. Check console for errors."
            else -> {
                safeLog("Missing language key: $key", Level.WARNING)
                key
            }
        }
    }
      /**
     * Color code translation for chat messages
     * Uses & as the color code prefix
     */
    private fun colorize(message: String): String {
        return try {
            ChatColor.translateAlternateColorCodes('&', message)
        } catch (e: Exception) {
            safeLog("Error colorizing message: ${e.message}", Level.WARNING)
            message // Return uncolored message if colorization fails
        }
    }
    
    /**
     * Get the plugin prefix from the language file
     */
    fun getPrefix(): String {
        return try {
            getMessage("common.prefix")
        } catch (e: Exception) {
            safeLog("Error getting prefix: ${e.message}", Level.WARNING)
            "&8[&bCloudly&8]&r " // Fallback prefix
        }
    }
    
    /**
     * Get a message with the plugin prefix
     */
    fun getPrefixedMessage(key: String, vararg args: Any): String {
        return try {
            getPrefix() + getMessage(key, *args)
        } catch (e: Exception) {
            safeLog("Error getting prefixed message: ${e.message}", Level.WARNING)
            "[Cloudly] " + getMessage(key, *args) // Fallback with simple prefix
        }
    }
    
    /**
     * Reload the language files
     * Used when the plugin is reloaded
     */
    fun reload(plugin: JavaPlugin) {
        try {
            pluginInstance = plugin
            
            // Update current language from config safely
            currentLanguage = plugin.config?.getString("plugin.language", defaultLanguage) ?: defaultLanguage
            
            // Reload language file
            loadLanguage(plugin)
            
            safeLog(plugin, "Reloaded language: $currentLanguage")
        } catch (e: Exception) {
            safeLog(plugin, "Failed to reload LanguageManager: ${e.message}", Level.SEVERE)
        }
    }
    
    /**
     * Get the current language
     */
    fun getCurrentLanguage(): String {
        return currentLanguage
    }
    
    /**
     * Safe logging method that handles plugin initialization issues
     */
    private fun safeLog(message: String, level: Level = Level.INFO) {
        safeLog(pluginInstance, message, level)
    }
    
    /**
     * Safe logging method that handles plugin initialization issues
     */
    private fun safeLog(plugin: JavaPlugin?, message: String, level: Level = Level.INFO) {
        try {
            plugin?.logger?.log(level, message)
        } catch (e: Exception) {
            // Fallback to System.out if plugin logger is not available
            System.out.println("[Cloudly-${level.name}] $message")
        }
    }
}