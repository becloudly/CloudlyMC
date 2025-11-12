package de.cloudly.moderation

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.moderation.model.BanEntry
import de.cloudly.storage.config.StorageConfig
import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.factory.StorageFactory
import de.cloudly.utils.AuditLogger
import de.cloudly.whitelist.WhitelistService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Moderation service that manages player bans (temporary and permanent).
 * Integrates with the storage system and prevents banned players from joining.
 */
class BanService(
    private val plugin: CloudlyPaper,
    private val whitelistService: WhitelistService
) : Listener {

    private val storageConfig = StorageConfig(plugin)
    private val storageFactory = StorageFactory(plugin)
    private var repository: DataRepository<BanEntry>? = null
    private var auditLogger: AuditLogger? = null
    private var initialized = false

    fun initialize() {
        storageConfig.load()
        auditLogger = AuditLogger(plugin)

        val deserializer: (String, String) -> BanEntry? = { data, _ ->
            BanEntry.deserialize(data)
        }

        repository = storageFactory.createRepository("bans", storageConfig, deserializer)

        if (repository?.initialize() == true) {
            cleanupExpiredBans()
            plugin.server.pluginManager.registerEvents(this, plugin)
            initialized = true
            plugin.logger.info("Ban service aktiviert; ${repository?.count() ?: 0} Einträge geladen")
        } else {
            plugin.logger.severe("Ban service konnte nicht initialisiert werden")
        }
    }

    fun shutdown() {
        HandlerList.unregisterAll(this)
        repository?.close()
        repository = null
        auditLogger?.close()
        auditLogger = null
        initialized = false
    }

    fun hasActiveBan(uuid: UUID): Boolean {
        return getActiveBan(uuid) != null
    }

    fun getActiveBan(uuid: UUID): BanEntry? {
        val repo = repository ?: return null
        val entry = repo.retrieve(uuid.toString()) ?: return null
        if (entry.isExpired()) {
            repo.remove(uuid.toString())
            return null
        }
        return entry
    }

    fun banPlayer(
        uuid: UUID,
        username: String,
        actor: UUID?,
        duration: Duration?,
        reason: String?,
        deleteFromWhitelist: Boolean
    ): BanResult {
        val repo = repository ?: return BanResult.StorageError

        val existing = getActiveBan(uuid)
        if (existing != null) {
            return BanResult.AlreadyBanned(existing)
        }

        val expiresAt = duration?.let { Instant.now().plus(it) }
        val entry = BanEntry(
            uuid = uuid,
            username = username,
            createdBy = actor,
            createdAt = Instant.now(),
            expiresAt = expiresAt,
            reason = reason
        )

        val stored = repo.store(uuid.toString(), entry)
        if (!stored) {
            return BanResult.StorageError
        }

        logAudit("BAN_ADD", uuid, actor, buildDetails(username, duration, reason))

        if (deleteFromWhitelist) {
            whitelistService.removePlayer(uuid, actor)
        }

        return BanResult.Success(entry)
    }

    fun unbanPlayer(uuid: UUID, actor: UUID?): Boolean {
        val repo = repository ?: return false
        val removed = repo.remove(uuid.toString())
        if (removed) {
            logAudit("BAN_REMOVE", uuid, actor, null)
        }
        return removed
    }

    fun cleanupExpiredBans() {
        val repo = repository ?: return
        val now = Instant.now()
        repo.getAll().forEach { (key, entry) ->
            if (entry.isExpired(now)) {
                repo.remove(key)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (!initialized) {
            return
        }

        val entry = getActiveBan(event.player.uniqueId) ?: return

        val msg = if (entry.isPermanent) {
            Messages.Moderation.Login.permanent(entry.reason)
        } else {
            val remaining = entry.remainingDuration()?.takeIf { !it.isZero && !it.isNegative }
            val remainingText = remaining?.let { formatDuration(it) } ?: "0 Minuten"
            Messages.Moderation.Login.temporary(remainingText, entry.reason)
        }

        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, msg)
    }

    private fun logAudit(action: String, target: UUID, actor: UUID?, details: String?) {
        plugin.logger.info("[BAN] $action - Target: $target - Actor: $actor - Details: $details")
        auditLogger?.log(action, target, actor, details ?: "")
    }

    private fun buildDetails(username: String, duration: Duration?, reason: String?): String {
        val parts = mutableListOf<String>()
        parts.add("Username: $username")
        if (duration != null) {
            parts.add("Duration: ${formatDuration(duration)}")
        } else {
            parts.add("Duration: permanent")
        }
        if (!reason.isNullOrBlank()) {
            parts.add("Reason: $reason")
        }
        return parts.joinToString(", ")
    }

    fun describeDuration(duration: Duration): String {
        return formatDuration(duration)
    }

    private fun formatDuration(duration: Duration): String {
        var remainingSeconds = duration.seconds.coerceAtLeast(0)

        val days = remainingSeconds / 86_400
        remainingSeconds -= days * 86_400
        val hours = remainingSeconds / 3_600
        remainingSeconds -= hours * 3_600
        val minutes = remainingSeconds / 60

        val parts = mutableListOf<String>()
        if (days > 0) {
            parts.add("$days ${if (days == 1L) "Tag" else "Tage"}")
        }
        if (hours > 0) {
            parts.add("$hours ${if (hours == 1L) "Stunde" else "Stunden"}")
        }
        if (minutes > 0 || parts.isEmpty()) {
            parts.add("$minutes ${if (minutes == 1L) "Minute" else "Minuten"}")
        }
        return parts.joinToString(" ")
    }

    sealed class BanResult {
        data class Success(val entry: BanEntry) : BanResult()
        data class AlreadyBanned(val entry: BanEntry) : BanResult()
        object StorageError : BanResult()
    }
}
