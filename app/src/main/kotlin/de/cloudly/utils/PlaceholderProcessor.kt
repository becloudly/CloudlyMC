package de.cloudly.utils

import org.bukkit.entity.Player

/**
 * Processes placeholders in text templates efficiently and maintainably.
 * 
 * This class provides a centralized, extensible way to handle placeholder replacement
 * in messages. Instead of chaining multiple string.replace() calls, placeholders are
 * registered once and can be processed in batch operations.
 * 
 * Example usage:
 * ```kotlin
 * val processor = PlaceholderProcessor()
 * processor.register("player_name") { it.name }
 * processor.register("player_display_name") { it.displayName }
 * 
 * val message = processor.process("Welcome {player_name}!", player)
 * ```
 * 
 * @see PlayerConnectionListener for usage example
 */
class PlaceholderProcessor {
    private val placeholders = mutableMapOf<String, (Player) -> String>()
    
    /**
     * Register a placeholder with its resolver function.
     * 
     * @param key The placeholder key (without braces)
     * @param resolver Function that takes a Player and returns the replacement value
     */
    fun register(key: String, resolver: (Player) -> String) {
        placeholders[key] = resolver
    }
    
    /**
     * Process a template string by replacing all registered placeholders.
     * 
     * @param template The template string containing placeholders in {key} format
     * @param player The player context for resolving placeholder values
     * @return The processed string with all placeholders replaced
     */
    fun process(template: String, player: Player): String {
        var result = template
        placeholders.forEach { (key, resolver) ->
            result = result.replace("{$key}", resolver(player))
        }
        return result
    }
}
