package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.GuiTheme.applyFrame
import de.cloudly.gui.GuiTheme.applyGlow
import de.cloudly.gui.GuiTheme.applyRow
import de.cloudly.gui.GuiTheme.pane
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

        private const val SLOT_PLAYER = 11
        private const val SLOT_META = 13
        private const val SLOT_MESSAGE = 15
        private const val SLOT_ADD = 21
        private const val SLOT_DISMISS = 23
        private const val SLOT_BACK = 26
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

        decorateInventory(inv)
        inv.setItem(SLOT_PLAYER, createPlayerCard(snapshot))
        inv.setItem(SLOT_META, createMetaCard(snapshot))
        inv.setItem(SLOT_MESSAGE, createMessageCard(snapshot))
        inv.setItem(SLOT_ADD, createAddButton())
        inv.setItem(SLOT_DISMISS, createDismissButton())
        inv.setItem(SLOT_BACK, createBackButton())
    }

    private fun decorateInventory(inv: Inventory) {
        applyFrame(inv)

        val skipRow = setOf(0, 8, SLOT_PLAYER % 9, SLOT_META % 9, SLOT_MESSAGE % 9)
        applyRow(inv, 0, pane(Material.BLACK_STAINED_GLASS_PANE, "§0"), skipRow)
        applyRow(inv, 1, pane(Material.GRAY_STAINED_GLASS_PANE), buildSet {
            add(0)
            add(8)
            add(SLOT_ADD % 9)
            add(SLOT_DISMISS % 9)
            add(SLOT_BACK % 9)
        })
        applyRow(inv, 2, pane(Material.GRAY_STAINED_GLASS_PANE), setOf(0, 8, SLOT_BACK % 9))
    }

    private fun createPlayerCard(snapshot: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        return ItemStack(Material.PLAYER_HEAD).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                owningPlayer = Bukkit.getOfflinePlayer(snapshot.uuid)
                setDisplayName(Messages.Gui.PendingWhitelist.playerLabel(snapshot.username))
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PendingWhitelist.infoUuid(snapshot.uuid.toString()))
                lore.add(Messages.Gui.PendingWhitelist.infoAttempts(snapshot.attemptCount))
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createMetaCard(snapshot: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        return ItemStack(Material.WRITABLE_BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.DETAIL_META_TITLE)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PendingWhitelist.infoFirstAttempt(DATE_FORMAT.format(snapshot.firstAttempt)))
                lore.add(Messages.Gui.PendingWhitelist.infoLastAttempt(DATE_FORMAT.format(snapshot.lastAttempt)))
                val address = snapshot.lastAddress
                lore.add(
                    address?.let { Messages.Gui.PendingWhitelist.infoAddress(it) }
                        ?: Messages.Gui.PendingWhitelist.DETAIL_NO_ADDRESS
                )
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createMessageCard(snapshot: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        return ItemStack(Material.PAPER).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.DETAIL_MESSAGE_HEADER)
                val lore = buildList {
                    val message = snapshot.lastMessage
                    if (message.isNullOrBlank()) {
                        add(Messages.Gui.PendingWhitelist.DETAIL_NO_MESSAGE)
                    } else {
                        message.chunked(32).forEach { add("§7$it") }
                    }
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createAddButton(): ItemStack {
        return ItemStack(Material.EMERALD_BLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_ADD)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PendingWhitelist.BUTTON_ADD_LORE)
                if (!viewer.hasPermission("cloudly.whitelist")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, viewer.hasPermission("cloudly.whitelist"))
        }
    }

    private fun createDismissButton(): ItemStack {
        return ItemStack(Material.REDSTONE_BLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_DISMISS)
                setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_DISMISS_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createBackButton(): ItemStack {
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_BACK)
                setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_BACK_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
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

        val snapshot = attemptService.getAttempt(attemptUuid) ?: run {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            reopenParent = true
            viewer.closeInventory()
            return
        }

        val result = whitelistService.addPlayer(snapshot.uuid, snapshot.username, viewer.uniqueId)
        if (result) {
            attemptService.removeAttempt(attemptUuid)
            viewer.sendMessage(Messages.Gui.PendingWhitelist.buttonAddSuccess(snapshot.username))
            reopenParent = true
            viewer.closeInventory()
        } else {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.buttonAddFailed(snapshot.username))
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
        inventory = null
    }
}
