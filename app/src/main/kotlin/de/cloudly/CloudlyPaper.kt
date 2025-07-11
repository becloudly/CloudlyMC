package de.cloudly

import de.cloudly.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    
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
        
        // Log plugin information
        logger.info("Cloudly Plugin v${description.version} enabled on Paper!")
        
        // Log configuration status
        val debugMode = configManager.getBoolean("plugin.debug", false)
        if (debugMode) {
            logger.info("Debug mode is enabled")
        }
    }
    
    override fun onDisable() {
        // Save configuration before shutdown
        if (::configManager.isInitialized) {
            configManager.saveConfig()
        }
        logger.info("Cloudly Plugin disabled")
    }
    
    /**
     * Get the configuration manager instance.
     */
    fun getConfigManager(): ConfigManager = configManager
}
