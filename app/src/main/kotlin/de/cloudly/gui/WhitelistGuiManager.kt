package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for handling whitelist GUI instances.
 * Ensures proper cleanup and prevents multiple instances per player.
 */
class WhitelistGuiManager(private val plugin: CloudlyPaper) {
    
    private val openGuis = ConcurrentHashMap<UUID, WhitelistGui>()
    
    /**
     * Opens the whitelist GUI for a player.
     * If the player already has a GUI open, it will be closed first.
     */
    fun openWhitelistGui(player: Player, initialPage: Int = 0) {
        // Close existing GUI if any
        closeGui(player)
        
        // Check if whitelist is empty
        val whitelistedPlayers = plugin.getWhitelistService().getAllPlayers()
        if (whitelistedPlayers.isEmpty()) {
            player.sendMessage(Messages.Commands.Whitelist.LIST_EMPTY)
            return
        }
        
        // Create and open new GUI
        val gui = WhitelistGui(plugin, player, initialPage)
        openGuis[player.uniqueId] = gui
        gui.open()
    }
    
    /**
     * Closes the GUI for a specific player.
     */
    fun closeGui(player: Player) {
        val gui = openGuis.remove(player.uniqueId)
        if (gui != null) {
            player.closeInventory()
        }
    }
    
    /**
     * Checks if a player has an open whitelist GUI.
     */
    fun hasOpenGui(player: Player): Boolean {
        return openGuis.containsKey(player.uniqueId)
    }
    
    /**
     * Gets the GUI instance for a player, if any.
     */
    fun getGui(player: Player): WhitelistGui? {
        return openGuis[player.uniqueId]
    }
    
    fun closeAll() {
        openGuis.keys
            .mapNotNull { plugin.server.getPlayer(it) }
            .forEach { it.closeInventory() }
        openGuis.clear()
    }
    
    /**
     * Removes a GUI from tracking when it's closed.
     * This is called by the GUI itself when it's cleaned up.
     */
    internal fun unregisterGui(playerUuid: UUID) {
        openGuis.remove(playerUuid)
    }
}