package de.cloudly.whitelist.model

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
)

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
)