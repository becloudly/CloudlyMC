package de.cloudly.moderation.model

import de.cloudly.storage.core.Serializable
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Represents a ban entry within the moderation system.
 * Supports both temporary and permanent bans with optional reasons.
 */
data class BanEntry(
    val uuid: UUID,
    val username: String,
    val createdBy: UUID?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val reason: String?
) : Serializable {

    val isPermanent: Boolean get() = expiresAt == null

    /**
     * Determine whether this ban has expired relative to the provided timestamp.
     */
    fun isExpired(reference: Instant = Instant.now()): Boolean {
        val expiry = expiresAt ?: return false
        return expiry.isBefore(reference)
    }

    /**
     * Calculate the remaining duration until the ban expires, if any.
     */
    fun remainingDuration(reference: Instant = Instant.now()): Duration? {
        val expiry = expiresAt ?: return null
        if (expiry.isBefore(reference)) {
            return Duration.ZERO
        }
        return Duration.between(reference, expiry)
    }

    override fun serialize(): String {
        val json = JSONObject()
        json.put("uuid", uuid.toString())
        json.put("username", username)
        json.put("createdBy", createdBy?.toString())
        json.put("createdAt", createdAt.epochSecond)
        json.put("expiresAt", expiresAt?.epochSecond)
        json.put("reason", reason)
        json.put("typeId", getTypeId())
        return json.toString()
    }

    override fun getTypeId(): String = "ban_entry"

    companion object {
        fun deserialize(data: String): BanEntry? {
            return try {
                val json = JSONObject(data)
                val uuid = UUID.fromString(json.getString("uuid"))
                val username = json.getString("username")
                val createdBy = if (json.isNull("createdBy")) null else UUID.fromString(json.getString("createdBy"))
                val createdAt = Instant.ofEpochSecond(json.getLong("createdAt"))
                val expiresAt = if (json.isNull("expiresAt")) null else Instant.ofEpochSecond(json.getLong("expiresAt"))
                val reason = if (json.isNull("reason")) null else json.getString("reason")
                BanEntry(uuid, username, createdBy, createdAt, expiresAt, reason)
            } catch (ex: Exception) {
                null
            }
        }
    }
}
