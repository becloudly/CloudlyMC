package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for admin GUI instances.
 * Keeps track of open menus so we can reuse and clean them up.
 */
class AdminGuiManager(private val plugin: CloudlyPaper) {

    private val openDashboards = ConcurrentHashMap<UUID, AdminGui>()

    /**
     * Opens the admin dashboard for the given player.
     */
    fun openAdminGui(player: Player, initialPage: Int = 0) {
        closeGui(player)

        val trackedPlayers = plugin.getWhitelistService().getAllPlayers()
        if (trackedPlayers.isEmpty()) {
            player.sendMessage(Messages.Gui.Admin.NO_PLAYERS_TRACKED)
            return
        }

        val gui = AdminGui(plugin, player, initialPage)
        openDashboards[player.uniqueId] = gui
        gui.open()
    }

    /**
     * Force-close any open GUI for the player.
     */
    fun closeGui(player: Player) {
        if (openDashboards.remove(player.uniqueId) != null) {
            player.closeInventory()
        }
    }

    /**
     * Called by GUIs once they dispose.
     */
    internal fun unregisterGui(playerUuid: UUID) {
        openDashboards.remove(playerUuid)
    }

    fun closeAll() {
        openDashboards.keys
            .mapNotNull { plugin.server.getPlayer(it) }
            .forEach { it.closeInventory() }
        openDashboards.clear()
    }
}
