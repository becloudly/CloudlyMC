package de.cloudly.radar

/**
 * Release channel types.
 */
enum class ReleaseChannel(val value: String) {
    RELEASE("release"),
    PRE_RELEASE("pre-release");

    companion object {
        fun fromString(value: String): ReleaseChannel {
            return values().find { it.value.equals(value, ignoreCase = true) } ?: RELEASE
        }
    }

    /**
     * Check if this channel should include pre-releases.
     */
    fun includePreReleases(): Boolean = this == PRE_RELEASE
}
