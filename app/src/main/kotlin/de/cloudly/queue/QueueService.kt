package de.cloudly.queue

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.queue.model.QueuedPlayer
import de.cloudly.queue.model.QueuePriority
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.scheduler.BukkitTask
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.logging.Level

/**
 * Service for managing the connection queue system.
 * Implements a smart priority queue where:
 * - Operators bypass the queue entirely
 * - Whitelisted players have higher priority
 * - First-time joiners have elevated priority over regular players
 *
 * Players are processed every configured interval (default 3 seconds).
 */
class QueueService(private val plugin: CloudlyPaper) {

    private val queue = PriorityBlockingQueue<QueuedPlayer>()
    private val queuedPlayers = ConcurrentHashMap<UUID, QueuedPlayer>()
    private val processing = ConcurrentHashMap<UUID, QueuedPlayer>()
    private val readyPlayers = ConcurrentHashMap<UUID, Long>()

    private var queueTask: BukkitTask? = null

    private var enabled = false
    private var joinDelay = 3L
    private var readyTimeoutSeconds = 15L

    /**
     * Initialize the queue service and start processing.
     */
    fun initialize() {
        enabled = plugin.getConfigManager().getBoolean("queue.enabled", true)
        joinDelay = plugin.getConfigManager().getInt("queue.join_delay_seconds", 3).toLong()
        readyTimeoutSeconds = plugin.getConfigManager().getInt("queue.ready_timeout_seconds", 15).toLong()

        if (!enabled) {
            plugin.logger.info("Connection queue is disabled in config")
            return
        }

        startQueueProcessor()

        plugin.logger.info(
            "Connection queue service initialized (delay: ${joinDelay}s, ready window: ${readyTimeoutSeconds}s)"
        )
    }

    /**
     * Start the queue processor task.
     */
    private fun startQueueProcessor() {
        queueTask?.cancel()
        queueTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { processQueue() }, 20L, 20L)
    }

    /**
     * Add a player to the queue.
     * @param event The login event for this player
     * @return true if player was added to queue, false if they should be allowed immediately
     */
    fun addToQueue(event: PlayerLoginEvent): Boolean {
        if (!enabled) {
            return false
        }

        val player = event.player
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()

        cleanupExpiredReadyPlayers(now)

        readyPlayers[uuid]?.let { expiresAt ->
            if (now <= expiresAt) {
                readyPlayers.remove(uuid)
                plugin.logger.info("Player ${player.name} bypassing queue (ready slot)")
                return false
            }
            readyPlayers.remove(uuid)
        }

        processing[uuid]?.let { queued ->
            val total = totalQueueSize().coerceAtLeast(1)
            sendQueueMessage(event, queued.priority, 1, total)
            return true
        }

        queuedPlayers[uuid]?.let { queued ->
            val position = getQueuePosition(uuid)
            val total = totalQueueSize().coerceAtLeast(position)
            sendQueueMessage(event, queued.priority, position, total)
            return true
        }

        val priority = determinePriority(event)
        if (priority == QueuePriority.OPERATOR) {
            plugin.logger.info("Player ${player.name} bypassing queue (operator)")
            return false
        }

        if (shouldAllowImmediately()) {
            plugin.logger.info("Player ${player.name} joined immediately (queue idle)")
            return false
        }

        val queuedPlayer = QueuedPlayer(
            uuid = uuid,
            name = player.name,
            address = event.address?.hostAddress ?: "unknown",
            priority = priority,
            queuedAt = Instant.now(),
            isFirstJoin = !player.hasPlayedBefore(),
            isWhitelisted = plugin.getWhitelistService().isWhitelisted(uuid),
            isOperator = player.isOp
        )

        queue.offer(queuedPlayer)
        queuedPlayers[uuid] = queuedPlayer

        val position = getQueuePosition(uuid)
        val total = kotlin.math.max(totalQueueSize(), position)
        sendQueueMessage(event, priority, position, total)

        plugin.logger.info("Player ${player.name} added to queue at position $position (priority: ${priority.name})")
        return true
    }

    /**
     * Determine the priority for a player.
     */
    private fun determinePriority(event: PlayerLoginEvent): QueuePriority {
        val player = event.player

        return when {
            player.isOp -> QueuePriority.OPERATOR
            plugin.getWhitelistService().isWhitelisted(player.uniqueId) -> QueuePriority.WHITELISTED
            !player.hasPlayedBefore() -> QueuePriority.FIRST_JOIN
            else -> QueuePriority.REGULAR
        }
    }

    /**
     * Decide if the player should join immediately instead of being queued.
     */
    private fun shouldAllowImmediately(): Boolean {
        if (queue.isNotEmpty() || processing.isNotEmpty()) {
            return false
        }

        val server = plugin.server
        return server.onlinePlayers.size < server.maxPlayers
    }

    /**
     * Get the position of a player in the queue (includes players currently being processed).
     */
    private fun getQueuePosition(uuid: UUID): Int {
        val order = mutableListOf<UUID>().apply {
            addAll(processing.keys)
            queue.sorted().mapTo(this) { it.uuid }
        }
        val index = order.indexOf(uuid)
        return if (index >= 0) index + 1 else 1
    }

    /**
     * Send the appropriate queue message for a player.
     */
    private fun sendQueueMessage(event: PlayerLoginEvent, priority: QueuePriority, position: Int, total: Int) {
        val safePosition = position.coerceAtLeast(1)
        val safeTotal = total.coerceAtLeast(safePosition)
        val message = when (priority) {
            QueuePriority.OPERATOR -> Messages.Queue.OPERATOR_BYPASS
            QueuePriority.WHITELISTED -> Messages.Queue.positionWhitelisted(safePosition, safeTotal)
            QueuePriority.FIRST_JOIN -> Messages.Queue.positionFirstJoin(safePosition, safeTotal)
            QueuePriority.REGULAR -> Messages.Queue.positionRegular(safePosition, safeTotal)
        }
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, message)
    }

    /**
     * Process the queue and prepare the next player to join.
     */
    private fun processQueue() {
        if (!enabled) {
            return
        }

        cleanupExpiredReadyPlayers(System.currentTimeMillis())

        if (queue.isEmpty() || processing.isNotEmpty()) {
            return
        }

        val queuedPlayer = queue.poll() ?: return
        queuedPlayers.remove(queuedPlayer.uuid)
        processing[queuedPlayer.uuid] = queuedPlayer

        try {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                try {
                    if (!enabled) {
                        readyPlayers.remove(queuedPlayer.uuid)
                        return@Runnable
                    }

                    val expiresAt = System.currentTimeMillis() + readyTimeoutSeconds * 1000
                    readyPlayers[queuedPlayer.uuid] = expiresAt
                    plugin.logger.info(
                        "Allowing player ${queuedPlayer.name} to join (priority: ${queuedPlayer.priority.name}); ready for ${readyTimeoutSeconds}s"
                    )
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Error finalizing queue entry for ${queuedPlayer.name}", e)
                } finally {
                    processing.remove(queuedPlayer.uuid)
                }
            }, joinDelay * 20L)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error scheduling queue processing for ${queuedPlayer.name}", e)
            processing.remove(queuedPlayer.uuid)
        }
    }

    /**
     * Remove expired ready-to-join flags.
     */
    private fun cleanupExpiredReadyPlayers(currentTime: Long) {
        readyPlayers.entries.removeIf { (_, expiresAt) -> expiresAt <= currentTime }
    }

    /**
     * Remove a player from the queue entirely.
     */
    fun removeFromQueue(uuid: UUID) {
        queue.removeIf { it.uuid == uuid }
        queuedPlayers.remove(uuid)
        processing.remove(uuid)
        readyPlayers.remove(uuid)
    }

    /**
     * Check if a player is waiting in the queue (including processing state).
     */
    fun isInQueue(uuid: UUID): Boolean {
        return queuedPlayers.containsKey(uuid) || processing.containsKey(uuid)
    }

    /**
     * Get the current queue size, including players being processed.
     */
    fun getQueueSize(): Int {
        return totalQueueSize()
    }

    private fun totalQueueSize(): Int {
        return queue.size + processing.size
    }

    /**
     * Check if the queue is enabled.
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * Reload the queue configuration.
     */
    fun reload() {
        enabled = plugin.getConfigManager().getBoolean("queue.enabled", true)
        joinDelay = plugin.getConfigManager().getInt("queue.join_delay_seconds", 3).toLong()
        readyTimeoutSeconds = plugin.getConfigManager().getInt("queue.ready_timeout_seconds", 15).toLong()

        if (!enabled) {
            queueTask?.cancel()
            queueTask = null
            queue.clear()
            queuedPlayers.clear()
            processing.clear()
            readyPlayers.clear()
        } else if (queueTask == null) {
            startQueueProcessor()
        }
    }

    /**
     * Shutdown the queue service.
     */
    fun shutdown() {
        queueTask?.cancel()
        queueTask = null
        queue.clear()
        queuedPlayers.clear()
        processing.clear()
        readyPlayers.clear()
    }
}
