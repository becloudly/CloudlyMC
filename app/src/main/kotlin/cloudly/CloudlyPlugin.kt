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
import cloudly.command.ExampleCommand

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
        
        // Log plugin startup with version info
        logger.info("Cloudly v${description.version} is starting up...")
        logger.info("Running on ${server.name} ${server.version}")
        logger.info("Java version: ${System.getProperty("java.version")}")
        
        try {
            // Initialize plugin components here
            initializePlugin()
            
            logger.info("Cloudly has been enabled successfully!")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to enable Cloudly", e)
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
            
            logger.info("Cloudly has been disabled successfully!")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during plugin shutdown", e)
        }
    }
      /**
     * Initialize plugin components
     * Add your initialization logic here
     */
    private fun initializePlugin() {
        // Save default config if it doesn't exist
        saveDefaultConfig()

        // Initialize configuration helper
        // ConfigHelper.initialize(this) // ConfigHelper will use hardcoded values or access config directly for non-plugin settings
        
        // Register event listeners here
        // server.pluginManager.registerEvents(this, this)
        
        // Register commands here
        getCommand("cloudly")?.setExecutor(ExampleCommand())
        
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
            logger.info("Reloading Cloudly...")

            // Reload configuration for settings not hardcoded
            reloadConfig()
            ConfigHelper.initialize(this) // Re-initialize for any non-hardcoded settings

            // Reinitialize components that depend on config
            // Example: reinitializeConfigDependentComponents()

            logger.info("Cloudly reloaded successfully!")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to reload Cloudly", e)
        }
    }
}
