/*
 * Cloudly - Player Freeze Manager
 * 
 * Manages freezing players who are not whitelisted and have pending applications.
 * Prevents movement, block interaction, and most game actions while preserving chat.
 */
package cloudly.util

import cloudly.CloudlyPlugin
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * PlayerFreezeManager handles freezing players pending whitelist approval
 */
object PlayerFreezeManager : Listener {
    
    private lateinit var plugin: CloudlyPlugin
    private val frozenPlayers = ConcurrentHashMap<UUID, FrozenPlayerData>()
    
    /**
     * Data class for frozen player information
     */
    data class FrozenPlayerData(
        val uuid: UUID,
        val playerName: String,
        val frozenAt: Long,
        val originalGameMode: GameMode,
        val originalLocation: org.bukkit.Location
    )
    
    /**
     * Initialize the freeze manager
     */
    fun initialize(pluginInstance: CloudlyPlugin) {
        plugin = pluginInstance
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }
    
    /**
     * Freeze a player
     */
    fun freezePlayer(player: Player, reason: String = "Pending whitelist application") {
        if (frozenPlayers.containsKey(player.uniqueId)) {
            return // Already frozen
        }
        
        val frozenData = FrozenPlayerData(
            uuid = player.uniqueId,
            playerName = player.name,
            frozenAt = System.currentTimeMillis(),
            originalGameMode = player.gameMode,
            originalLocation = player.location.clone()
        )
        
        frozenPlayers[player.uniqueId] = frozenData
        
        // Set player to adventure mode to prevent block breaking/placing
        player.gameMode = GameMode.ADVENTURE
        
        // Send freeze message
        LanguageManager.sendMessage(player, "whitelist.freeze.frozen", reason)
        
        // Send title
        player.sendTitle(
            LanguageManager.getMessage("whitelist.freeze.title"),
            LanguageManager.getMessage("whitelist.freeze.subtitle"),
            10, 100, 20
        )
        
        // Start periodic reminders
        startFreezeReminders(player)
    }
    
    /**
     * Unfreeze a player
     */
    fun unfreezePlayer(player: Player) {
        val frozenData = frozenPlayers.remove(player.uniqueId) ?: return
        
        // Restore original game mode
        player.gameMode = frozenData.originalGameMode
        
        // Send unfreeze message
        LanguageManager.sendMessage(player, "whitelist.freeze.unfrozen")
        
        // Send title
        player.sendTitle(
            LanguageManager.getMessage("whitelist.freeze.unfrozen.title"),
            LanguageManager.getMessage("whitelist.freeze.unfrozen.subtitle"),
            10, 70, 20
        )
        
        // Play sound
        player.playSound(player.location, "entity.player.levelup", 1.0f, 1.0f)
    }
    
    /**
     * Check if a player is frozen
     */
    fun isPlayerFrozen(player: Player): Boolean {
        return frozenPlayers.containsKey(player.uniqueId)
    }
    
    /**
     * Get frozen player data
     */
    fun getFrozenPlayerData(uuid: UUID): FrozenPlayerData? {
        return frozenPlayers[uuid]
    }
    
    /**
     * Get all frozen players
     */
    fun getFrozenPlayers(): List<FrozenPlayerData> {
        return frozenPlayers.values.toList()
    }
    
    /**
     * Start periodic reminders for frozen player
     */
    private fun startFreezeReminders(player: Player) {
        val taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!player.isOnline || !frozenPlayers.containsKey(player.uniqueId)) {
                return@Runnable
            }
            
            // Send reminder message
            LanguageManager.sendMessage(player, "whitelist.freeze.reminder")
            
            // Send action bar
            player.sendTitle("", "", 0, 40, 0)
            player.sendTitle(
                "",
                LanguageManager.getMessage("whitelist.freeze.actionbar"),
                0, 40, 0
            )
        }, 60L * 20L, 60L * 20L) // Every 60 seconds
        
        // Store task ID for cleanup
        frozenPlayers[player.uniqueId]?.let { frozenData ->
            // We'll add task ID to the data class if needed
        }
    }
    
    // Event Handlers
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        if (!isPlayerFrozen(player)) return
        
        // Allow looking around but prevent movement
        val from = event.from
        val to = event.to ?: return
        
        if (from.x != to.x || from.y != to.y || from.z != to.z) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.move")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.break")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.place")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.interact")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.inventory")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.drop")
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerPickupItem(event: PlayerPickupItemEvent) {
        val player = event.player
        
        if (isPlayerFrozen(player)) {
            event.isCancelled = true
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player
        val victim = event.entity as? Player
        
        // Prevent frozen players from attacking
        if (damager != null && isPlayerFrozen(damager)) {
            event.isCancelled = true
            LanguageManager.sendMessage(damager, "whitelist.freeze.cannot.attack")
        }
          // Prevent attacking frozen players (optional protection)
        if (victim != null && isPlayerFrozen(victim)) {
            event.isCancelled = true
            damager?.let { player: Player -> LanguageManager.sendMessage(player, "whitelist.freeze.cannot.attack.frozen") }
        }
    }
    
    @EventHandler
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        
        if (!isPlayerFrozen(player)) return
        
        val command = event.message.lowercase()
        
        // Allow certain commands for frozen players
        val allowedCommands = listOf(
            "/whitelist", "/wl", "/help", "/rules", "/info", "/spawn", "/hub", "/lobby"
        )
        
        if (allowedCommands.none { command.startsWith(it) }) {
            event.isCancelled = true
            LanguageManager.sendMessage(player, "whitelist.freeze.cannot.command")
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        frozenPlayers.remove(player.uniqueId)
    }
    
    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        val player = event.player
        frozenPlayers.remove(player.uniqueId)
    }
}
