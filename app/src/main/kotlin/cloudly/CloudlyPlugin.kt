/*
 * Cloudly - High-performance Minecraft Plugin
 * Version: 1.0.0.0-dev (Epoch.Major.Minor.Patch-TAG)
 * 
 * Optimized for:
 * - Minecraft 1.18+
 * - PaperMC servers
 * - Java 17+
 * - High performance & low memory usage
 */
package cloudly

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import java.util.logging.Level
import cloudly.util.ConfigHelper
import cloudly.util.LanguageManager
import cloudly.util.CloudlyUtils
import cloudly.command.CloudlyCommand

/**
 * Main plugin class for Cloudly
 * 
 * This serves as the entry point for the plugin and handles:
 * - Plugin lifecycle (onEnable, onDisable)
 * - Configuration management
 * - Command registration
 * - Event listener registration
 */
class CloudlyPlugin : JavaPlugin(), Listener {

    companion object {
        /**
         * Plugin instance for global access
         * Use sparingly to maintain good architecture
         */
        lateinit var instance: CloudlyPlugin
            private set

        // Hardcoded plugin settings
        const val DEBUG = true // Default from config.yml
    }
    
    /**
     * Called when the plugin is loading - used to colorize the loading message
     */
    override fun onLoad() {
        // Use a custom colored message for plugin loading
        val consoleMessage = "§b[Cloudly] §fLoading server plugin Cloudly v${description.version}"
        server.consoleSender.sendMessage(consoleMessage)
    }
      /**
     * Called when the plugin is enabled
     * This is where initialization logic should go
     */    override fun onEnable() {
        instance = this
        
        // Send colored plugin enabling message
        server.consoleSender.sendMessage("§7[INFO]: §r[Cloudly] §fEnabling Cloudly v${description.version}")
        
        // First save default config and initialize language manager
        // since we need it for logging with translations
        saveDefaultConfig()
        LanguageManager.initialize(this)
        
        // Log plugin startup with version info using colored console output
        // instead of raw logger to ensure color codes are rendered properly
        CloudlyUtils.logColored(LanguageManager.getMessage("system.startup", description.version))
        CloudlyUtils.logColored(LanguageManager.getMessage("system.running-on", server.name, server.version))
          try {
            // Initialize plugin components here
            initializePlugin()
            
            CloudlyUtils.logColored(LanguageManager.getMessage("system.enabled-success"))
        } catch (e: Exception) {
            CloudlyUtils.logColored(LanguageManager.getMessage("system.enable-failed"), Level.SEVERE)
            logger.log(Level.SEVERE, "Error details:", e)
            server.pluginManager.disablePlugin(this)
        }
    }
    
    /**
     * Called when the plugin is disabled
     * This is where cleanup logic should go
     */    override fun onDisable() {
        try {
            // Cleanup plugin components here
            cleanupPlugin()
            
            CloudlyUtils.logColored(LanguageManager.getMessage("system.shutdown"))
        } catch (e: Exception) {
            CloudlyUtils.logColored(LanguageManager.getMessage("system.shutdown-error"), Level.SEVERE)
            logger.log(Level.SEVERE, "Error details:", e)
        }
    }
      /**
     * Initialize plugin components
     * Add your initialization logic here
     */
    private fun initializePlugin() {
        // Language manager is already initialized in onEnable
        // to enable translated logging during startup
        
        // Initialize configuration helper
        ConfigHelper.initialize(this)
          // Register event listeners here
        // server.pluginManager.registerEvents(this, this)
        
        // Register commands here
        getCommand("cloudly")?.setExecutor(CloudlyCommand())
        
        // Initialize any managers, services, or components here
        // Example: playerManager = PlayerManager()
    }
    
    /**
     * Cleanup plugin components
     * Add your cleanup logic here
     */
    private fun cleanupPlugin() {
        // Cancel all running tasks
        server.scheduler.cancelTasks(this)
        
        // Close any resources, databases, etc.
        // Example: database.close()
        
        // Clear any caches or temporary data
        // Example: playerManager.clearCache()
    }
    
    /**
     * Reload the plugin configuration and components
     * Call this method when you want to reload the plugin without restarting
     */
    fun reloadPlugin() {
        try {
            logger.info(LanguageManager.getMessage("system.reloading"))

            // Reload configuration for settings not hardcoded
            reloadConfig()
            
            // Reload language files
            LanguageManager.reload(this)
            
            // Re-initialize for any non-hardcoded settings
            ConfigHelper.initialize(this)

            // Reinitialize components that depend on config
            // Example: reinitializeConfigDependentComponents()

            logger.info(LanguageManager.getMessage("common.reload-success"))
        } catch (e: Exception) {
            logger.log(Level.SEVERE, LanguageManager.getMessage("common.reload-failed"), e)
        }
    }
}
