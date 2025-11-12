package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.discord.DiscordLinkHealth
import de.cloudly.whitelist.model.DiscordConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Listener to enforce Discord verification requirements.
 * - Players must verify within 5 minutes or get kicked
 * - Players cannot move, interact, or do anything until verified
 * - Shows verification messages to players
 */
class DiscordVerificationListener(private val plugin: CloudlyPaper) : Listener {
    
    // Track players awaiting verification
    private val awaitingVerification = ConcurrentHashMap<UUID, VerificationState>()
    
    // Timeout tasks for each player
    private val timeoutTasks = ConcurrentHashMap<UUID, BukkitTask?>()

    // Track which players were hidden from awaiting players so we can reveal them later
    private val hiddenPlayers = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private enum class VerificationTrigger(val logLabel: String) {
        JOIN("player join"),
        MANUAL("manual reset")
    }
    
    /**
     * State tracking for a player awaiting verification.
     */
    private data class VerificationState(
        val joinedAt: Instant,
        val uuid: UUID,
        val name: String,
        var warningsSent: Int = 0
    )
    
    /**
     * Handle player join to start verification process.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoinVerification(event: PlayerJoinEvent) {
        val player = event.player
        
        try {
            // Check if Discord verification is enabled
            val discordService = plugin.getDiscordService()
            if (!discordService.isEnabled()) {
                return
            }

            val whitelistPlayer = plugin.getWhitelistService().getPlayer(player.uniqueId)
            val discordConnection = whitelistPlayer?.discordConnection
            if (discordConnection?.verified == true) {
                verifyLinkedAccountOnJoin(player, discordConnection)
                return
            }

            // Check if verification is required
            val requireVerification = plugin.getConfigManager().getBoolean("discord.require_verification", false)
            if (!requireVerification) {
                return
            }

            startVerificationFlow(player, VerificationTrigger.JOIN, overrideExisting = true)
            
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Error starting Discord verification for ${player.name}", e)
        }
    }
    
    /**
     * Apply restrictions to unverified player.
     */
    private fun applyRestrictions(player: Player) {
        // Set to adventure mode to prevent block breaking
        player.gameMode = GameMode.ADVENTURE
        
        // Apply blindness effect for "black screen"
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                Int.MAX_VALUE,
                1,
                false,
                false,
                false
            )
        )
        
        // Apply slowness to prevent movement
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                Int.MAX_VALUE,
                10,
                false,
                false,
                false
            )
        )

        // Hide all other players from this user until verification completes
        hideOtherPlayersFor(player)
    }

    private fun startVerificationFlow(player: Player, trigger: VerificationTrigger, overrideExisting: Boolean) {
        val uuid = player.uniqueId

        if (!overrideExisting && awaitingVerification.containsKey(uuid)) {
            return
        }

        awaitingVerification[uuid] = VerificationState(
            joinedAt = Instant.now(),
            uuid = uuid,
            name = player.name
        )

        hidePlayerFromAwaitingPlayers(player)
        applyRestrictions(player)
        hidePlayerFromAwaitingPlayers(player)

        sendVerificationMessage(player)

        val timeoutMinutes = plugin.getConfigManager().getInt("discord.verification_timeout_minutes", 5).toLong()
        scheduleTimeout(player, timeoutMinutes)

        plugin.logger.info("Player ${player.name} requires Discord verification (${trigger.logLabel})")
    }
    
    /**
     * Remove restrictions from verified player.
     */
    private fun removeRestrictions(player: Player) {
        // Remove effects
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        
        // Set back to survival (or their previous gamemode)
        player.gameMode = GameMode.SURVIVAL

        // Reveal everyone again
        showHiddenPlayersFor(player)
    }

    /**
     * Hide all currently online players from the awaiting player.
     */
    private fun hideOtherPlayersFor(player: Player) {
        val hiddenSet = hiddenPlayers.computeIfAbsent(player.uniqueId) { ConcurrentHashMap.newKeySet() }
        Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != player.uniqueId }
            .forEach { other ->
                player.hidePlayer(plugin, other)
                hiddenSet.add(other.uniqueId)
            }
    }

    /**
     * Hide the given player from everyone that is still awaiting verification.
     */
    private fun hidePlayerFromAwaitingPlayers(target: Player) {
        awaitingVerification.keys
            .filter { it != target.uniqueId }
            .forEach { awaitingUuid ->
                Bukkit.getPlayer(awaitingUuid)?.let { awaitingPlayer ->
                    val hiddenSet = hiddenPlayers.computeIfAbsent(awaitingUuid) { ConcurrentHashMap.newKeySet() }
                    awaitingPlayer.hidePlayer(plugin, target)
                    hiddenSet.add(target.uniqueId)
                }
            }
    }

    /**
     * Reveal players that had been hidden from an awaiting player.
     */
    private fun showHiddenPlayersFor(player: Player) {
        hiddenPlayers.remove(player.uniqueId)?.forEach { hiddenUuid ->
            Bukkit.getPlayer(hiddenUuid)?.let { player.showPlayer(plugin, it) }
        }
    }
    
    /**
     * Send verification message to player.
     */
    private fun sendVerificationMessage(player: Player) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline && awaitingVerification.containsKey(player.uniqueId)) {
                player.sendMessage(Messages.Commands.Discord.VERIFICATION_REQUIRED)
                player.sendMessage(Messages.Commands.Discord.VERIFICATION_COMMAND)
            }
        }, 20L) // Send 1 second after join
    }
    
    /**
     * Schedule timeout kick for unverified player.
     */
    private fun scheduleTimeout(player: Player, timeoutMinutes: Long) {
        // Cancel existing timeout if any
        timeoutTasks.remove(player.uniqueId)?.cancel()
        
        val timeoutTicks = timeoutMinutes * 60L * 20L // Convert minutes to ticks
        
        // Schedule warnings at 3 min, 2 min remaining
        val warnings = listOf(
            (timeoutMinutes - 3L) to Messages.Commands.Discord.VERIFICATION_WARNING_3MIN,
            (timeoutMinutes - 4L) to Messages.Commands.Discord.VERIFICATION_WARNING_2MIN
        )
        
        warnings.forEach { (minutesMark, message) ->
            if (minutesMark > 0L) {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (player.isOnline && awaitingVerification.containsKey(player.uniqueId)) {
                        player.sendMessage(message)
                    }
                }, minutesMark * 60L * 20L)
            }
        }
        
        // Schedule 30 second warning separately
        if (timeoutMinutes >= 1L) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (player.isOnline && awaitingVerification.containsKey(player.uniqueId)) {
                    player.sendMessage(Messages.Commands.Discord.VERIFICATION_WARNING_30SEC)
                }
            }, (timeoutTicks - 600L)) // 30 seconds before timeout (600 ticks)
        }
        
        // Schedule final kick
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline && awaitingVerification.containsKey(player.uniqueId)) {
                player.kickPlayer(Messages.Commands.Discord.VERIFICATION_TIMEOUT)
                plugin.logger.info("Player ${player.name} kicked for not verifying Discord within $timeoutMinutes minutes")
            }
        }, timeoutTicks)
        
        // Store the task
        timeoutTasks[player.uniqueId] = task
    }

    private fun verifyLinkedAccountOnJoin(player: Player, connection: DiscordConnection) {
        val playerUuid = player.uniqueId
        val playerName = player.name

        coroutineScope.launch {
            when (val health = plugin.getDiscordService().evaluateLinkedAccount(connection)) {
                DiscordLinkHealth.Valid, DiscordLinkHealth.ServiceDisabled -> {
                    // Nothing to do, player may continue playing
                }
                DiscordLinkHealth.NotServerMember -> {
                    plugin.logger.info("Player $playerName kicked: Discord account not on server")
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getPlayer(playerUuid)?.takeIf { it.isOnline }?.kickPlayer(Messages.Commands.Discord.JOIN_NOT_MEMBER)
                    })
                }
                DiscordLinkHealth.MissingRole -> {
                    plugin.logger.info("Player $playerName kicked: Discord role missing")
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getPlayer(playerUuid)?.takeIf { it.isOnline }?.kickPlayer(Messages.Commands.Discord.joinMissingRole(null))
                    })
                }
                is DiscordLinkHealth.ApiError -> {
                    plugin.logger.log(Level.WARNING, "Could not validate Discord link for $playerName: ${health.message}")
                }
            }
        }
    }

    /**
     * Reapply the verification flow for a player, enforcing restrictions again.
     */
    fun restartVerification(player: Player, force: Boolean = false) {
        val discordService = plugin.getDiscordService()
        if (!discordService.isEnabled()) {
            return
        }

        val verificationRequired = force || plugin.getConfigManager().getBoolean("discord.require_verification", false)
        if (!verificationRequired) {
            return
        }

        startVerificationFlow(player, VerificationTrigger.MANUAL, overrideExisting = true)
    }
    
    /**
     * Mark player as verified and remove restrictions.
     */
    fun markPlayerVerified(player: Player) {
        if (awaitingVerification.remove(player.uniqueId) != null) {
            // Cancel timeout task
            timeoutTasks.remove(player.uniqueId)?.cancel()
            
            // Remove restrictions
            removeRestrictions(player)
            
            // Send success message
            player.sendMessage(Messages.Commands.Discord.VERIFICATION_SUCCESS)
            
            plugin.logger.info("Player ${player.name} successfully verified Discord")
        }
    }
    
    /**
     * Check if player is awaiting verification.
     */
    fun isAwaitingVerification(uuid: UUID): Boolean {
        return awaitingVerification.containsKey(uuid)
    }
    
    /**
     * Prevent movement for unverified players.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (awaitingVerification.containsKey(event.player.uniqueId)) {
            // Allow looking around but not moving
            if (event.from.x != event.to?.x || event.from.z != event.to?.z || event.from.y != event.to?.y) {
                event.isCancelled = true
            }
        }
    }
    
    /**
     * Prevent interaction for unverified players.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (awaitingVerification.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
    
    /**
     * Prevent item drops for unverified players.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (awaitingVerification.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
    
    /**
     * Prevent chat for unverified players while linking is pending.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerChat(event: PlayerChatEvent) {
        val uuid = event.player.uniqueId
        if (!awaitingVerification.containsKey(uuid)) {
            return
        }

        if (plugin.getDiscordService().hasPendingVerification(uuid)) {
            // Allow the async chat listener to intercept verification codes silently
            return
        }

        event.isCancelled = true
        event.player.sendMessage(Messages.Commands.Discord.VERIFICATION_CHAT_BLOCKED)
    }
    
    /**
     * Prevent commands for players that still need to complete linking, allowing only link-related commands.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        val uuid = event.player.uniqueId
        val awaiting = awaitingVerification.containsKey(uuid)
        val pendingLink = plugin.getDiscordService().hasPendingVerification(uuid)

        if (!awaiting && !pendingLink) {
            return
        }

        val parts = event.message.trim().lowercase().split("\\s+".toRegex())
        if (parts.isEmpty()) {
            return
        }

        val baseCommand = parts[0]
        val subcommand = parts.getOrNull(1)

        val isCloudly = baseCommand == "/cloudly"
        val isLink = subcommand == "link"
        val isUnlink = subcommand == "unlink"

        if (!(isCloudly && (isLink || isUnlink))) {
            event.isCancelled = true
            event.player.sendMessage(Messages.Commands.Discord.VERIFICATION_COMMAND_BLOCKED)
        }
    }

    /**
     * Prevent block breaking for unverified players as additional safeguard.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (awaitingVerification.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
    
    /**
     * Clean up when player quits.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        awaitingVerification.remove(uuid)
        timeoutTasks.remove(uuid)?.cancel()
        hiddenPlayers.remove(uuid)
    }
    
    /**
     * Shutdown and clean up.
     */
    fun shutdown() {
        // Cancel all timeout tasks
        timeoutTasks.values.forEach { it?.cancel() }
        timeoutTasks.clear()
        awaitingVerification.clear()
        hiddenPlayers.clear()
        coroutineScope.cancel()
    }
}
