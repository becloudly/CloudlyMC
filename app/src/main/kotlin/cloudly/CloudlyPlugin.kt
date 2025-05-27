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
     * Called when the plugin is enabled
     * This is where initialization logic should go
     */
    override fun onEnable() {
        instance = this
        
        // First save default config and initialize language manager
        // since we need it for logging with translations
        saveDefaultConfig()
        LanguageManager.initialize(this)
        
        // Log plugin startup with version info using raw logger initially
        // (since we need to ensure language files are loaded first)
        logger.info(LanguageManager.getMessage("system.startup", description.version))
        logger.info(LanguageManager.getMessage("system.running-on", server.name, server.version))
        
        try {
            // Initialize plugin components here
            initializePlugin()
            
            logger.info(LanguageManager.getMessage("system.enabled-success"))
        } catch (e: Exception) {
            logger.log(Level.SEVERE, LanguageManager.getMessage("system.enable-failed"), e)
            server.pluginManager.disablePlugin(this)
        }
    }
    
    /**
     * Called when the plugin is disabled
     * This is where cleanup logic should go
     */
    override fun onDisable() {
        try {
            // Cleanup plugin components here
            cleanupPlugin()
            
            logger.info(LanguageManager.getMessage("system.shutdown"))
        } catch (e: Exception) {
            logger.log(Level.SEVERE, LanguageManager.getMessage("system.shutdown-error"), e)
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
