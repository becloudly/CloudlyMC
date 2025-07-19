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
    
    /**
     * Initialize the scheduler utility by detecting server type.
     */
    private fun initialize() {
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
            
            isFolia = true
            // Log successful Folia detection (optional, for debugging)
        } catch (e: ClassNotFoundException) {
            // Folia classes not found, we're on Paper/Spigot
            isFolia = false
        } catch (e: NoSuchMethodException) {
            // Method not found, assume Paper/Spigot
            isFolia = false
        } catch (e: Exception) {
            // Any other error, assume Paper/Spigot
            isFolia = false
        }
    }
    
    /**
     * Check if the server is running Folia.
     */
    fun isFolia(): Boolean {
        initialize()
        return isFolia ?: false
    }
    
    /**
     * Run a task on the main thread (global region in Folia).
     */
    fun runTask(plugin: JavaPlugin, task: Runnable): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's global region scheduler
                val runMethod = foliaGlobalRegionScheduler?.javaClass?.getMethod("run", JavaPlugin::class.java, Runnable::class.java)
                runMethod?.invoke(foliaGlobalRegionScheduler, plugin, task)
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                // Fallback to Bukkit scheduler - explicitly cast to Runnable
                org.bukkit.Bukkit.getScheduler().runTask(plugin, task as Runnable)
            }
        } else {
            // Use traditional Bukkit scheduler - explicitly cast to Runnable
            org.bukkit.Bukkit.getScheduler().runTask(plugin, task as Runnable)
        }
    }
    
    /**
     * Run a task asynchronously.
     */
    fun runTaskAsynchronously(plugin: JavaPlugin, task: Runnable): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler
                val runNowMethod = foliaAsyncScheduler?.javaClass?.getMethod("runNow", JavaPlugin::class.java, Runnable::class.java)
                runNowMethod?.invoke(foliaAsyncScheduler, plugin, task)
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                // Fallback to Bukkit scheduler - explicitly cast to Runnable
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, task as Runnable)
            }
        } else {
            // Use traditional Bukkit scheduler - explicitly cast to Runnable
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, task as Runnable)
        }
    }
    
    /**
     * Run a repeating task asynchronously.
     */
    fun runTaskTimerAsynchronously(plugin: JavaPlugin, task: Runnable, delay: Long, period: Long): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler with repeating
                val runAtFixedRateMethod = foliaAsyncScheduler?.javaClass?.getMethod(
                    "runAtFixedRate", 
                    JavaPlugin::class.java, 
                    Runnable::class.java, 
                    Long::class.javaPrimitiveType, 
                    Long::class.javaPrimitiveType, 
                    TimeUnit::class.java
                )
                
                // Convert ticks to milliseconds (20 ticks = 1 second = 1000ms)
                val delayMs = delay * 50L
                val periodMs = period * 50L
                
                runAtFixedRateMethod?.invoke(
                    foliaAsyncScheduler, 
                    plugin, 
                    task, 
                    delayMs, 
                    periodMs, 
                    TimeUnit.MILLISECONDS
                )
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                // Fallback to Bukkit scheduler - explicitly cast to Runnable
                org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task as Runnable, delay, period)
            }
        } else {
            // Use traditional Bukkit scheduler - explicitly cast to Runnable
            org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task as Runnable, delay, period)
        }
    }
    
    /**
     * Schedule a delayed task asynchronously.
     */
    fun runTaskLaterAsynchronously(plugin: JavaPlugin, task: Runnable, delay: Long): BukkitTask? {
        initialize()
        
        return if (isFolia == true) {
            try {
                // Use Folia's async scheduler with delay
                val runDelayedMethod = foliaAsyncScheduler?.javaClass?.getMethod(
                    "runDelayed", 
                    JavaPlugin::class.java, 
                    Runnable::class.java, 
                    Long::class.javaPrimitiveType, 
                    TimeUnit::class.java
                )
                
                // Convert ticks to milliseconds (20 ticks = 1 second = 1000ms)
                val delayMs = delay * 50L
                
                runDelayedMethod?.invoke(
                    foliaAsyncScheduler, 
                    plugin, 
                    task, 
                    delayMs, 
                    TimeUnit.MILLISECONDS
                )
                null // Folia doesn't return BukkitTask
            } catch (e: Exception) {
                // Fallback to Bukkit scheduler - explicitly cast to Runnable
                org.bukkit.Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task as Runnable, delay)
            }
        } else {
            // Use traditional Bukkit scheduler - explicitly cast to Runnable
            org.bukkit.Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task as Runnable, delay)
        }
    }
}
