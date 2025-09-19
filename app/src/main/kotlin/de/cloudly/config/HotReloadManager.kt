package de.cloudly.config

import de.cloudly.CloudlyPaper
import org.bukkit.command.CommandSender
import java.util.logging.Level

/**
 * Manages hot-reload functionality for the Cloudly plugin.
 * Provides centralized reload capabilities for configuration, languages, and other components.
 */
class HotReloadManager(private val plugin: CloudlyPaper) {
    
    /**
     * Performs a complete hot-reload of all plugin components.
     * This includes configuration files, language files, and reloadable components.
     * 
     * @param sender The command sender requesting the reload (for feedback messages)
     * @return true if reload was successful, false if any component failed to reload
     */
    fun performHotReload(sender: CommandSender?): Boolean {
        val languageManager = plugin.getLanguageManager()
        var success = true
        
        try {
            // Reload configuration
            sender?.sendMessage(languageManager.getMessage("commands.reload.reloading_config"))
            plugin.getConfigManager().reloadConfig()
            plugin.logger.info(languageManager.getMessage("commands.reload.config_reloaded"))
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload configuration", e)
            sender?.sendMessage(languageManager.getMessage("commands.reload.config_failed"))
            success = false
        }
        
        try {
            // Reload language files
            sender?.sendMessage(languageManager.getMessage("commands.reload.reloading_languages"))
            languageManager.reloadLanguages()
            plugin.logger.info(languageManager.getMessage("commands.reload.languages_reloaded"))
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload language files", e)
            sender?.sendMessage(languageManager.getMessage("commands.reload.languages_failed"))
            success = false
        }
        
        // Component reload section removed
        
        // Send completion message
        if (success) {
            sender?.sendMessage(languageManager.getMessage("commands.reload.success"))
            plugin.logger.info(languageManager.getMessage("commands.reload.success_log"))
        } else {
            sender?.sendMessage(languageManager.getMessage("commands.reload.partial_failure"))
            plugin.logger.warning(languageManager.getMessage("commands.reload.partial_failure_log"))
        }
        
        return success
    }
    
    // ReleaseRadar functionality removed
    
    /**
     * Performs a reload of only the configuration files (config.yml).
     * 
     * @param sender The command sender requesting the reload
     * @return true if reload was successful
     */
    fun reloadConfigOnly(sender: CommandSender?): Boolean {
        return try {
            val languageManager = plugin.getLanguageManager()
            sender?.sendMessage(languageManager.getMessage("commands.reload.reloading_config"))
            plugin.getConfigManager().reloadConfig()
            sender?.sendMessage(languageManager.getMessage("commands.reload.config_success"))
            plugin.logger.info(languageManager.getMessage("commands.reload.config_reloaded"))
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload configuration", e)
            sender?.sendMessage(plugin.getLanguageManager().getMessage("commands.reload.config_failed"))
            false
        }
    }
    
    /**
     * Performs a reload of only the language files.
     * 
     * @param sender The command sender requesting the reload
     * @return true if reload was successful
     */
    fun reloadLanguagesOnly(sender: CommandSender?): Boolean {
        return try {
            val languageManager = plugin.getLanguageManager()
            sender?.sendMessage(languageManager.getMessage("commands.reload.reloading_languages"))
            languageManager.reloadLanguages()
            sender?.sendMessage(languageManager.getMessage("commands.reload.languages_success"))
            plugin.logger.info(languageManager.getMessage("commands.reload.languages_reloaded"))
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload language files", e)
            sender?.sendMessage(plugin.getLanguageManager().getMessage("commands.reload.languages_failed"))
            false
        }
    }
}
