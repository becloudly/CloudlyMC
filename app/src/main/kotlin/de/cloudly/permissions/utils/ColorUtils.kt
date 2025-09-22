package de.cloudly.permissions.utils

import net.md_5.bungee.api.ChatColor
import java.util.regex.Pattern

/**
 * Utility class for handling color codes and text formatting.
 * Supports both legacy & color codes and modern hex color codes.
 */
object ColorUtils {
    
    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val LEGACY_PATTERN = Pattern.compile("&([0-9A-Fa-fK-Ok-oRr])")
    
    /**
     * Translate color codes in a string.
     * Supports both legacy & codes and hex &#RRGGBB codes.
     * 
     * @param text The text to translate
     * @param supportHex Whether to support hex color codes
     * @param supportLegacy Whether to support legacy & color codes
     * @return The translated text with color codes
     */
    fun translateColors(
        text: String?, 
        supportHex: Boolean = true, 
        supportLegacy: Boolean = true
    ): String? {
        if (text == null) return null
        
        var result = text
        
        // Translate hex colors first (&#RRGGBB)
        if (supportHex) {
            result = translateHexColors(result)
        }
        
        // Translate legacy colors (&a, &c, etc.)
        if (supportLegacy) {
            result = ChatColor.translateAlternateColorCodes('&', result)
        }
        
        return result
    }
    
    /**
     * Translate hex color codes (&#RRGGBB) to ChatColor.
     * 
     * @param text The text containing hex color codes
     * @return The text with hex colors translated
     */
    private fun translateHexColors(text: String): String {
        val matcher = HEX_PATTERN.matcher(text)
        val buffer = StringBuffer()
        
        while (matcher.find()) {
            val hexColor = matcher.group(1)
            try {
                val color = ChatColor.of("#$hexColor")
                matcher.appendReplacement(buffer, color.toString())
            } catch (e: Exception) {
                // If hex color is invalid, leave it as is
                matcher.appendReplacement(buffer, matcher.group(0))
            }
        }
        
        matcher.appendTail(buffer)
        return buffer.toString()
    }
    
    /**
     * Strip all color codes from text.
     * 
     * @param text The text to strip colors from
     * @return The text without color codes
     */
    fun stripColors(text: String?): String? {
        if (text == null) return null
        
        // First strip hex colors
        var result = HEX_PATTERN.matcher(text).replaceAll("")
        
        // Then strip legacy colors
        result = ChatColor.stripColor(result)
        
        return result
    }
    
    /**
     * Check if text contains color codes.
     * 
     * @param text The text to check
     * @return true if the text contains color codes
     */
    fun hasColors(text: String?): Boolean {
        if (text == null) return false
        
        return HEX_PATTERN.matcher(text).find() || LEGACY_PATTERN.matcher(text).find()
    }
    
    /**
     * Format a chat message with prefix and suffix.
     * 
     * @param prefix The prefix to add (can contain color codes)
     * @param message The original message
     * @param suffix The suffix to add (can contain color codes)
     * @param playerName The player's name
     * @param supportHex Whether to support hex colors
     * @param supportLegacy Whether to support legacy colors
     * @return The formatted message
     */
    fun formatChatMessage(
        prefix: String?,
        message: String,
        suffix: String?,
        playerName: String,
        supportHex: Boolean = true,
        supportLegacy: Boolean = true
    ): String {
        val translatedPrefix = translateColors(prefix, supportHex, supportLegacy) ?: ""
        val translatedSuffix = translateColors(suffix, supportHex, supportLegacy) ?: ""
        
        // Add automatic space after prefix if it's not empty
        val prefixWithSpace = if (translatedPrefix.isNotEmpty()) "$translatedPrefix " else ""
        
        return "$prefixWithSpace$playerName$translatedSuffix: $message"
    }
    
    /**
     * Format a tablist entry with prefix and suffix.
     * 
     * @param prefix The prefix to add
     * @param playerName The player's name
     * @param suffix The suffix to add
     * @param supportHex Whether to support hex colors
     * @param supportLegacy Whether to support legacy colors
     * @return The formatted tablist entry
     */
    fun formatTablistEntry(
        prefix: String?,
        playerName: String,
        suffix: String?,
        supportHex: Boolean = true,
        supportLegacy: Boolean = true
    ): String {
        val translatedPrefix = translateColors(prefix, supportHex, supportLegacy) ?: ""
        val translatedSuffix = translateColors(suffix, supportHex, supportLegacy) ?: ""
        
        // Add automatic space after prefix if it's not empty
        val prefixWithSpace = if (translatedPrefix.isNotEmpty()) "$translatedPrefix " else ""
        
        return "$prefixWithSpace$playerName$translatedSuffix"
    }
    
    /**
     * Format a nametag with prefix and suffix.
     * Note: Nametags have character limits in Minecraft.
     * 
     * @param prefix The prefix to add
     * @param playerName The player's name
     * @param suffix The suffix to add
     * @param maxLength Maximum length for nametag (default 16 for older versions)
     * @param supportHex Whether to support hex colors
     * @param supportLegacy Whether to support legacy colors
     * @return The formatted nametag
     */
    fun formatNametag(
        prefix: String?,
        playerName: String,
        suffix: String?,
        maxLength: Int = 16,
        supportHex: Boolean = true,
        supportLegacy: Boolean = true
    ): String {
        val translatedPrefix = translateColors(prefix, supportHex, supportLegacy) ?: ""
        val translatedSuffix = translateColors(suffix, supportHex, supportLegacy) ?: ""
        
        // Add automatic space after prefix if it's not empty
        val prefixWithSpace = if (translatedPrefix.isNotEmpty()) "$translatedPrefix " else ""
        
        val fullName = "$prefixWithSpace$playerName$translatedSuffix"
        
        // Truncate if too long (preserve color codes if possible)
        return if (stripColors(fullName)?.length ?: 0 > maxLength) {
            truncateWithColors(fullName, maxLength)
        } else {
            fullName
        }
    }
    
    /**
     * Truncate text while preserving color codes.
     * 
     * @param text The text to truncate
     * @param maxLength The maximum length (excluding color codes)
     * @return The truncated text
     */
    private fun truncateWithColors(text: String, maxLength: Int): String {
        if (maxLength <= 0) return ""
        
        val stripped = stripColors(text) ?: return ""
        if (stripped.length <= maxLength) return text
        
        // Simple truncation - more sophisticated logic could be added
        val truncateAt = maxLength - 3 // Leave room for "..."
        val strippedTruncated = stripped.substring(0, maxLength.coerceAtMost(truncateAt)) + "..."
        
        return strippedTruncated
    }
    
    /**
     * Get the length of text without color codes.
     * 
     * @param text The text to measure
     * @return The length without color codes
     */
    fun getColorlessLength(text: String?): Int {
        return stripColors(text)?.length ?: 0
    }
    
    /**
     * Apply color formatting to a template string.
     * Replaces placeholders like {player}, {prefix}, {suffix}, etc.
     * 
     * @param template The template string
     * @param placeholders Map of placeholder values
     * @param supportHex Whether to support hex colors
     * @param supportLegacy Whether to support legacy colors
     * @return The formatted string
     */
    fun applyTemplate(
        template: String,
        placeholders: Map<String, String>,
        supportHex: Boolean = true,
        supportLegacy: Boolean = true
    ): String {
        var result = template
        
        // Replace placeholders
        placeholders.forEach { (placeholder, value) ->
            result = result.replace("{$placeholder}", value)
        }
        
        // Apply color codes
        return translateColors(result, supportHex, supportLegacy) ?: result
    }
    
    /**
     * Create a gradient text effect between two colors.
     * Only works with hex colors.
     * 
     * @param text The text to apply gradient to
     * @param startColor Starting hex color (e.g., "FF0000")
     * @param endColor Ending hex color (e.g., "0000FF")
     * @return The gradient text
     */
    fun createGradient(text: String, startColor: String, endColor: String): String {
        if (text.isEmpty()) return text
        
        try {
            val startRgb = hexToRgb(startColor)
            val endRgb = hexToRgb(endColor)
            
            val result = StringBuilder()
            val length = text.length
            
            for (i in text.indices) {
                val progress = if (length == 1) 0f else i.toFloat() / (length - 1)
                
                val r = (startRgb[0] + (endRgb[0] - startRgb[0]) * progress).toInt()
                val g = (startRgb[1] + (endRgb[1] - startRgb[1]) * progress).toInt()
                val b = (startRgb[2] + (endRgb[2] - startRgb[2]) * progress).toInt()
                
                val hexColor = String.format("%02X%02X%02X", r, g, b)
                result.append("&#$hexColor${text[i]}")
            }
            
            return translateColors(result.toString()) ?: result.toString()
        } catch (e: Exception) {
            // If gradient fails, return original text
            return text
        }
    }
    
    /**
     * Convert hex color to RGB array.
     * 
     * @param hex The hex color (without #)
     * @return Array of [R, G, B] values
     */
    private fun hexToRgb(hex: String): IntArray {
        val color = hex.removePrefix("#")
        return intArrayOf(
            color.substring(0, 2).toInt(16),
            color.substring(2, 4).toInt(16),
            color.substring(4, 6).toInt(16)
        )
    }
}
