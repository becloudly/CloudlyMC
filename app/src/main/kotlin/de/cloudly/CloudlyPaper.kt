package de.cloudly

import de.cloudly.commands.CloudlyCommand
import de.cloudly.commands.VanillaWhitelistCommand
import de.cloudly.config.ConfigManager
import de.cloudly.discord.DiscordService
import de.cloudly.gui.AdminGuiManager
import de.cloudly.gui.WhitelistGuiManager
import de.cloudly.listeners.AntiCommandBlockListener
import de.cloudly.listeners.PlayerConnectionListener
import de.cloudly.listeners.PlayerChatListener
import de.cloudly.moderation.BanService
import de.cloudly.whitelist.WhitelistService
import de.cloudly.whitelist.attempts.WhitelistAttemptService
import org.bukkit.plugin.java.JavaPlugin

class CloudlyPaper : JavaPlugin() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var whitelistService: WhitelistService
    private lateinit var adminGuiManager: AdminGuiManager
    private lateinit var whitelistGuiManager: WhitelistGuiManager
    private lateinit var discordService: DiscordService
    private lateinit var queueService: de.cloudly.queue.QueueService
    private lateinit var whitelistAttemptService: WhitelistAttemptService
    private lateinit var playerConnectionListener: PlayerConnectionListener
    private lateinit var playerChatListener: PlayerChatListener
    private lateinit var discordVerificationListener: de.cloudly.listeners.DiscordVerificationListener
    private lateinit var antiCommandBlockListener: AntiCommandBlockListener
    private lateinit var cloudlyCommand: CloudlyCommand
    private lateinit var banService: BanService
    
    companion object {
        lateinit var instance: CloudlyPaper
            private set
    }
    
    /**
     * Get the whitelist service instance
     */
    fun getWhitelistService(): WhitelistService {
        if (!::whitelistService.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return whitelistService
    }

    /**
     * Get the whitelist GUI manager instance
     */
    fun getAdminGuiManager(): AdminGuiManager {
        if (!::adminGuiManager.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return adminGuiManager
    }

    fun getWhitelistGuiManager(): WhitelistGuiManager {
        if (!::whitelistGuiManager.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return whitelistGuiManager
    }

    fun getBanService(): BanService {
        if (!::banService.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return banService
    }

    /**
     * Get the Discord service instance
     */
    fun getDiscordService(): DiscordService {
        if (!::discordService.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return discordService
    }
    
    /**
     * Get the queue service instance
     */
    fun getQueueService(): de.cloudly.queue.QueueService {
        if (!::queueService.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return queueService
    }

    fun getWhitelistAttemptService(): WhitelistAttemptService {
        if (!::whitelistAttemptService.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return whitelistAttemptService
    }
    
    /**
     * Get the Discord verification listener instance
     */
    fun getDiscordVerificationListener(): de.cloudly.listeners.DiscordVerificationListener {
        if (!::discordVerificationListener.isInitialized) {
            throw IllegalStateException("Plugin not fully initialized")
        }
        return discordVerificationListener
    }
    
    override fun onEnable() {
        // Set plugin instance
        instance = this
        
        // Initialize configuration system
        configManager = ConfigManager(this)
        configManager.initialize()
        initializeConfigDefaults()
        
        // Register commands
        registerCommands()
        
        // Log plugin information
        logger.info(Messages.Plugin.enabled(description.version))
        
        // Log debug mode if enabled
        val debugMode = configManager.getBoolean("plugin.debug", false)
        if (debugMode) {
            logger.info(Messages.Plugin.DEBUG_ENABLED)
        }
        
        // Initialize whitelist service
        whitelistService = WhitelistService(this)
        whitelistAttemptService = WhitelistAttemptService()
        whitelistService.setAttemptService(whitelistAttemptService)
        whitelistService.initialize()
        
        // Initialize moderation/ban service
        banService = BanService(this, whitelistService)
        banService.initialize()
        
        // Initialize GUI manager
        adminGuiManager = AdminGuiManager(this)
        whitelistGuiManager = WhitelistGuiManager(this)
        
        // Initialize Discord service
        discordService = DiscordService(this)
        discordService.initialize()
        
        // Initialize connection queue service
        queueService = de.cloudly.queue.QueueService(this)
        queueService.initialize()
        
        // Initialize player connection listener
        playerConnectionListener = PlayerConnectionListener(this)
        server.pluginManager.registerEvents(playerConnectionListener, this)
        
        // Initialize player chat listener
        playerChatListener = PlayerChatListener(this)
        server.pluginManager.registerEvents(playerChatListener, this)
        
        // Initialize Discord verification listener
        discordVerificationListener = de.cloudly.listeners.DiscordVerificationListener(this)
        server.pluginManager.registerEvents(discordVerificationListener, this)

        // Initialize anti command block listener
        antiCommandBlockListener = AntiCommandBlockListener(this)
        server.pluginManager.registerEvents(antiCommandBlockListener, this)
        antiCommandBlockListener.runInitialScan()
    }
    
    override fun onDisable() {
        // Close all open GUIs
        if (::adminGuiManager.isInitialized) {
            adminGuiManager.closeAll()
        }
        if (::whitelistGuiManager.isInitialized) {
            whitelistGuiManager.closeAll()
        }
        
        // Close whitelist service resources
        if (::whitelistService.isInitialized) {
            whitelistService.shutdown()
        }
        
        if (::banService.isInitialized) {
            banService.shutdown()
        }

        if (::whitelistAttemptService.isInitialized) {
            whitelistAttemptService.clear()
        }
        
        // Close Discord service resources
        if (::discordService.isInitialized) {
            discordService.shutdown()
        }
        
        // Close queue service resources
        if (::queueService.isInitialized) {
            queueService.shutdown()
        }
        
        // Close Discord verification listener resources
        if (::discordVerificationListener.isInitialized) {
            discordVerificationListener.shutdown()
        }
        
        if (::antiCommandBlockListener.isInitialized) {
            antiCommandBlockListener.shutdown()
        }

        // Close command handler resources
        if (::cloudlyCommand.isInitialized) {
            cloudlyCommand.shutdown()
        }
        
        // Save configuration before shutdown
        if (::configManager.isInitialized) {
            configManager.saveConfig()
        }
        
        logger.info(Messages.Plugin.DISABLED)
    }
    
    private fun initializeConfigDefaults() {
        var changed = false
        if (!configManager.contains("protections.anti_command_block.enabled")) {
            configManager.set("protections.anti_command_block.enabled", true)
            changed = true
        }
        if (!configManager.contains("protections.anti_command_block.notify_admins")) {
            configManager.set("protections.anti_command_block.notify_admins", true)
            changed = true
        }
        if (changed) {
            configManager.saveConfig()
        }
    }

    /**
     * Register all plugin commands.
     */
    private fun registerCommands() {
        // Register main Cloudly command
        getCommand("cloudly")?.let { command ->
            cloudlyCommand = CloudlyCommand(this)
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
}
