package de.cloudly.whitelist.attempts

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks connection attempts from players who are not on the whitelist.
 * Stores aggregated attempt information so staff can review and react inside the GUI.
 */
class WhitelistAttemptService(private val maxEntries: Int = 200) {

    private val attempts = ConcurrentHashMap<UUID, AttemptRecord>()

    /** Snapshot of a tracked whitelist attempt exposed to consumers. */
    data class AttemptSnapshot(
        val uuid: UUID,
        val username: String,
        val lastAddress: String?,
        val firstAttempt: Instant,
        val lastAttempt: Instant,
        val attemptCount: Int,
        val lastMessage: String?
    )

    private data class AttemptRecord(
        val uuid: UUID,
        @Volatile var username: String,
        @Volatile var lastAddress: String?,
        @Volatile var firstAttempt: Instant,
        @Volatile var lastAttempt: Instant,
        val attemptCount: AtomicInteger,
        @Volatile var lastMessage: String?
    ) {
        fun toSnapshot(): AttemptSnapshot = AttemptSnapshot(
            uuid = uuid,
            username = username,
            lastAddress = lastAddress,
            firstAttempt = firstAttempt,
            lastAttempt = lastAttempt,
            attemptCount = attemptCount.get(),
            lastMessage = lastMessage
        )
    }

    /**
     * Record a new join attempt for the given player.
     */
    fun recordAttempt(uuid: UUID, username: String, address: String?, message: String?) {
        val now = Instant.now()
        attempts.compute(uuid) { _, existing ->
            if (existing == null) {
                AttemptRecord(
                    uuid = uuid,
                    username = username,
                    lastAddress = address,
                    firstAttempt = now,
                    lastAttempt = now,
                    attemptCount = AtomicInteger(1),
                    lastMessage = message
                )
            } else {
                existing.username = username
                existing.lastAddress = address ?: existing.lastAddress
                existing.lastAttempt = now
                existing.lastMessage = message
                existing.attemptCount.incrementAndGet()
                existing
            }
        }
        pruneIfNecessary()
    }

    /**
     * Retrieve a sorted list of attempt snapshots (newest first).
     */
    fun getAttempts(): List<AttemptSnapshot> {
        return attempts.values
            .map { it.toSnapshot() }
            .sortedByDescending { it.lastAttempt }
    }

    fun getAttempt(uuid: UUID): AttemptSnapshot? = attempts[uuid]?.toSnapshot()

    fun removeAttempt(uuid: UUID) {
        attempts.remove(uuid)
    }

    fun clear() {
        attempts.clear()
    }

    private fun pruneIfNecessary() {
        if (attempts.size <= maxEntries) {
            return
        }

        val overflow = attempts.size - maxEntries
        if (overflow <= 0) {
            return
        }

        attempts.values
            .sortedBy { it.lastAttempt }
            .take(overflow)
            .forEach { attempts.remove(it.uuid) }
    }
}
