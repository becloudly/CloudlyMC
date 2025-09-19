package de.cloudly

import de.cloudly.commands.CloudlyCommand
import de.cloudly.commands.WhitelistCommand
import de.cloudly.config.ConfigManager
import de.cloudly.config.HotReloadManager
import de.cloudly.config.LanguageManager
import de.cloudly.utils.SchedulerUtils
import de.cloudly.whitelist.WhitelistService
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var languageManager: LanguageManager
    private lateinit var hotReloadManager: HotReloadManager
    private lateinit var whitelistService: WhitelistService
    
    companion object {
        lateinit var instance: CloudlyPaper
            private set
    }
    
    /**
     * Get the whitelist service instance
     */
    fun getWhitelistService(): WhitelistService {
        return whitelistService
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
        
        // Initialize hot-reload manager
        hotReloadManager = HotReloadManager(this)
        
        // Register commands
        registerCommands()
        
        // Log plugin information using translations
        logger.info(languageManager.getMessage("plugin.enabled", "version" to description.version))
        
        // Log server type detection
        val serverType = if (SchedulerUtils.isFolia()) "Folia" else "Paper/Spigot"
        logger.info("Detected server type: $serverType")
        
        // Log configuration status
        val debugMode = configManager.getBoolean("plugin.debug", false)
        
        // Initialize whitelist service
        whitelistService = WhitelistService(this)
        whitelistService.initialize()
        if (debugMode) {
            logger.info(languageManager.getMessage("plugin.debug_enabled"))
        }
    }
    
    override fun onDisable() {
        // Close whitelist service resources
        if (::whitelistService.isInitialized) {
            whitelistService.shutdown()
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
     * Register all plugin commands.
     */
    private fun registerCommands() {
        getCommand("cloudly")?.let { command ->
            val cloudlyCommand = CloudlyCommand(this)
            command.setExecutor(cloudlyCommand)
            command.tabCompleter = cloudlyCommand
        }
        getCommand("whitelist")?.let { command ->
            val whitelistCommand = WhitelistCommand(this)
            command.setExecutor(whitelistCommand)
            command.tabCompleter = whitelistCommand
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
     * Get the hot-reload manager instance.
     */
    fun getHotReloadManager(): HotReloadManager = hotReloadManager
}
