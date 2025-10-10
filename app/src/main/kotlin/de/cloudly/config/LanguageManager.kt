package de.cloudly.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream
import java.util.logging.Level

/**
 * Manages language files and translations for the Cloudly plugin.
 * Supports multiple languages with fallback to English.
 */
class LanguageManager(private val plugin: JavaPlugin, private val configManager: ConfigManager) {
    
    private var currentLanguage: String = "en"
    private var languageConfig: FileConfiguration? = null
    private var fallbackConfig: FileConfiguration? = null
    
    /**
     * Initialize the language system.
     * Loads the language specified in config or defaults to English.
     */
    fun initialize() {
        // Get language from config, default to English
        currentLanguage = configManager.getString("plugin.language", "en")
        
        // Create languages directory if it doesn't exist
        val languagesDir = File(File(plugin.dataFolder.parentFile, "Cloudly"), "languages")
        if (!languagesDir.exists()) {
            languagesDir.mkdirs()
        }
        
        // Copy language files from resources if they don't exist
        copyLanguageFileIfNotExists("en.yml", languagesDir)
        copyLanguageFileIfNotExists("de.yml", languagesDir)
        
        // Load the selected language
        loadLanguage(currentLanguage)
        
        // Always load English as fallback
        loadFallbackLanguage()
    }
    
    /**
     * Copy a language file from resources to the languages directory if it doesn't exist.
     */
    private fun copyLanguageFileIfNotExists(fileName: String, languagesDir: File) {
        val targetFile = File(languagesDir, fileName)
        if (!targetFile.exists()) {
            try {
                val resourceStream: InputStream? = plugin.getResource("lang/$fileName")
                if (resourceStream != null) {
                    targetFile.outputStream().use { output ->
                        resourceStream.copyTo(output)
                    }
                    if (configManager.getBoolean("plugin.debug", false)) {
                        plugin.logger.info("Created language file: $fileName")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Could not copy language file: $fileName", e)
            }
        }
    }
    
    /**
     * Load the specified language file.
     */
    private fun loadLanguage(language: String) {
        val languagesDir = File(File(plugin.dataFolder.parentFile, "Cloudly"), "languages")
        val languageFile = File(languagesDir, "$language.yml")
        
        if (languageFile.exists()) {
            languageConfig = YamlConfiguration.loadConfiguration(languageFile)
            plugin.logger.info("Loaded language: $language")
        } else {
            plugin.logger.warning("Language file not found: $language.yml, using fallback")
            currentLanguage = "en"
            loadLanguage("en")
        }
    }
    
    /**
     * Load English as fallback language.
     */
    private fun loadFallbackLanguage() {
        val languagesDir = File(File(plugin.dataFolder.parentFile, "Cloudly"), "languages")
        val fallbackFile = File(languagesDir, "en.yml")
        
        if (fallbackFile.exists()) {
            fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile)
        }
    }
    
    /**
     * Get a translated message by key.
     * If the key is not found in the current language, falls back to English.
     * Supports placeholder replacement using {placeholder} syntax.
     */
    fun getMessage(key: String, vararg placeholders: Pair<String, Any>): String {
        // Try to get message from current language
        var message: String? = languageConfig?.getString(key)
        
        // Fallback to English if not found
        if (message == null) {
            message = fallbackConfig?.getString(key)
        }
        
        // Return key if still not found
        if (message == null) {
            plugin.logger.warning("Translation key not found: $key")
            return key
        }
        
        // Replace placeholders - use non-null assertion since we checked above
        var result = message!!
        for ((placeholder, value) in placeholders) {
            result = result.replace("{$placeholder}", value.toString())
        }
        
        // Replace literal \n with actual newlines
        result = result.replace("\\n", "\n")
        
        return result
    }
    
    /**
     * Get current language code.
     */
    fun getCurrentLanguage(): String = currentLanguage
    
    /**
     * Set the current language and reload language files.
     */
    fun setLanguage(language: String) {
        currentLanguage = language
        loadLanguage(language)
        
        // Update config
        configManager.set("plugin.language", language)
        configManager.saveConfig()
    }
    
    /**
     * Reload language files.
     */
    fun reloadLanguages() {
        loadLanguage(currentLanguage)
        loadFallbackLanguage()
        plugin.logger.info("Language files reloaded")
    }
    
    /**
     * Get available languages.
     */
    fun getAvailableLanguages(): List<String> {
        val languagesDir = File(File(plugin.dataFolder.parentFile, "Cloudly"), "languages")
        if (!languagesDir.exists()) {
            return listOf("en")
        }
        
        return languagesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".yml")
        }?.map { file ->
            file.nameWithoutExtension
        } ?: listOf("en")
    }
}
