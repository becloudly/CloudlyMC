package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
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
        // Check if user has whitelist permission
        if (!sender.hasPermission("cloudly.whitelist")) {
            sender.sendMessage(Messages.Commands.NO_PERMISSION)
            return true
        }
        
        // Send blocking message and redirect to Cloudly whitelist
        sender.sendMessage(Messages.Commands.VanillaWhitelist.DISABLED)
        sender.sendMessage(Messages.Commands.VanillaWhitelist.USE_CLOUDLY)
        sender.sendMessage(Messages.Commands.VanillaWhitelist.HELP_MESSAGE)
        
        return true
    }
}
