package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Read-only viewer that mirrors a player's inventory layout for staff review.
 */
class PlayerInventoryGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val target: Player,
    private val onReturn: () -> Unit
) : Listener {

    private var inventory: Inventory? = null
    private var cleanedUp = false

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        val title = Messages.Gui.PlayerAdmin.inventoryTitle(target.name)
        inventory = Bukkit.createInventory(null, 54, title)
        populateInventory()
        viewer.openInventory(requireNotNull(inventory))
    }

    private fun populateInventory() {
        val inv = inventory ?: return
        inv.clear()

        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§7") }
        }

        for (slot in 36 until 45) {
            inv.setItem(slot, filler)
        }

        val storageContents = target.inventory.storageContents
        storageContents.forEachIndexed { index, item ->
            inv.setItem(index, item?.clone())
        }

        inv.setItem(45, target.inventory.helmet?.clone())
        inv.setItem(46, target.inventory.chestplate?.clone())
        inv.setItem(47, target.inventory.leggings?.clone())
        inv.setItem(48, target.inventory.boots?.clone())
        inv.setItem(50, target.inventory.itemInOffHand?.clone())
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        plugin.server.scheduler.runTask(plugin, Runnable { onReturn() })
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        inventory = null
    }
}
