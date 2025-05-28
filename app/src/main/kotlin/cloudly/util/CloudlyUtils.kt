/*
 * Cloudly - Utility Functions
 * 
 * This file contains common utility functions and performance optimizations
 * for the Cloudly plugin. All functions are null-safe and error-resistant.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import java.util.logging.Level

/**
 * Safe utility to get plugin instance
 */
private fun getPluginInstanceSafely(): CloudlyPlugin? {
    return try {
        CloudlyPlugin.instance
    } catch (e: Exception) {
        null
    }
}

/**
 * Safe utility to get logger
 */
private fun getLoggerSafely(): Logger {
    return try {
        getPluginInstanceSafely()?.logger ?: Logger.getLogger("Cloudly-Fallback")
    } catch (e: Exception) {
        Logger.getLogger("Cloudly-Fallback")
    }
}

/**
 * Utility object containing common functions used throughout the plugin
 * All methods are null-safe and handle errors gracefully
 */
object CloudlyUtils {
    
    // Create a coroutine scope for async operations
    private val pluginScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
      /**
     * Color code translation for chat messages
     * Uses & as the color code prefix - null-safe implementation
     */
    fun colorize(message: String?): String {
        if (message.isNullOrBlank()) return ""
        
        return try {
            ChatColor.translateAlternateColorCodes('&', message)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error colorizing message: ${e.message}")
            message
        }
    }
    
    /**
     * Send a colored message to a command sender - null-safe
     */
    fun sendMessage(sender: CommandSender?, message: String?) {
        if (sender == null || message.isNullOrBlank()) return
        
        try {
            sender.sendMessage(colorize(message))
        } catch (e: Exception) {
            getLoggerSafely().warning("Error sending message to sender: ${e.message}")
        }
    }
    
    /**
     * Send a message with the plugin prefix - null-safe
     */
    fun sendPrefixedMessage(sender: CommandSender?, message: String?, prefix: String? = null) {
        if (sender == null || message.isNullOrBlank()) return
        
        try {
            val safePrefix = prefix ?: getSafePrefix()
            sender.sendMessage(colorize(safePrefix + message))
        } catch (e: Exception) {
            getLoggerSafely().warning("Error sending prefixed message: ${e.message}")
            // Fallback: send message without prefix
            sendMessage(sender, message)
        }
    }
    
    /**
     * Send a translated message to a command sender - null-safe
     */    fun sendTranslatedMessage(sender: CommandSender?, key: String?, vararg args: Any?) {
        if (sender == null || key.isNullOrBlank()) return
        
        try {
            val message = getSafeTranslatedMessage(key, *(args.filterNotNull().toTypedArray()))
            sender.sendMessage(message)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error sending translated message: ${e.message}")
            // Fallback: send the key itself
            sendMessage(sender, key)
        }
    }
    
    /**
     * Send a translated message with the plugin prefix - null-safe
     */    fun sendPrefixedTranslatedMessage(sender: CommandSender?, key: String?, vararg args: Any?) {
        if (sender == null || key.isNullOrBlank()) return
        
        try {
            val prefix = getSafePrefix()
            val message = getSafeTranslatedMessage(key, *(args.filterNotNull().toTypedArray()))
            sender.sendMessage(prefix + message)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error sending prefixed translated message: ${e.message}")
            // Fallback: send translated message without prefix
            sendTranslatedMessage(sender, key, *(args.filterNotNull().toTypedArray()))
        }
    }
    
    /**
     * Check if a player has a specific permission - null-safe
     * Returns false if player or permission is null
     */
    fun hasPermission(player: Player?, permission: String?): Boolean {
        if (player == null || permission.isNullOrBlank()) return false
        
        return try {
            player.hasPermission(permission) || player.isOp
        } catch (e: Exception) {
            getLoggerSafely().warning("Error checking permission '$permission' for player '${player.name}': ${e.message}")
            false
        }
    }
    
    /**
     * Safe async execution with comprehensive error handling
     * Use this for any async operations to prevent plugin crashes
     */
    fun runAsync(action: suspend () -> Unit) {
        try {
            pluginScope.launch {
                try {
                    action()
                } catch (e: CancellationException) {
                    // Coroutine was cancelled, this is normal
                } catch (e: Exception) {
                    getLoggerSafely().log(Level.SEVERE, "Error in async operation: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            getLoggerSafely().log(Level.SEVERE, "Error launching async operation: ${e.message}", e)
        }
    }
      /**
     * Safe sync execution (run on main thread) - null-safe
     * Use this when you need to interact with Bukkit API from async context
     */
    fun runSync(plugin: org.bukkit.plugin.Plugin?, action: () -> Unit) {
        if (plugin == null) {
            getLoggerSafely().warning("Cannot run sync operation: plugin is null")
            return
        }
        
        try {
            plugin.server?.scheduler?.runTask(plugin, Runnable {
                try {
                    action()
                } catch (e: Exception) {
                    getLoggerSafely().log(Level.SEVERE, "Error in sync operation: ${e.message}", e)
                }
            })
        } catch (e: Exception) {
            getLoggerSafely().log(Level.SEVERE, "Error scheduling sync operation: ${e.message}", e)
        }
    }
    
    /**
     * Cancel all running coroutines - call this when the plugin is disabled
     * Safe to call multiple times
     */
    fun cleanup() {
        try {
            pluginScope.cancel("Plugin cleanup")
        } catch (e: Exception) {
            getLoggerSafely().warning("Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Log a message to console with proper color support - null-safe
     * This method sends colored messages directly to the console without timestamps
     * to avoid conflicts with server logging format
     */
    fun logColored(message: String?, level: Level = Level.INFO) {
        if (message.isNullOrBlank()) return
        
        try {
            // Strip any existing color codes and re-apply them properly
            val cleanMessage = ChatColor.stripColor(colorize(message)) ?: message
            val coloredMessage = when (level) {
                Level.SEVERE -> "§c[Cloudly] $cleanMessage"
                Level.WARNING -> "§e[Cloudly] $cleanMessage" 
                Level.INFO -> "§a[Cloudly] $cleanMessage"
                else -> "§7[Cloudly] $cleanMessage"
            }
            
            // Send directly to console sender to preserve colors
            val consoleSender = Bukkit.getConsoleSender()
            if (consoleSender != null) {
                consoleSender.sendMessage(coloredMessage)
            } else {
                // Fallback to standard logger if console sender is null
                fallbackLog(cleanMessage, level)
            }
        } catch (e: Exception) {
            // Fallback to standard logger if anything fails
            fallbackLog(message, level)
        }
    }
    
    /**
     * Fallback logging method when colored logging fails
     */
    private fun fallbackLog(message: String, level: Level) {
        try {
            val logger = getLoggerSafely()
            when (level) {
                Level.SEVERE -> logger.severe("[Cloudly] $message")
                Level.WARNING -> logger.warning("[Cloudly] $message")
                else -> logger.info("[Cloudly] $message")
            }
        } catch (e: Exception) {
            // Last resort: print to system err
            System.err.println("[Cloudly-ERROR] Failed to log message: $message")
        }
    }
    
    /**
     * Safe method to get prefix from LanguageManager
     */
    private fun getSafePrefix(): String {
        return try {
            LanguageManager.getPrefix()
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting prefix: ${e.message}")
            "&8[&bCloudly&8]&r " // Fallback prefix
        }
    }
      /**
     * Safe method to get translated message from LanguageManager
     */
    private fun getSafeTranslatedMessage(key: String, vararg args: Any): String {
        return try {
            LanguageManager.getMessage(key, *args)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting translated message for key '$key': ${e.message}")
            key // Fallback: return the key itself
        }
    }
}

/**
 * High-performance cache implementation using ConcurrentHashMap
 * Thread-safe and optimized for concurrent access - null-safe implementation
 */
class FastCache<K : Any, V : Any>(private val maxSize: Int = 1000) {
    
    private val cache = ConcurrentHashMap<K, V>()
    private val accessOrder = ConcurrentHashMap<K, Long>()
    
    /**
     * Get a value from the cache - null-safe
     */
    fun get(key: K?): V? {
        if (key == null) return null
        
        return try {
            val value = cache[key]
            if (value != null) {
                accessOrder[key] = System.currentTimeMillis()
            }
            value
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting value from cache: ${e.message}")
            null
        }
    }
    
    /**
     * Put a value into the cache - null-safe
     * Automatically removes oldest entries if cache exceeds max size
     */
    fun put(key: K?, value: V?) {
        if (key == null || value == null) return
        
        try {
            if (cache.size >= maxSize) {
                removeOldestEntry()
            }
            cache[key] = value
            accessOrder[key] = System.currentTimeMillis()
        } catch (e: Exception) {
            getLoggerSafely().warning("Error putting value into cache: ${e.message}")
        }
    }
    
    /**
     * Remove a specific key from the cache - null-safe
     */
    fun remove(key: K?): V? {
        if (key == null) return null
        
        return try {
            accessOrder.remove(key)
            cache.remove(key)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error removing value from cache: ${e.message}")
            null
        }
    }
    
    /**
     * Clear the entire cache - safe operation
     */
    fun clear() {
        try {
            cache.clear()
            accessOrder.clear()
        } catch (e: Exception) {
            getLoggerSafely().warning("Error clearing cache: ${e.message}")
        }
    }
      /**
     * Get current cache size - safe operation
     */
    fun size(): Int {
        return try {
            cache.size
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting cache size: ${e.message}")
            0
        }
    }
    
    /**
     * Check if cache contains a key - null-safe
     */
    fun containsKey(key: K?): Boolean {
        if (key == null) return false
        
        return try {
            cache.containsKey(key)
        } catch (e: Exception) {
            getLoggerSafely().warning("Error checking if cache contains key: ${e.message}")
            false
        }
    }
    
    /**
     * Remove the oldest accessed entry from the cache - safe operation
     */
    private fun removeOldestEntry() {
        try {
            val oldestKey = accessOrder.minByOrNull { it.value }?.key
            if (oldestKey != null) {
                cache.remove(oldestKey)
                accessOrder.remove(oldestKey)
            }
        } catch (e: Exception) {
            getLoggerSafely().warning("Error removing oldest cache entry: ${e.message}")
        }
    }
}

/**
 * Configuration helper for easy config access - null-safe implementation
 */
object ConfigHelper {

    private var plugin: org.bukkit.plugin.Plugin? = null
    private var language: String = "en" // Default language

    // Hardcoded performance settings
    private const val ASYNC_OPERATIONS = true
    private const val PLAYER_CACHE_SIZE = 1000
    private const val CACHE_EXPIRATION = 30

    /**
     * Initialize the config helper - null-safe
     */
    fun initialize(pluginInstance: org.bukkit.plugin.Plugin?) {
        plugin = pluginInstance
        if (pluginInstance != null) {
            try {
                // Load language from config, as it's not hardcoded
                language = pluginInstance.config?.getString("plugin.language", "en") ?: "en"
            } catch (e: Exception) {
                getLoggerSafely().warning("Error loading language from config: ${e.message}")
                language = "en" // Fallback to English
            }
        } else {
            getLoggerSafely().warning("Plugin instance is null during ConfigHelper initialization")
        }
    }

    /**
     * Check if ConfigHelper is properly initialized
     */
    fun isInitialized(): Boolean = plugin != null

    /**
     * Getter for debug (uses hardcoded value from CloudlyPlugin) - safe
     */
    fun isDebug(): Boolean {
        return try {
            CloudlyPlugin.DEBUG
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting debug flag: ${e.message}")
            false // Safe default
        }
    }

    /**
     * Getter for language (loaded from config) - safe
     */
    fun getLanguage(): String = language

    /**
     * Getter for async-operations (hardcoded) - safe
     */
    fun useAsyncOperations(): Boolean = ASYNC_OPERATIONS

    /**
     * Getter for player-cache-size (hardcoded) - safe
     */
    fun getPlayerCacheSize(): Int = PLAYER_CACHE_SIZE

    /**
     * Getter for cache-expiration (hardcoded) - safe
     */
    fun getCacheExpiration(): Int = CACHE_EXPIRATION
    
    /**
     * Get a string value from config with default - null-safe
     */
    fun getString(path: String?, default: String = ""): String {
        if (path.isNullOrBlank() || plugin == null) return default
        
        return try {
            plugin!!.config?.getString(path, default) ?: default
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting string config value for '$path': ${e.message}")
            default
        }
    }
    
    /**
     * Get an integer value from config with default - null-safe
     */
    fun getInt(path: String?, default: Int = 0): Int {
        if (path.isNullOrBlank() || plugin == null) return default
        
        return try {
            plugin!!.config?.getInt(path, default) ?: default
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting int config value for '$path': ${e.message}")
            default
        }
    }
    
    /**
     * Get a boolean value from config with default - null-safe
     */
    fun getBoolean(path: String?, default: Boolean = false): Boolean {
        if (path.isNullOrBlank() || plugin == null) return default
        
        return try {
            plugin!!.config?.getBoolean(path, default) ?: default
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting boolean config value for '$path': ${e.message}")
            default
        }
    }
    
    /**
     * Get a double value from config with default - null-safe
     */
    fun getDouble(path: String?, default: Double = 0.0): Double {
        if (path.isNullOrBlank() || plugin == null) return default
        
        return try {
            plugin!!.config?.getDouble(path, default) ?: default
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting double config value for '$path': ${e.message}")
            default
        }
    }
    
    /**
     * Get a string list from config with default - null-safe
     */
    fun getStringList(path: String?): List<String> {
        if (path.isNullOrBlank() || plugin == null) return emptyList()
        
        return try {
            plugin!!.config?.getStringList(path) ?: emptyList()
        } catch (e: Exception) {
            getLoggerSafely().warning("Error getting string list config value for '$path': ${e.message}")
            emptyList()
        }
    }
}