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
    private val openPendingMenus = ConcurrentHashMap<UUID, PendingWhitelistGui>()

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
        if (openPendingMenus.remove(player.uniqueId) != null) {
            player.closeInventory()
        }
    }

    /**
     * Called by GUIs once they dispose.
     */
    internal fun unregisterGui(playerUuid: UUID) {
        openDashboards.remove(playerUuid)
    }

    fun openPendingWhitelistGui(player: Player, initialPage: Int = 0) {
        closePendingGui(player)

        val attempts = plugin.getWhitelistAttemptService().getAttempts()
        if (attempts.isEmpty()) {
            player.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            return
        }

        val gui = PendingWhitelistGui(plugin, player, initialPage)
        openPendingMenus[player.uniqueId] = gui
        gui.open()
    }

    fun closePendingGui(player: Player) {
        if (openPendingMenus.remove(player.uniqueId) != null) {
            player.closeInventory()
        }
    }

    internal fun unregisterPendingGui(playerUuid: UUID) {
        openPendingMenus.remove(playerUuid)
    }

    fun closeAll() {
        openDashboards.keys
            .mapNotNull { plugin.server.getPlayer(it) }
            .forEach { it.closeInventory() }
        openDashboards.clear()

        openPendingMenus.keys
            .mapNotNull { plugin.server.getPlayer(it) }
            .forEach { it.closeInventory() }
        openPendingMenus.clear()
    }
}
