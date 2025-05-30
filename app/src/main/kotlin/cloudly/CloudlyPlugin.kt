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
import kotlinx.coroutines.*

/**
 * Main plugin class for Cloudly
 * 
 * This serves as the entry point for the plugin and handles:
 * - Plugin lifecycle (onEnable, onDisable)
 * - Configuration management
 * - Command registration
 * - Event listener registration
 * 
 * All methods are null-safe and handle errors gracefully
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
        
        /**
         * Safe way to get plugin instance
         * Returns null if instance is not initialized yet
         */
        fun getInstanceSafely(): CloudlyPlugin? {
            return try {
                instance
            } catch (e: Exception) {
                null
            }
        }    }
    
    // Plugin coroutine scope with proper lifecycle management
    private val pluginScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + 
        CoroutineExceptionHandler { _, exception ->
            logger.log(Level.SEVERE, "Unhandled coroutine exception", exception)
        }
    )
    
    // Track initialization state
    private var isInitialized = false
    private var isShuttingDown = false
      /**
     * Called when the plugin is loading
     * Note: Server automatically logs "Loading server plugin" message
     */
    override fun onLoad() {
        // Server automatically handles loading message
        // No additional logging needed here to avoid duplicates
    }
      /**
     * Called when the plugin is enabled
     * This is where initialization logic should go
     * Note: Server automatically logs "Enabling" message
     */
    override fun onEnable() {
        try {
            instance = this
            
            // Server automatically handles enabling message
            // No additional logging needed here to avoid duplicates
            
            // Initialize plugin components
            if (initializePlugin()) {
                isInitialized = true
                
                // Now we can use colored logging since everything is initialized
                val version = description?.version ?: "unknown"
                safeLogColored("system.startup", version)
                safeLogColored("system.running-on", getServerName(), getServerVersion())
                safeLogColored("system.enabled-success")
            } else {
                throw RuntimeException("Plugin initialization failed")
            }
            
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Critical error during plugin enable", e)
            safeLogColored("system.enable-failed", Level.SEVERE)
            
            try {
                server?.pluginManager?.disablePlugin(this)
            } catch (disableError: Exception) {
                logger.log(Level.SEVERE, "Failed to disable plugin after initialization error", disableError)
            }
        }
    }
      /**
     * Called when the plugin is disabled
     * This is where cleanup logic should go
     */
    override fun onDisable() {
        isShuttingDown = true
        
        try {
            // Cancel all plugin coroutines first
            pluginScope.cancel("Plugin disabled")
            
            // Cleanup plugin components
            cleanupPlugin()
            
            safeLogColored("system.shutdown")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during plugin disable", e)
            safeLogColored("system.shutdown-error", Level.SEVERE)
        } finally {
            isInitialized = false
        }
    }
    
    /**
     * Initialize plugin components
     * Returns true if initialization was successful, false otherwise
     */
    private fun initializePlugin(): Boolean {
        return try {
            // Save default config first
            saveDefaultConfigSafely()
            
            // Initialize language manager (required for translated logging)
            if (!initializeLanguageManager()) {
                logger.severe("Failed to initialize language manager")
                return false
            }
            
            // Initialize configuration helper
            if (!initializeConfigHelper()) {
                logger.severe("Failed to initialize config helper")
                return false
            }
            
            // Register event listeners
            registerEventListeners()
            
            // Register commands
            registerCommands()
            
            // Initialize any other managers, services, or components here
            initializeComponents()
            
            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during plugin initialization", e)
            false
        }
    }
    
    /**
     * Safely save default configuration
     */
    private fun saveDefaultConfigSafely() {
        try {
            saveDefaultConfig()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to save default config", e)
        }
    }
    
    /**
     * Initialize language manager safely
     */
    private fun initializeLanguageManager(): Boolean {
        return try {
            LanguageManager.initialize(this)
            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize language manager", e)
            false
        }
    }
    
    /**
     * Initialize config helper safely
     */
    private fun initializeConfigHelper(): Boolean {
        return try {
            ConfigHelper.initialize(this)
            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize config helper", e)
            false
        }
    }
    
    /**
     * Register event listeners safely
     */
    private fun registerEventListeners() {
        try {
            server?.pluginManager?.registerEvents(this, this)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to register event listeners", e)
        }
    }
    
    /**
     * Register commands safely
     */
    private fun registerCommands() {
        try {
            val cloudlyCommand = getCommand("cloudly")
            if (cloudlyCommand != null) {
                cloudlyCommand.setExecutor(CloudlyCommand())
            } else {
                logger.warning("Command 'cloudly' not found in plugin.yml")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to register commands", e)
        }
    }
    
    /**
     * Initialize additional components
     */
    private fun initializeComponents() {
        try {
            // Initialize any managers, services, or components here
            // Example: playerManager = PlayerManager()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to initialize some components", e)
        }
    }
      /**
     * Cleanup plugin components safely
     */
    private fun cleanupPlugin() {
        try {
            // Cancel all running tasks
            server?.scheduler?.cancelTasks(this)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error canceling tasks", e)
        }
        
        try {
            // Cleanup utilities (clear caches, etc.)
            CloudlyUtils.cleanup()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error cleaning up utilities", e)
        }
        
        try {
            // Close any resources, databases, etc.
            // Example: database?.close()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error closing resources", e)
        }
        
        try {
            // Clear any caches or temporary data
            // Example: playerManager?.clearCache()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error clearing caches", e)
        }
    }
    
    /**
     * Reload the plugin configuration and components
     * Call this method when you want to reload the plugin without restarting
     */
    fun reloadPlugin() {
        if (isShuttingDown) {
            logger.warning("Cannot reload plugin while shutting down")
            return
        }
        
        try {
            safeLogColored("system.reloading")

            // Reload configuration
            try {
                reloadConfig()
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to reload config", e)
            }
            
            // Reload language files
            try {
                LanguageManager.reload(this)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to reload language manager", e)
            }
            
            // Re-initialize config helper
            try {
                ConfigHelper.initialize(this)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to re-initialize config helper", e)
            }

            // Reinitialize components that depend on config
            try {
                reinitializeConfigDependentComponents()
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to reinitialize some components", e)
            }

            safeLogColored("common.reload-success")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during plugin reload", e)
            safeLogColored("common.reload-failed", Level.SEVERE)
        }
    }
    
    /**
     * Reinitialize components that depend on configuration
     */
    private fun reinitializeConfigDependentComponents() {
        // Add logic here for components that need to be reinitialized after config reload
    }
    
    /**
     * Safe way to log colored messages
     */
    private fun safeLogColored(key: String, vararg args: Any, level: Level = Level.INFO) {
        try {
            if (isInitialized && !isShuttingDown) {
                CloudlyUtils.logColored(LanguageManager.getMessage(key, *args), level)
            } else {
                // Fallback to regular logging if colored logging is not available
                logger.log(level, "[$key] ${args.joinToString(", ")}")
            }
        } catch (e: Exception) {
            // Final fallback
            logger.log(level, "Error logging message for key '$key': ${e.message}")
        }
    }
    
    /**
     * Safe way to get server name
     */
    private fun getServerName(): String {
        return try {
            server?.name ?: "Unknown Server"
        } catch (e: Exception) {
            "Unknown Server"
        }
    }
    
    /**
     * Safe way to get server version
     */
    private fun getServerVersion(): String {
        return try {
            server?.version ?: "Unknown Version"
        } catch (e: Exception) {
            "Unknown Version"
        }
    }
      /**
     * Check if plugin is properly initialized
     */
    fun isPluginInitialized(): Boolean = isInitialized && !isShuttingDown
    
    /**
     * Get the plugin's coroutine scope for async operations
     * This scope is automatically cancelled when the plugin is disabled
     */
    fun getPluginScope(): CoroutineScope = pluginScope
}