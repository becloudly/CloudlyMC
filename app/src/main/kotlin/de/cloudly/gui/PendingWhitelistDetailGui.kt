package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.whitelist.attempts.WhitelistAttemptService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Detail view for a single pending whitelist attempt, enabling staff to whitelist or dismiss it.
 */
class PendingWhitelistDetailGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val attemptUuid: UUID,
    private val parentPage: Int
) : Listener {

    private val attemptService = plugin.getWhitelistAttemptService()
    private val whitelistService = plugin.getWhitelistService()

    private var inventory: Inventory? = null
    private var cleanedUp = false
    private var reopenParent = false
    private var lastKnownName: String = ""

    companion object {
        private const val INVENTORY_SIZE = 27
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private const val SLOT_INFO = 13
        private const val SLOT_MESSAGE = 11
        private const val SLOT_ADD = 14
        private const val SLOT_DISMISS = 15
        private const val SLOT_BACK = 22
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        val snapshot = attemptService.getAttempt(attemptUuid)
        if (snapshot == null) {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            plugin.server.scheduler.runTask(plugin, Runnable {
                plugin.getAdminGuiManager().openPendingWhitelistGui(viewer, parentPage)
            })
            cleanup()
            return
        }

        lastKnownName = snapshot.username
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, Messages.Gui.PendingWhitelist.detailTitle(snapshot.username))
        updateInventory(snapshot)
        viewer.openInventory(requireNotNull(inventory))
    }

    private fun updateInventory(snapshotParam: WhitelistAttemptService.AttemptSnapshot? = null) {
        val inv = inventory ?: return
        inv.clear()

        val snapshot = snapshotParam ?: attemptService.getAttempt(attemptUuid)
        if (snapshot == null) {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            reopenParent = true
            viewer.closeInventory()
            return
        }

        if (snapshot.username != lastKnownName) {
            lastKnownName = snapshot.username
            reopenParent = true
            inv.viewers.toList().forEach { it.closeInventory() }
            return
        }

        fillBorder(inv)
        inv.setItem(SLOT_INFO, createInfoItem(snapshot))
        inv.setItem(SLOT_MESSAGE, createMessageItem(snapshot))
        inv.setItem(SLOT_ADD, createAddItem())
        inv.setItem(SLOT_DISMISS, createDismissItem())
        inv.setItem(SLOT_BACK, createBackItem())
    }

    private fun fillBorder(inv: Inventory) {
        val borderItem = ItemStack(BORDER_MATERIAL).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§7") }
        }
        for (slot in 0 until INVENTORY_SIZE step 9) {
            for (col in 0 until 9) {
                if (slot == 0 || slot == INVENTORY_SIZE - 9 || col == 0 || col == 8) {
                    inv.setItem(slot + col, borderItem)
                }
            }
        }
    }

    private fun createInfoItem(snapshot: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.itemMeta = (item.itemMeta as SkullMeta).apply {
            owningPlayer = Bukkit.getOfflinePlayer(snapshot.uuid)
            setDisplayName(Messages.Gui.PendingWhitelist.playerLabel(snapshot.username))
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PendingWhitelist.infoUuid(snapshot.uuid.toString()))
            lore.add(Messages.Gui.PendingWhitelist.infoAttempts(snapshot.attemptCount))
            lore.add(Messages.Gui.PendingWhitelist.infoFirstAttempt(DATE_FORMAT.format(snapshot.firstAttempt)))
            lore.add(Messages.Gui.PendingWhitelist.infoLastAttempt(DATE_FORMAT.format(snapshot.lastAttempt)))
            snapshot.lastAddress?.let {
                lore.add(Messages.Gui.PendingWhitelist.infoAddress(it))
            } ?: lore.add(Messages.Gui.PendingWhitelist.DETAIL_NO_ADDRESS)
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createMessageItem(snapshot: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        val item = ItemStack(Material.PAPER)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PendingWhitelist.DETAIL_MESSAGE_HEADER)
            val lore = buildList {
                val message = snapshot.lastMessage
                if (message.isNullOrBlank()) {
                    add(Messages.Gui.PendingWhitelist.DETAIL_NO_MESSAGE)
                } else {
                    message.chunked(30).forEach { add("§7$it") }
                }
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createAddItem(): ItemStack {
        val item = ItemStack(Material.EMERALD_BLOCK)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_ADD)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PendingWhitelist.BUTTON_ADD_LORE)
            if (!viewer.hasPermission("cloudly.whitelist")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createDismissItem(): ItemStack {
        val item = ItemStack(Material.REDSTONE_BLOCK)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_DISMISS)
            setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_DISMISS_LORE))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createBackItem(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_BACK)
            setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_BACK_LORE))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        when (event.slot) {
            SLOT_ADD -> handleAdd()
            SLOT_DISMISS -> handleDismiss()
            SLOT_BACK -> handleBack()
        }
    }

    private fun handleAdd() {
        if (!viewer.hasPermission("cloudly.whitelist")) {
            viewer.sendMessage(Messages.Commands.NO_PERMISSION)
            return
        }

        val snapshot = attemptService.getAttempt(attemptUuid)
        if (snapshot == null) {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            reopenParent = true
            viewer.closeInventory()
            return
        }

        if (whitelistService.isWhitelisted(snapshot.uuid)) {
            attemptService.removeAttempt(snapshot.uuid)
            viewer.sendMessage(Messages.Gui.PendingWhitelist.ENTRY_DISMISSED)
            reopenParent = true
            viewer.closeInventory()
            return
        }

        val added = whitelistService.addPlayer(snapshot.uuid, snapshot.username, viewer.uniqueId)
        if (added) {
            attemptService.removeAttempt(snapshot.uuid)
            viewer.sendMessage(Messages.Commands.Whitelist.playerAdded(snapshot.username))
            reopenParent = true
            viewer.closeInventory()
        } else {
            viewer.sendMessage(Messages.Commands.Whitelist.addFailed(snapshot.username))
            updateInventory()
        }
    }

    private fun handleDismiss() {
        attemptService.removeAttempt(attemptUuid)
        viewer.sendMessage(Messages.Gui.PendingWhitelist.ENTRY_DISMISSED)
        reopenParent = true
        viewer.closeInventory()
    }

    private fun handleBack() {
        reopenParent = true
        viewer.closeInventory()
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        if (reopenParent) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                plugin.getAdminGuiManager().openPendingWhitelistGui(viewer, parentPage)
            })
        }
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        this.inventory = null
    }
}
