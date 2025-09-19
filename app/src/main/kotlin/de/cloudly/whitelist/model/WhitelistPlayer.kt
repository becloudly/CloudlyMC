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
 */
data class WhitelistPlayer(
    val uuid: UUID,
    val username: String,
    val addedBy: UUID? = null,
    val addedAt: Instant = Instant.now(),
    val reason: String? = null
)