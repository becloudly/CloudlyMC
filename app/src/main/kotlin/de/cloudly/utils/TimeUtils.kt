package de.cloudly.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for time and date formatting with proper timezone handling.
 */
object TimeUtils {
    
    /**
     * Default date-time formatter using the system's default timezone.
     */
    private val defaultFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    
    /**
     * Format an Instant to a string using the system's default timezone.
     * This ensures timestamps are displayed in the local time instead of UTC.
     * 
     * @param instant The instant to format
     * @return Formatted date-time string in local timezone
     */
    fun formatTimestamp(instant: Instant): String {
        return defaultFormatter.format(instant)
    }
    
    /**
     * Format an Instant to a string using a custom pattern and the system's default timezone.
     * 
     * @param instant The instant to format
     * @param pattern The date-time pattern (e.g., "yyyy-MM-dd HH:mm:ss")
     * @return Formatted date-time string in local timezone
     */
    fun formatTimestamp(instant: Instant, pattern: String): String {
        val formatter = DateTimeFormatter
            .ofPattern(pattern)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
    
    /**
     * Format an Instant to a string using a custom timezone.
     * 
     * @param instant The instant to format
     * @param zoneId The timezone to use
     * @return Formatted date-time string in the specified timezone
     */
    fun formatTimestamp(instant: Instant, zoneId: ZoneId): String {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.getDefault())
            .withZone(zoneId)
        return formatter.format(instant)
    }
}
