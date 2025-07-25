package de.cloudly.commands

import de.cloudly.CloudlyPaper
import de.cloudly.config.HotReloadManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Main command handler for the Cloudly plugin.
 * Handles all /cloudly subcommands including hot-reload functionality.
 */
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    
    private val hotReloadManager = HotReloadManager(plugin)
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val languageManager = plugin.getLanguageManager()
        
        // Check permission
        if (!sender.hasPermission("cloudly.admin")) {
            sender.sendMessage(languageManager.getMessage("commands.no_permission"))
            return true
        }
        
        // Handle subcommands
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> handleReloadCommand(sender, args)
            "info" -> handleInfoCommand(sender)
            "version" -> handleVersionCommand(sender)
            "help", null -> handleHelpCommand(sender)
            else -> {
                sender.sendMessage(languageManager.getMessage("commands.unknown_subcommand", "subcommand" to args[0]))
                handleHelpCommand(sender)
            }
        }
        
        return true
    }
    
    /**
     * Handles the reload subcommand with optional specific targets.
     * Usage: /cloudly reload [config|lang|all]
     */
    private fun handleReloadCommand(sender: CommandSender, args: Array<out String>) {
        val languageManager = plugin.getLanguageManager()
        val target = args.getOrNull(1)?.lowercase()
        
        when (target) {
            "config" -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_config"))
                hotReloadManager.reloadConfigOnly(sender)
            }
            "lang", "language", "languages" -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_languages"))
                hotReloadManager.reloadLanguagesOnly(sender)
            }
            "all", null -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.starting_full"))
                hotReloadManager.performHotReload(sender)
            }
            else -> {
                sender.sendMessage(languageManager.getMessage("commands.reload.invalid_target", "target" to target))
                sender.sendMessage(languageManager.getMessage("commands.reload.usage"))
            }
        }
    }
    
    /**
     * Handles the info subcommand.
     * Displays plugin information and current configuration status.
     */
    private fun handleInfoCommand(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        val configManager = plugin.getConfigManager()
        
        sender.sendMessage(languageManager.getMessage("commands.info.header"))
        sender.sendMessage(languageManager.getMessage("commands.info.version", "version" to plugin.description.version))
        sender.sendMessage(languageManager.getMessage("commands.info.language", "language" to languageManager.getCurrentLanguage()))
        sender.sendMessage(languageManager.getMessage("commands.info.debug", "debug" to configManager.getBoolean("plugin.debug", false)))
        
        // Server type detection
        val serverType = if (de.cloudly.utils.SchedulerUtils.isFolia()) "Folia" else "Paper/Spigot"
        sender.sendMessage(languageManager.getMessage("commands.info.server_type", "type" to serverType))
    }
    
    /**
     * Handles the version subcommand.
     * Displays plugin version information.
     */
    private fun handleVersionCommand(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        sender.sendMessage(languageManager.getMessage("commands.version.info", 
            "version" to plugin.description.version,
            "author" to plugin.description.authors.joinToString(", ")
        ))
    }
    
    /**
     * Handles the help subcommand.
     * Displays available commands and their usage.
     */
    private fun handleHelpCommand(sender: CommandSender) {
        val languageManager = plugin.getLanguageManager()
        
        sender.sendMessage(languageManager.getMessage("commands.help.header"))
        sender.sendMessage(languageManager.getMessage("commands.help.reload"))
        sender.sendMessage(languageManager.getMessage("commands.help.info"))
        sender.sendMessage(languageManager.getMessage("commands.help.version"))
        sender.sendMessage(languageManager.getMessage("commands.help.help"))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        // Only provide tab completion for players with permission
        if (!sender.hasPermission("cloudly.admin")) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> {
                // Main subcommands
                listOf("reload", "info", "version", "help")
                    .filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                // Subcommand arguments
                when (args[0].lowercase()) {
                    "reload" -> listOf("config", "lang", "all")
                        .filter { it.startsWith(args[1].lowercase()) }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}