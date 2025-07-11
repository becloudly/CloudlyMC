package de.cloudly

import de.cloudly.config.ConfigManager
import de.cloudly.config.LanguageManager
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var languageManager: LanguageManager
    
    companion object {
        lateinit var instance: CloudlyPaper
            private set
    }

    override fun onEnable() {
        // Set plugin instance
        instance = this
        
        // Initialize configuration system
        configManager = ConfigManager(this)
        configManager.initialize()
        
        // Initialize language system
        languageManager = LanguageManager(this, configManager)
        languageManager.initialize()
        
        // Connect language manager to config manager for translated messages
        configManager.setLanguageManager(languageManager)
        
        // Log plugin information using translations
        logger.info(languageManager.getMessage("plugin.enabled", "version" to description.version))
        
        // Log configuration status
        val debugMode = configManager.getBoolean("plugin.debug", false)
        if (debugMode) {
            logger.info(languageManager.getMessage("plugin.debug_enabled"))
        }
    }
    
    override fun onDisable() {
        // Save configuration before shutdown
        if (::configManager.isInitialized) {
            configManager.saveConfig()
        }
        
        if (::languageManager.isInitialized) {
            logger.info(languageManager.getMessage("plugin.disabled"))
        } else {
            logger.info("Cloudly Plugin disabled")
        }
    }
    
    /**
     * Get the configuration manager instance.
     */
    fun getConfigManager(): ConfigManager = configManager
    
    /**
     * Get the language manager instance.
     */
    fun getLanguageManager(): LanguageManager = languageManager
}
