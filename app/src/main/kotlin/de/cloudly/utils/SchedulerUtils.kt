package de.cloudly.utils

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * Utility class for cross-platform scheduling that works on both Paper and Folia.
 * Automatically detects the server type and uses the appropriate scheduling method.
 */
object SchedulerUtils {
    
    private var isFolia: Boolean? = null
    private var foliaGlobalRegionScheduler: Any? = null
    private var foliaAsyncScheduler: Any? = null
    private var foliaGlobalRegionSchedulerMethod: Method? = null
    private var foliaAsyncSchedulerMethod: Method? = null
    
    // Cached reflection methods for Folia scheduler operations
    private var foliaRunMethod: Method? = null
    private var foliaRunNowMethod: Method? = null
    private var foliaRunAtFixedRateMethod: Method? = null
    private var foliaRunDelayedMethod: Method? = null
    
    // 1 Minecraft tick = 50ms (20 ticks per second)
    const val TICKS_TO_MILLISECONDS = 50L
    
    /**
     * Initialize the scheduler utility by detecting server type.
     */
    private fun initialize(debug: Boolean = false) {
        if (isFolia != null) return
        
        try {
            // Try to load Folia's GlobalRegionScheduler class
            val foliaClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
            val asyncClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler")
            val serverClass = Class.forName("org.bukkit.Bukkit")
            
            // Get Folia scheduler instances
            foliaGlobalRegionSchedulerMethod = serverClass.getMethod("getGlobalRegionScheduler")
            foliaAsyncSchedulerMethod = serverClass.getMethod("getAsyncScheduler")
            
            foliaGlobalRegionScheduler = foliaGlobalRegionSchedulerMethod?.invoke(null)
            foliaAsyncScheduler = foliaAsyncSchedulerMethod?.invoke(null)
            
            // Cache reflection methods for Folia scheduler operations
            foliaRunMethod = foliaGlobalRegionScheduler?.javaClass?.getMethod(
                "run",
                org.bukkit.plugin.Plugin::class.java,
                java.util.function.Consumer::class.java
            )
            foliaRunNowMethod = foliaAsyncScheduler?.javaClass?.getMethod(
                "runNow",
                org.bukkit.plugin.Plugin::class.java,
                java.util.function.Consumer::class.java
            )
            foliaRunAtFixedRateMethod = foliaAsyncScheduler?.javaClass?.getMethod(
                "runAtFixedRate",
                org.bukkit.plugin.Plugin::class.java,
                java.util.function.Consumer::class.java,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                TimeUnit::class.java
            )
            foliaRunDelayedMethod = foliaAsyncScheduler?.javaClass?.getMethod(
                "runDelayed",
                org.bukkit.plugin.Plugin::class.java,
                java.util.function.Consumer::class.java,
                Long::class.javaPrimitiveType,
                TimeUnit::class.java
            )
            
            isFolia = true
            // Log successful Folia detection only if debug is enabled
            if (debug) {
                println("[Cloudly] Detected Folia/Canvas server - using Folia schedulers")
            }
        } catch (e: ClassNotFoundException) {
            // Folia classes not found, we're on Paper/Spigot
            isFolia = false
            if (debug) {
                println("[Cloudly] Detected Paper/Spigot server - using Bukkit schedulers")
            }
        } catch (e: NoSuchMethodException) {
            // Method not found, assume Paper/Spigot
            isFolia = false
            if (debug) {
                println("[Cloudly] Method not found, assuming Paper/Spigot server")
            }
        } catch (e: Exception) {
            // Any other error, assume Paper/Spigot
            isFolia = false
            if (debug) {
                println("[Cloudly] Error detecting server type: ${e.message}, assuming Paper/Spigot")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Check if the server is running Folia.
     */
    fun isFolia(debug: Boolean = false): Boolean {
        initialize(debug)
        return isFolia ?: false
    }
    
    /**
     * Run a task on the main thread (global region in Folia).
     */
    fun runTask(plugin: JavaPlugin, task: Runnable): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's global region scheduler - convert Runnable to Consumer<ScheduledTask>
                // Convert Runnable to Consumer<ScheduledTask>
                val taskConsumer = java.util.function.Consumer<Any> { _ -> task.run() }
                
                foliaRunMethod?.invoke(foliaGlobalRegionScheduler, plugin, taskConsumer)
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                plugin.logger.severe("Failed to use Folia global region scheduler: ${e.message}")
                e.printStackTrace()
                // Do NOT fallback to Bukkit scheduler on Folia - it's intentionally disabled
                null
            }
        } else {
            // Use traditional Bukkit scheduler
            try {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, task)
            } catch (e: Exception) {
                plugin.logger.severe("Bukkit scheduler failed: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Run a task asynchronously.
     */
    fun runTaskAsynchronously(plugin: JavaPlugin, task: Runnable): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler - convert Runnable to Consumer<ScheduledTask>
                // Convert Runnable to Consumer<ScheduledTask>
                val taskConsumer = java.util.function.Consumer<Any> { _ -> task.run() }
                
                foliaRunNowMethod?.invoke(foliaAsyncScheduler, plugin, taskConsumer)
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                plugin.logger.severe("Failed to use Folia async scheduler: ${e.message}")
                e.printStackTrace()
                // Do NOT fallback to Bukkit scheduler on Folia - it's not supported
                null
            }
        } else {
            // Use traditional Bukkit scheduler
            try {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
            } catch (e: Exception) {
                plugin.logger.severe("Bukkit async scheduler failed: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Run a repeating task asynchronously.
     */
    fun runTaskTimerAsynchronously(plugin: JavaPlugin, task: Runnable, delay: Long, period: Long): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler with repeating - convert Runnable to Consumer<ScheduledTask>
                // Convert Runnable to Consumer<ScheduledTask>
                val taskConsumer = java.util.function.Consumer<Any> { _ -> task.run() }
                
                // Convert ticks to milliseconds (20 ticks = 1 second = 1000ms)
                val delayMs = delay * TICKS_TO_MILLISECONDS
                val periodMs = period * TICKS_TO_MILLISECONDS
                
                foliaRunAtFixedRateMethod?.invoke(
                    foliaAsyncScheduler, 
                    plugin, 
                    taskConsumer, 
                    delayMs, 
                    periodMs, 
                    TimeUnit.MILLISECONDS
                )
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                plugin.logger.severe("Failed to use Folia async timer: ${e.message}")
                e.printStackTrace()
                // Do NOT fallback to Bukkit scheduler on Folia - it's not supported
                null
            }
        } else {
            // Use traditional Bukkit scheduler
            try {
                org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
            } catch (e: Exception) {
                plugin.logger.severe("Bukkit async timer failed: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Schedule a delayed task asynchronously.
     */
    fun runTaskLaterAsynchronously(plugin: JavaPlugin, task: Runnable, delay: Long): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler with delay - convert Runnable to Consumer<ScheduledTask>
                // Convert Runnable to Consumer<ScheduledTask>
                val taskConsumer = java.util.function.Consumer<Any> { _ -> task.run() }
                
                // Convert ticks to milliseconds (20 ticks = 1 second = 1000ms)
                val delayMs = delay * TICKS_TO_MILLISECONDS
                
                foliaRunDelayedMethod?.invoke(
                    foliaAsyncScheduler, 
                    plugin, 
                    taskConsumer, 
                    delayMs, 
                    TimeUnit.MILLISECONDS
                )
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                plugin.logger.severe("Failed to use Folia async delayed task: ${e.message}")
                e.printStackTrace()
                // Do NOT fallback to Bukkit scheduler on Folia - it's not supported
                null
            }
        } else {
            // Use traditional Bukkit scheduler
            try {
                org.bukkit.Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
            } catch (e: Exception) {
                plugin.logger.severe("Bukkit async delayed task failed: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
