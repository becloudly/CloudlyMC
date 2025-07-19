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
     * First compares versions semantically, then falls back to publication date.
     */
    fun isNewerThan(other: GitHubRelease?): Boolean {
        if (other == null) return true
        
        // Compare versions semantically first
        val thisVersion = normalizeVersion(this.tagName)
        val otherVersion = normalizeVersion(other.tagName)
        
        val versionComparison = compareVersions(thisVersion, otherVersion)
        
        // If versions are the same, compare by publication date
        return if (versionComparison == 0) {
            this.publishedAt.isAfter(other.publishedAt)
        } else {
            versionComparison > 0
        }
    }
    
    /**
     * Check if this release matches the current plugin version.
     */
    fun matchesVersion(pluginVersion: String): Boolean {
        val releaseVersion = normalizeVersion(this.tagName)
        val currentVersion = normalizeVersion(pluginVersion)
        return releaseVersion == currentVersion
    }
    
    /**
     * Normalize a version string by removing 'v' prefix and handling common formats.
     */
    private fun normalizeVersion(version: String): String {
        return version.removePrefix("v").lowercase()
    }
    
    /**
     * Compare two semantic version strings.
     * Returns: > 0 if version1 > version2, < 0 if version1 < version2, 0 if equal
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = parseVersionParts(version1)
        val parts2 = parseVersionParts(version2)
        
        // Compare major, minor, patch
        for (i in 0 until minOf(parts1.size, parts2.size, 3)) {
            val compare = parts1[i].compareTo(parts2[i])
            if (compare != 0) return compare
        }
        
        // Handle pre-release identifiers (alpha, beta, rc)
        val preRelease1 = extractPreReleaseInfo(version1)
        val preRelease2 = extractPreReleaseInfo(version2)
        
        return when {
            preRelease1 == null && preRelease2 == null -> 0
            preRelease1 == null -> 1 // Release > pre-release
            preRelease2 == null -> -1 // Pre-release < release
            else -> comparePreReleases(preRelease1, preRelease2)
        }
    }
    
    /**
     * Parse version string into numeric parts.
     */
    private fun parseVersionParts(version: String): List<Int> {
        val mainVersion = version.split("-")[0] // Remove pre-release suffix
        return mainVersion.split(".").mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * Extract pre-release information (type and number).
     */
    private fun extractPreReleaseInfo(version: String): Pair<String, Int>? {
        val preReleaseRegex = Regex("-(alpha|beta|rc)_?(\\d+)?", RegexOption.IGNORE_CASE)
        val match = preReleaseRegex.find(version) ?: return null
        
        val type = match.groupValues[1].lowercase()
        val number = match.groupValues[2].toIntOrNull() ?: 1
        
        return Pair(type, number)
    }
    
    /**
     * Compare pre-release versions.
     */
    private fun comparePreReleases(preRelease1: Pair<String, Int>, preRelease2: Pair<String, Int>): Int {
        val typeOrder = mapOf("alpha" to 1, "beta" to 2, "rc" to 3)
        
        val type1Order = typeOrder[preRelease1.first] ?: 0
        val type2Order = typeOrder[preRelease2.first] ?: 0
        
        return when {
            type1Order != type2Order -> type1Order.compareTo(type2Order)
            else -> preRelease1.second.compareTo(preRelease2.second)
        }
    }
}
