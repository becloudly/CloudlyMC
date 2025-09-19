package de.cloudly.commands

import de.cloudly.CloudlyPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Command executor that intercepts the vanilla Minecraft whitelist command
 * and redirects users to use the CloudlyMC whitelist system instead.
 */
class VanillaWhitelistCommand(private val plugin: CloudlyPaper) : CommandExecutor {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val languageManager = plugin.getLanguageManager()
        
        // Check if user has whitelist permission
        if (!sender.hasPermission("cloudly.whitelist")) {
            sender.sendMessage(languageManager.getMessage("commands.no_permission"))
            return true
        }
        
        // Send blocking message and redirect to Cloudly whitelist
        sender.sendMessage(languageManager.getMessage("commands.vanilla_whitelist.disabled"))
        sender.sendMessage(languageManager.getMessage("commands.vanilla_whitelist.use_cloudly"))
        sender.sendMessage(languageManager.getMessage("commands.vanilla_whitelist.help_message"))
        
        return true
    }
}
