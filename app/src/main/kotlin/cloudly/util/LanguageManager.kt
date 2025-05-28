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
    
    /**
     * Initialize the language manager with the plugin instance
     * Loads the configured language file
     */    fun initialize(plugin: JavaPlugin) {
        // Get language from config
        currentLanguage = plugin.config.getString("plugin.language", defaultLanguage) ?: defaultLanguage
        
        // Load language file
        loadLanguage(plugin)
        
        // Use colored logging for language loading message
        if (CloudlyUtils::class.java.isInitialized()) {
            CloudlyUtils.logColored("&aLoaded language: &f$currentLanguage")
        } else {
            plugin.logger.info("Loaded language: $currentLanguage")
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
                
                // Save default language files
                plugin.saveResource("lang/en.yml", false)
                plugin.saveResource("lang/de.yml", false)
            }
            
            // Try to load the configured language
            val langFile = File(langDir, "$currentLanguage.yml")
              // If the language file doesn't exist, try to create it from the jar
            if (!langFile.exists()) {
                plugin.saveResource("lang/$currentLanguage.yml", false)
            }
            
            // If the file still doesn't exist, fall back to English
            if (!langFile.exists()) {
                // Use colored warning if possible
                if (CloudlyUtils::class.java.isInitialized()) {
                    CloudlyUtils.logColored("&eLanguage file for '$currentLanguage' not found, falling back to English", Level.WARNING)
                } else {
                    plugin.logger.warning("Language file for '$currentLanguage' not found, falling back to English")
                }
                currentLanguage = defaultLanguage
                
                // Try to load English file
                val defaultLangFile = File(langDir, "$defaultLanguage.yml")
                if (!defaultLangFile.exists()) {
                    plugin.saveResource("lang/$defaultLanguage.yml", false)
                }
                  if (defaultLangFile.exists()) {
                    messages = YamlConfiguration.loadConfiguration(defaultLangFile)
                } else {
                    // Use colored severe message if possible
                    if (CloudlyUtils::class.java.isInitialized()) {
                        CloudlyUtils.logColored("&cDefault language file not found! Messages will not be available.", Level.SEVERE)
                    } else {
                        plugin.logger.severe("Default language file not found! Messages will not be available.")
                    }
                    messages = YamlConfiguration()
                }
            } else {
                // Load the language file
                messages = YamlConfiguration.loadConfiguration(langFile)
            }
              } catch (e: Exception) {
            // Use colored severe message if possible
            if (CloudlyUtils::class.java.isInitialized()) {
                CloudlyUtils.logColored("&cFailed to load language file: " + e.message, Level.SEVERE)
            } else {
                plugin.logger.log(Level.SEVERE, "Failed to load language file", e)
            }
            messages = YamlConfiguration()
        }
    }
    
    /**
     * Get a message from the language file with the given key
     * Optionally format the message with the provided arguments
     * Falls back to the key itself if the message is not found
     */
    fun getMessage(key: String, vararg args: Any): String {
        val message = messages?.getString(key) ?: return getDefaultMessage(key)
        
        // Format message with arguments if provided
        return if (args.isEmpty()) {
            colorize(message)
        } else {
            try {
                colorize(MessageFormat.format(message, *args))
            } catch (e: Exception) {
                CloudlyPlugin.instance.logger.warning("Error formatting message '$key': ${e.message}")
                colorize(message)
            }
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
            else -> {
                CloudlyPlugin.instance.logger.warning("Missing language key: $key")
                key
            }
        }
    }
    
    /**
     * Color code translation for chat messages
     * Uses & as the color code prefix
     */
    private fun colorize(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }
    
    /**
     * Get the plugin prefix from the language file
     */
    fun getPrefix(): String {
        return getMessage("common.prefix")
    }
    
    /**
     * Get a message with the plugin prefix
     */
    fun getPrefixedMessage(key: String, vararg args: Any): String {
        return getPrefix() + getMessage(key, *args)
    }
      /**
     * Reload the language files
     * Used when the plugin is reloaded
     */
    fun reload(plugin: JavaPlugin) {
        // Update current language from config
        currentLanguage = plugin.config.getString("plugin.language", defaultLanguage) ?: defaultLanguage
        
        // Reload language file
        loadLanguage(plugin)
        
        // Use colored logging for reload message
        if (CloudlyUtils::class.java.isInitialized()) {
            CloudlyUtils.logColored("&aReloaded language: &f$currentLanguage")
        } else {
            plugin.logger.info("Reloaded language: $currentLanguage")
        }
    }
    
    /**
     * Get the current language
     */
    fun getCurrentLanguage(): String {
        return currentLanguage
    }
}
