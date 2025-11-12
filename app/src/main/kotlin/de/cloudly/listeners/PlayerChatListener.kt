package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.discord.DiscordCodeValidationResult
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Level

/**
 * Handles player chat events to provide custom message formatting.
 */
class PlayerChatListener(private val plugin: CloudlyPaper) : Listener {
    
    
    /**
     * Handle player chat events.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncChatEvent) {
        try {
            val player = event.player
            val discordService = plugin.getDiscordService()
            val verificationListener = plugin.getDiscordVerificationListener()

            // Handle verification codes before broadcasting chat messages
            if (discordService.hasPendingVerification(player.uniqueId)) {
                event.isCancelled = true
                val plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    handleVerificationCodeInput(player, plainMessage)
                })
                return
            }

            // Block regular chat for players that still need to verify
            if (verificationListener.isAwaitingVerification(player.uniqueId)) {
                event.isCancelled = true
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.sendMessage(Messages.Commands.Discord.VERIFICATION_CHAT_BLOCKED)
                })
                return
            }
            
            // Create the formatted message
            val formattedMessage = Component.text()
                .append(Component.text(player.name, NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(Component.text().color(NamedTextColor.GRAY).append(event.message()))
                .build()
            
            // Set the custom renderer to display our formatted message
            event.renderer { _, _, message, _ ->
                formattedMessage
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to format chat message", e)
        }
    }

    private fun handleVerificationCodeInput(player: org.bukkit.entity.Player, codeInput: String) {
        val discordService = plugin.getDiscordService()
        val whitelistService = plugin.getWhitelistService()

        if (codeInput.isBlank()) {
            player.sendMessage(Messages.Commands.Discord.CODE_INVALID)
            return
        }

        if (whitelistService.getPlayer(player.uniqueId) == null) {
            player.sendMessage(Messages.Commands.Discord.NOT_WHITELISTED)
            return
        }

        when (val result = discordService.validateVerificationCode(player.uniqueId, codeInput, whitelistService)) {
            is DiscordCodeValidationResult.Success -> {
                val updated = whitelistService.updatePlayerDiscord(player.uniqueId, result.connection, player.uniqueId)
                if (updated) {
                    player.sendMessage(Messages.Commands.Discord.connectedSuccessfully(result.connection.discordUsername))
                    plugin.getDiscordVerificationListener().markPlayerVerified(player)
                } else {
                    player.sendMessage(Messages.Commands.Discord.CONNECTION_FAILED)
                }
            }
            DiscordCodeValidationResult.InvalidCode -> {
                player.sendMessage(Messages.Commands.Discord.CODE_INVALID)
            }
            DiscordCodeValidationResult.CodeExpired -> {
                player.sendMessage(Messages.Commands.Discord.CODE_EXPIRED)
            }
            DiscordCodeValidationResult.NoPending -> {
                player.sendMessage(Messages.Commands.Discord.NO_PENDING_VERIFICATION)
            }
            DiscordCodeValidationResult.AccountInUse -> {
                player.sendMessage(Messages.Commands.Discord.ACCOUNT_ALREADY_IN_USE)
            }
        }
    }
}
