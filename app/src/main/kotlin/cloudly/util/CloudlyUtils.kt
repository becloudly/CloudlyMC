/*
 * Cloudly - Utility Functions
 * 
 * This file contains common utility functions and performance optimizations
 * for the Cloudly plugin.
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
 * Extension function to check if a Kotlin object is initialized
 */
fun <T : Any> Class<T>.isInitialized(): Boolean {
    return try {
        // For Kotlin object classes, we can check if INSTANCE is accessible
        this.getDeclaredField("INSTANCE").get(null) != null
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Utility object containing common functions used throughout the plugin
 */
object CloudlyUtils {
    
    private val logger: Logger = Logger.getLogger("Cloudly")
    
    // Create a coroutine scope for async operations
    private val pluginScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Color code translation for chat messages
     * Uses & as the color code prefix
     */
    fun colorize(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }
    
    /**
     * Send a colored message to a command sender
     */
    fun sendMessage(sender: CommandSender, message: String) {
        sender.sendMessage(colorize(message))
    }
    
    /**
     * Send a message with the plugin prefix
     */
    fun sendPrefixedMessage(sender: CommandSender, message: String, prefix: String = LanguageManager.getPrefix()) {
        sender.sendMessage(colorize(prefix + message))
    }
    
    /**
     * Send a translated message to a command sender
     */
    fun sendTranslatedMessage(sender: CommandSender, key: String, vararg args: Any) {
        sender.sendMessage(LanguageManager.getMessage(key, *args))
    }
    
    /**
     * Send a translated message with the plugin prefix
     */
    fun sendPrefixedTranslatedMessage(sender: CommandSender, key: String, vararg args: Any) {
        sender.sendMessage(LanguageManager.getPrefixedMessage(key, *args))
    }
    
    /**
     * Check if a player has a specific permission
     * Returns true if player has permission or is OP
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        return player.hasPermission(permission) || player.isOp
    }
      /**
     * Safe async execution with error handling
     * Use this for any async operations to prevent plugin crashes
     */
    fun runAsync(action: suspend () -> Unit) {
        pluginScope.launch {
            try {
                action()
            } catch (e: Exception) {
                logger.severe("Error in async operation: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Safe sync execution (run on main thread)
     * Use this when you need to interact with Bukkit API from async context
     */
    fun runSync(plugin: org.bukkit.plugin.Plugin, action: () -> Unit) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                action()
            } catch (e: Exception) {
                logger.severe("Error in sync operation: ${e.message}")
                e.printStackTrace()
            }
        })
    }
    
    /**
     * Cancel all running coroutines - call this when the plugin is disabled
     */
    fun cleanup() {
        pluginScope.cancel()
    }
      /**
     * Log a message to console with proper color support
     * This uses Bukkit's console sender instead of java.util.logging
     * to ensure color codes are properly rendered
     */
    fun logColored(message: String, level: Level = Level.INFO) {
        val coloredMessage = "[Cloudly] " + colorize(message)
        
        when (level) {
            Level.SEVERE -> Bukkit.getConsoleSender().sendMessage("§c" + coloredMessage)
            Level.WARNING -> Bukkit.getConsoleSender().sendMessage("§e" + coloredMessage)
            Level.INFO -> Bukkit.getConsoleSender().sendMessage(coloredMessage)
            else -> Bukkit.getConsoleSender().sendMessage("§7" + coloredMessage)
        }
    }
    
    /**
     * Format a console message to match the server log format with colors
     * This creates a message that looks like: [HH:MM:SS] [Level]: [Cloudly] Message
     * Used for special cases where we need to maintain log format consistency
     */
    fun logFormattedColored(message: String, level: Level = Level.INFO) {
        // Get current time for timestamp
        val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        
        // Create the formatted message with timestamp
        val formattedMessage = when (level) {
            Level.SEVERE -> "§7[${timestamp}] §c[SEVERE]: §r[Cloudly] " + colorize(message)
            Level.WARNING -> "§7[${timestamp}] §e[WARNING]: §r[Cloudly] " + colorize(message)
            Level.INFO -> "§7[${timestamp}] §7[INFO]: §r[Cloudly] " + colorize(message) 
            else -> "§7[${timestamp}] §7[${level.name}]: §r[Cloudly] " + colorize(message)
        }
        
        // Send to console
        Bukkit.getConsoleSender().sendMessage(formattedMessage)
    }
}

/**
 * High-performance cache implementation using ConcurrentHashMap
 * Thread-safe and optimized for concurrent access
 */
class FastCache<K, V>(private val maxSize: Int = 1000) {
    
    private val cache = ConcurrentHashMap<K, V>()
    private val accessOrder = ConcurrentHashMap<K, Long>()
    
    /**
     * Get a value from the cache
     */
    fun get(key: K): V? {
        val value = cache[key]
        if (value != null) {
            accessOrder[key] = System.currentTimeMillis()
        }
        return value
    }
    
    /**
     * Put a value into the cache
     * Automatically removes oldest entries if cache exceeds max size
     */
    fun put(key: K, value: V) {
        if (cache.size >= maxSize) {
            removeOldestEntry()
        }
        cache[key] = value
        accessOrder[key] = System.currentTimeMillis()
    }
    
    /**
     * Remove a specific key from the cache
     */
    fun remove(key: K): V? {
        accessOrder.remove(key)
        return cache.remove(key)
    }
    
    /**
     * Clear the entire cache
     */
    fun clear() {
        cache.clear()
        accessOrder.clear()
    }
    
    /**
     * Get current cache size
     */
    fun size(): Int = cache.size
    
    /**
     * Check if cache contains a key
     */
    fun containsKey(key: K): Boolean = cache.containsKey(key)
    
    /**
     * Remove the oldest accessed entry from the cache
     */
    private fun removeOldestEntry() {
        val oldestKey = accessOrder.minByOrNull { it.value }?.key
        if (oldestKey != null) {
            cache.remove(oldestKey)
            accessOrder.remove(oldestKey)
        }
    }
}

/**
 * Configuration helper for easy config access
 */
object ConfigHelper {

    private lateinit var plugin: org.bukkit.plugin.Plugin
    private var language: String = "en" // Default language

    // Hardcoded performance settings
    private const val ASYNC_OPERATIONS = true
    private const val PLAYER_CACHE_SIZE = 1000
    private const val CACHE_EXPIRATION = 30


    fun initialize(pluginInstance: org.bukkit.plugin.Plugin) {
        plugin = pluginInstance
        // Load language from config, as it's not hardcoded
        language = plugin.config.getString("plugin.language", "en") ?: "en"
    }

    // Getter for debug (uses hardcoded value from CloudlyPlugin)
    fun isDebug(): Boolean {
        return CloudlyPlugin.DEBUG
    }

    // Getter for language (loaded from config)
    fun getLanguage(): String {
        return language
    }

    // Getter for async-operations (hardcoded)
    fun useAsyncOperations(): Boolean {
        return ASYNC_OPERATIONS
    }

    // Getter for player-cache-size (hardcoded)
    fun getPlayerCacheSize(): Int {
        return PLAYER_CACHE_SIZE
    }

    // Getter for cache-expiration (hardcoded)
    fun getCacheExpiration(): Int {
        return CACHE_EXPIRATION
    }
    
    /**
     * Get a string value from config with default
     */
    fun getString(path: String, default: String = ""): String {
        return plugin.config.getString(path, default) ?: default
    }
    
    /**
     * Get an integer value from config with default
     */
    fun getInt(path: String, default: Int = 0): Int {
        return plugin.config.getInt(path, default)
    }
    
    /**
     * Get a boolean value from config with default
     */
    fun getBoolean(path: String, default: Boolean = false): Boolean {
        return plugin.config.getBoolean(path, default)
    }
    
    /**
     * Get a double value from config with default
     */
    fun getDouble(path: String, default: Double = 0.0): Double {
        return plugin.config.getDouble(path, default)
    }
    
    /**
     * Get a string list from config with default
     */
    fun getStringList(path: String): List<String> {
        return plugin.config.getStringList(path)
    }
}
