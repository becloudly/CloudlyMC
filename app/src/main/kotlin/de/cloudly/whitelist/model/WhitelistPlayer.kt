package de.cloudly.whitelist.model

import de.cloudly.storage.core.Serializable
import org.json.JSONObject
import java.util.UUID
import java.time.Instant

/**
 * Represents a player in the whitelist system.
 *
 * @property uuid The UUID of the player
 * @property username The username of the player
 * @property addedBy The UUID of the player or admin who added this player to the whitelist
 * @property addedAt The timestamp when the player was added to the whitelist
 * @property reason Optional reason for whitelisting the player
 * @property discordConnection Discord connection information, null if not connected
 */
data class WhitelistPlayer(
    val uuid: UUID,
    val username: String,
    val addedBy: UUID? = null,
    val addedAt: Instant = Instant.now(),
    val reason: String? = null,
    val discordConnection: DiscordConnection? = null
) : Serializable {
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("uuid", uuid.toString())
        json.put("username", username)
        json.put("addedBy", addedBy?.toString())
        json.put("addedAt", addedAt.epochSecond)
        json.put("reason", reason)
        json.put("discordConnection", discordConnection?.serialize())
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "whitelist_player"
    
    companion object {
        /**
         * Deserialize a WhitelistPlayer from a JSON string.
         * @param data The JSON string data
         * @return The deserialized WhitelistPlayer, or null if deserialization failed
         */
        fun deserialize(data: String): WhitelistPlayer? {
            return try {
                val json = JSONObject(data)
                
                val uuid = UUID.fromString(json.getString("uuid"))
                val username = json.getString("username")
                val addedBy = if (json.has("addedBy") && !json.isNull("addedBy")) {
                    UUID.fromString(json.getString("addedBy"))
                } else null
                val addedAt = Instant.ofEpochSecond(json.getLong("addedAt"))
                val reason = if (json.has("reason") && !json.isNull("reason")) {
                    json.getString("reason")
                } else null
                val discordConnection = if (json.has("discordConnection") && !json.isNull("discordConnection")) {
                    DiscordConnection.deserialize(json.getString("discordConnection"))
                } else null
                
                WhitelistPlayer(uuid, username, addedBy, addedAt, reason, discordConnection)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Represents a Discord connection for a whitelisted player.
 *
 * @property discordId The Discord user ID (numeric snowflake)
 * @property discordUsername The Discord username (e.g., "username" or "username#1234")
 * @property verified Whether the Discord connection has been verified
 * @property connectedAt The timestamp when the Discord connection was established
 * @property verifiedAt The timestamp when the Discord connection was verified (null if not verified)
 */
data class DiscordConnection(
    val discordId: String,
    val discordUsername: String,
    val verified: Boolean = false,
    val connectedAt: Instant = Instant.now(),
    val verifiedAt: Instant? = null
) : Serializable {
    
    override fun serialize(): String {
        val json = JSONObject()
        json.put("discordId", discordId)
        json.put("discordUsername", discordUsername)
        json.put("verified", verified)
        json.put("connectedAt", connectedAt.epochSecond)
        json.put("verifiedAt", verifiedAt?.epochSecond)
        json.put("typeId", getTypeId())
        return json.toString()
    }
    
    override fun getTypeId(): String = "discord_connection"
    
    companion object {
        /**
         * Deserialize a DiscordConnection from a JSON string.
         * @param data The JSON string data
         * @return The deserialized DiscordConnection, or null if deserialization failed
         */
        fun deserialize(data: String): DiscordConnection? {
            return try {
                val json = JSONObject(data)
                
                val discordId = json.getString("discordId")
                val discordUsername = json.getString("discordUsername")
                val verified = json.getBoolean("verified")
                val connectedAt = Instant.ofEpochSecond(json.getLong("connectedAt"))
                val verifiedAt = if (json.has("verifiedAt") && !json.isNull("verifiedAt")) {
                    Instant.ofEpochSecond(json.getLong("verifiedAt"))
                } else null
                
                DiscordConnection(discordId, discordUsername, verified, connectedAt, verifiedAt)
            } catch (e: Exception) {
                null
            }
        }
    }
}