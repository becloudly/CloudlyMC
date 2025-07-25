package de.cloudly.config

/**
 * Interface for components that need to be notified when configuration changes occur.
 * Implement this interface to receive callbacks when the configuration is reloaded.
 */
interface ConfigChangeListener {
    
    /**
     * Called before the configuration reload process begins.
     * Use this method to prepare for configuration changes, save state, or pause operations.
     */
    fun onConfigReloadStart() {}
    
    /**
     * Called after the configuration has been successfully reloaded.
     * Use this method to update internal state based on new configuration values.
     * 
     * @param configManager The updated configuration manager instance
     * @param languageManager The updated language manager instance
     */
    fun onConfigReloaded(configManager: ConfigManager, languageManager: LanguageManager)
    
    /**
     * Called if the configuration reload process fails.
     * Use this method to handle reload failures gracefully and restore previous state if needed.
     * 
     * @param error The exception that caused the reload to fail
     */
    fun onConfigReloadFailed(error: Exception) {}
}
