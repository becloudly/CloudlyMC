package de.cloudly

import de.cloudly.config.ConfigManager
import de.cloudly.config.LanguageManager
import de.cloudly.radar.ReleaseRadar
import de.cloudly.utils.SchedulerUtils
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var languageManager: LanguageManager
    private lateinit var releaseRadar: ReleaseRadar
    
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
        
        // Initialize release radar
        releaseRadar = ReleaseRadar(this, configManager, languageManager)
        releaseRadar.initialize()
        
        // Log plugin information using translations
        logger.info(languageManager.getMessage("plugin.enabled", "version" to description.version))
        
        // Log server type detection
        val serverType = if (SchedulerUtils.isFolia()) "Folia" else "Paper/Spigot"
        logger.info("Detected server type: $serverType")
        
        // Log configuration status
        val debugMode = configManager.getBoolean("plugin.debug", false)
        if (debugMode) {
            logger.info(languageManager.getMessage("plugin.debug_enabled"))
        }
    }
    
    override fun onDisable() {
        // Shutdown release radar
        if (::releaseRadar.isInitialized) {
            releaseRadar.shutdown()
        }
        
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
    
    /**
     * Get the release radar instance.
     */
    fun getReleaseRadar(): ReleaseRadar = releaseRadar
}
