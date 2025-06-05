/*
 * Cloudly - Chat Input Manager
 * 
 * Manages chat input for various administrative tasks like application approval/denial.
 * Provides a clean way to capture chat input from players for specific actions.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * ChatInputManager handles chat input capture for admin tasks
 */
object ChatInputManager : Listener {
    
    private lateinit var plugin: CloudlyPlugin
    private val pendingInputs = ConcurrentHashMap<UUID, PendingInput>()
    
    /**
     * Data class for pending input
     */
    data class PendingInput(
        val playerUUID: UUID,
        val inputType: String,
        val relatedId: Int,
        val callback: (String) -> Unit,
        val timeoutAt: Long
    )
    
    /**
     * Initialize the chat input manager
     */
    fun initialize(pluginInstance: CloudlyPlugin) {
        plugin = pluginInstance
        Bukkit.getPluginManager().registerEvents(this, plugin)
        
        // Start cleanup task for expired inputs
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            cleanupExpiredInputs()
        }, 20L * 60L, 20L * 30L) // Check every 30 seconds, starting after 1 minute
    }
    
    /**
     * Wait for chat input from a player
     */
    fun waitForInput(
        player: Player, 
        inputType: String, 
        relatedId: Int, 
        callback: (String) -> Unit
    ) {
        val timeoutAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes timeout
        
        val pendingInput = PendingInput(
            playerUUID = player.uniqueId,
            inputType = inputType,
            relatedId = relatedId,
            callback = callback,
            timeoutAt = timeoutAt
        )
        
        pendingInputs[player.uniqueId] = pendingInput
        
        // Send timeout warning
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (pendingInputs.containsKey(player.uniqueId)) {
                LanguageManager.sendMessage(player, "chat.input.timeout.warning")
            }
        }, 20L * 60L * 4L) // 4 minutes warning
    }
    
    /**
     * Cancel pending input for a player
     */
    fun cancelInput(player: Player) {
        pendingInputs.remove(player.uniqueId)
        LanguageManager.sendMessage(player, "chat.input.cancelled")
    }
    
    /**
     * Check if player has pending input
     */
    fun hasPendingInput(player: Player): Boolean {
        return pendingInputs.containsKey(player.uniqueId)
    }
    
    /**
     * Get pending input type for player
     */
    fun getPendingInputType(player: Player): String? {
        return pendingInputs[player.uniqueId]?.inputType
    }
    
    /**
     * Handle chat events for input capture
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val pendingInput = pendingInputs[player.uniqueId] ?: return
        
        // Cancel the chat event to prevent it from being broadcast
        event.isCancelled = true
        
        val message = event.message
        
        // Handle cancel command
        if (message.equals("cancel", ignoreCase = true) || message.equals("exit", ignoreCase = true)) {
            pendingInputs.remove(player.uniqueId)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                LanguageManager.sendMessage(player, "chat.input.cancelled")
            })
            return
        }
        
        // Validate input based on type
        val validationResult = validateInput(message, pendingInput.inputType)
        if (!validationResult.isValid) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                LanguageManager.sendMessage(player, "chat.input.invalid", validationResult.errorMessage)
                LanguageManager.sendMessage(player, "chat.input.try.again")
            })
            return
        }
        
        // Remove pending input and execute callback
        pendingInputs.remove(player.uniqueId)
        
        try {
            pendingInput.callback(message)
        } catch (e: Exception) {
            plugin.logger.severe("Error executing chat input callback: ${e.message}")
            Bukkit.getScheduler().runTask(plugin, Runnable {
                LanguageManager.sendMessage(player, "chat.input.error")
            })
        }
    }
    
    /**
     * Validate input based on type
     */
    private fun validateInput(input: String, inputType: String): ValidationResult {
        return when (inputType) {
            "APPROVE_APPLICATION" -> {
                // Approval reason can be empty or any text
                if (input.length > 200) {
                    ValidationResult(false, "Reason too long (max 200 characters)")
                } else {
                    ValidationResult(true)
                }
            }
            
            "DENY_APPLICATION" -> {
                // Denial reason is required and must be meaningful
                when {
                    input.isBlank() -> ValidationResult(false, "Denial reason is required")
                    input.length < 3 -> ValidationResult(false, "Reason too short (minimum 3 characters)")
                    input.length > 200 -> ValidationResult(false, "Reason too long (max 200 characters)")
                    else -> ValidationResult(true)
                }
            }
            
            else -> ValidationResult(true) // Default: accept any input
        }
    }
    
    /**
     * Clean up expired inputs
     */
    private fun cleanupExpiredInputs() {
        val currentTime = System.currentTimeMillis()
        val expiredInputs = pendingInputs.values.filter { it.timeoutAt < currentTime }
        
        expiredInputs.forEach { expired ->
            pendingInputs.remove(expired.playerUUID)
            
            val player = Bukkit.getPlayer(expired.playerUUID)
            player?.let {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    LanguageManager.sendMessage(it, "chat.input.timeout")
                })
            }
        }
    }
    
    /**
     * Handle player quit events
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        pendingInputs.remove(event.player.uniqueId)
    }
    
    /**
     * Data class for validation results
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )
}
