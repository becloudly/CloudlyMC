package de.cloudly.queue.model

import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID

/**
 * Represents a player in the connection queue.
 * Contains information about their priority, join time, and connection details.
 */
data class QueuedPlayer(
    /**
     * The UUID of the player.
     */
    val uuid: UUID,
    
    /**
     * The player's name.
     */
    val name: String,
    
    /**
     * The IP address the player is connecting from.
     */
    val address: String,
    
    /**
     * The priority level of this player in the queue.
     */
    val priority: QueuePriority,
    
    /**
     * When this player joined the queue.
     */
    val queuedAt: Instant = Instant.now(),
    
    /**
     * Whether this is the player's first time joining the server.
     */
    val isFirstJoin: Boolean = false,
    
    /**
     * Whether the player is whitelisted.
     */
    val isWhitelisted: Boolean = false,
    
    /**
     * Whether the player is an operator.
     */
    val isOperator: Boolean = false
) : Comparable<QueuedPlayer> {
    
    /**
     * Compare players based on their priority first, then by queue time.
     * Players with higher priority or earlier queue time go first.
     */
    override fun compareTo(other: QueuedPlayer): Int {
        // First compare by priority (higher priority goes first)
        val priorityCompare = QueuePriority.compare(this.priority, other.priority)
        if (priorityCompare != 0) return priorityCompare
        
        // If same priority, compare by queue time (earlier time goes first)
        return this.queuedAt.compareTo(other.queuedAt)
    }
    
    /**
     * Get the position message key for language files based on priority.
     */
    fun getPositionMessageKey(): String {
        return when (priority) {
            QueuePriority.OPERATOR -> "queue.operator_bypass"
            QueuePriority.WHITELISTED -> "queue.position_whitelisted"
            QueuePriority.FIRST_JOIN -> "queue.position_first_join"
            QueuePriority.REGULAR -> "queue.position_regular"
        }
    }
}
