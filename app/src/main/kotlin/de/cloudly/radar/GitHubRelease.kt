package de.cloudly.radar

import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Represents a GitHub release with relevant information for the release radar.
 */
data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: Instant,
    val prerelease: Boolean,
    val draft: Boolean
) {
    companion object {
        /**
         * Parse a GitHub release from JSON response.
         */
        fun fromJson(json: JSONObject): GitHubRelease {
            return GitHubRelease(
                id = json.getLong("id"),
                tagName = json.getString("tag_name"),
                name = json.optString("name", json.getString("tag_name")),
                body = json.optString("body", ""),
                htmlUrl = json.getString("html_url"),
                publishedAt = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(json.getString("published_at"))),
                prerelease = json.getBoolean("prerelease"),
                draft = json.getBoolean("draft")
            )
        }
    }

    /**
     * Check if this release is newer than another release.
     */
    fun isNewerThan(other: GitHubRelease?): Boolean {
        return other == null || this.publishedAt.isAfter(other.publishedAt)
    }
}
