package de.cloudly

import de.cloudly.commands.CloudlyCommand
import de.cloudly.commands.VanillaWhitelistCommand
import de.cloudly.config.ConfigManager
import de.cloudly.config.HotReloadManager
import de.cloudly.config.LanguageManager
import de.cloudly.discord.DiscordService
import de.cloudly.gui.WhitelistGuiManager
import de.cloudly.listeners.PlayerConnectionListener
import de.cloudly.listeners.PlayerChatListener
import de.cloudly.utils.SchedulerUtils
import de.cloudly.whitelist.WhitelistService
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var languageManager: LanguageManager
    private lateinit var hotReloadManager: HotReloadManager
    private lateinit var whitelistService: WhitelistService
    private lateinit var whitelistGuiManager: WhitelistGuiManager
    private lateinit var discordService: DiscordService
    private lateinit var playerConnectionListener: PlayerConnectionListener
    private lateinit var playerChatListener: PlayerChatListener
    
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

    /**
     * Get the whitelist GUI manager instance
     */
    fun getWhitelistGuiManager(): WhitelistGuiManager {
        return whitelistGuiManager
    }

    /**
     * Get the Discord service instance
     */
    fun getDiscordService(): DiscordService {
        return discordService
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
        val debugMode = configManager.getBoolean("plugin.debug", false)
        val serverType = if (SchedulerUtils.isFolia(debugMode)) "Folia" else "Paper/Spigot"
        if (debugMode) {
            logger.info("Detected server type: $serverType")
        }
        
        // Initialize whitelist service
        whitelistService = WhitelistService(this)
        whitelistService.initialize()
        
        // Initialize GUI manager
        whitelistGuiManager = WhitelistGuiManager(this)
        
        // Initialize Discord service
        discordService = DiscordService(this)
        discordService.initialize()
        
        // Initialize player connection listener
        playerConnectionListener = PlayerConnectionListener(this)
        server.pluginManager.registerEvents(playerConnectionListener, this)
        
        // Initialize player chat listener
        playerChatListener = PlayerChatListener(this)
        server.pluginManager.registerEvents(playerChatListener, this)
        
        if (debugMode) {
            logger.info(languageManager.getMessage("plugin.debug_enabled"))
        }
    }
    
    override fun onDisable() {
        // Close all open GUIs
        if (::whitelistGuiManager.isInitialized) {
            whitelistGuiManager.closeAllGuis()
        }
        
        // Close whitelist service resources
        if (::whitelistService.isInitialized) {
            whitelistService.shutdown()
        }
        
        // Close Discord service resources
        if (::discordService.isInitialized) {
            discordService.shutdown()
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
        // Register main Cloudly command
        getCommand("cloudly")?.let { command ->
            val cloudlyCommand = CloudlyCommand(this)
            command.setExecutor(cloudlyCommand)
            command.tabCompleter = cloudlyCommand
        }
        
        // Register vanilla whitelist command override to disable it
        getCommand("whitelist")?.let { command ->
            val vanillaWhitelistCommand = VanillaWhitelistCommand(this)
            command.setExecutor(vanillaWhitelistCommand)
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
