package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.GuiTheme.applyFrame
import de.cloudly.gui.GuiTheme.applyRow
import de.cloudly.gui.GuiTheme.pane
import de.cloudly.gui.GuiTheme.standardFiller
import de.cloudly.whitelist.attempts.WhitelistAttemptService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.format.DateTimeFormatter

/**
 * Lists players who attempted to join without being whitelisted.
 */
class PendingWhitelistGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val initialPage: Int = 0
) : Listener {

    private val attemptService: WhitelistAttemptService = plugin.getWhitelistAttemptService()

    private var inventory: Inventory? = null
    private var currentPage = initialPage
    private var attempts: List<WhitelistAttemptService.AttemptSnapshot> = emptyList()
    private var cleanedUp = false
    private val displayedAttempts = mutableMapOf<Int, WhitelistAttemptService.AttemptSnapshot>()

    companion object {
        private const val INVENTORY_SIZE = 54
        private val ATTEMPT_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )

        private const val SLOT_INFO = 4
        private const val SLOT_PREV = 45
        private const val SLOT_BACK = 48
        private const val SLOT_REFRESH = 49
        private const val SLOT_NEXT = 53
        private val DATE_FORMAT: DateTimeFormatter = PendingWhitelistDetailGui.DATE_FORMAT
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        loadAttempts()
        if (attempts.isEmpty()) {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            cleanup()
            plugin.getAdminGuiManager().unregisterPendingGui(viewer.uniqueId)
            return
        }
        createInventory()
        updateInventory()
        viewer.openInventory(requireNotNull(inventory))
    }

    private fun loadAttempts() {
        attempts = attemptService.getAttempts()
    }

    private fun createInventory() {
        val title = Messages.Gui.PendingWhitelist.title(attempts.size)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        displayedAttempts.clear()

        decorateInventory(inv)
        renderAttempts(inv)
        renderFooter(inv)
    }

    private fun decorateInventory(inv: Inventory) {
        applyFrame(inv)

        val topSkip = setOf(0, 8, SLOT_INFO % 9)
        applyRow(inv, 0, pane(Material.BLACK_STAINED_GLASS_PANE, "§0"), topSkip)

        val midSkip = buildSet {
            add(0)
            add(8)
            ATTEMPT_SLOTS.forEach { add(it % 9) }
        }
        applyRow(inv, 1, pane(Material.GRAY_STAINED_GLASS_PANE), midSkip)
        applyRow(inv, 2, pane(Material.GRAY_STAINED_GLASS_PANE), midSkip)
        applyRow(inv, 3, pane(Material.GRAY_STAINED_GLASS_PANE), midSkip)

        inv.setItem(SLOT_INFO, createInfoItem())
    }

    private fun renderAttempts(inv: Inventory) {
        val pageSize = ATTEMPT_SLOTS.size
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, attempts.size)

        ATTEMPT_SLOTS.forEach { inv.setItem(it, null) }

        var slotIndex = 0
        for (i in startIndex until endIndex) {
            val slot = ATTEMPT_SLOTS.getOrNull(slotIndex) ?: break
            val attempt = attempts[i]
            inv.setItem(slot, createAttemptItem(attempt))
            displayedAttempts[slot] = attempt
            slotIndex++
        }
    }

    private fun renderFooter(inv: Inventory) {
        inv.setItem(SLOT_PREV, createPrevItem())
        inv.setItem(SLOT_BACK, createBackItem())
        inv.setItem(SLOT_REFRESH, createRefreshItem())
        inv.setItem(SLOT_NEXT, createNextItem())

        // Fill remaining bottom row slots that are empty
        for (slot in 45..53) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, standardFiller())
            }
        }
    }

    private fun createInfoItem(): ItemStack {
        val totalPages = getTotalPages()
        val lore = mutableListOf<String>()
        lore.add(Messages.Gui.PendingWhitelist.infoAttempts(attempts.size))
        lore.add(Messages.Gui.PendingWhitelist.infoPage(currentPage + 1, totalPages))

        return ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.title(attempts.size))
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createPrevItem(): ItemStack? {
        if (currentPage == 0) return null
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.PREVIOUS_PAGE)
                setLore(listOf(Messages.Gui.Admin.previousPageLore(currentPage)))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createNextItem(): ItemStack? {
        if (currentPage >= getTotalPages() - 1) return null
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.NEXT_PAGE)
                setLore(listOf(Messages.Gui.Admin.nextPageLore(currentPage + 2)))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createBackItem(): ItemStack {
        return ItemStack(Material.BARRIER).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_BACK)
                setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_BACK_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createRefreshItem(): ItemStack {
        return ItemStack(Material.EMERALD).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.REFRESH_BUTTON)
                setLore(listOf(Messages.Gui.Admin.REFRESH_LORE))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createAttemptItem(attempt: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        val offlinePlayer = Bukkit.getOfflinePlayer(attempt.uuid)
        return ItemStack(Material.PLAYER_HEAD).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                owningPlayer = offlinePlayer
                setDisplayName(Messages.Gui.PendingWhitelist.playerLabel(attempt.username))

                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PendingWhitelist.infoUuid(attempt.uuid.toString()))
                val lastAttempt = DATE_FORMAT.format(attempt.lastAttempt)
                lore.add(Messages.Gui.PendingWhitelist.infoLastAttempt(lastAttempt))
                val firstAttempt = DATE_FORMAT.format(attempt.firstAttempt)
                lore.add(Messages.Gui.PendingWhitelist.infoFirstAttempt(firstAttempt))
                lore.add(Messages.Gui.PendingWhitelist.infoAttempts(attempt.attemptCount))
                val address = attempt.lastAddress
                lore.add(
                    address?.let { Messages.Gui.PendingWhitelist.infoAddress(it) }
                        ?: Messages.Gui.PendingWhitelist.DETAIL_NO_ADDRESS
                )
                lore.add("§7")
                lore.add(Messages.Gui.PendingWhitelist.ACTIONS_TITLE)
                lore.add(Messages.Gui.PendingWhitelist.ACTION_LEFT_CLICK)
                lore.add(Messages.Gui.PendingWhitelist.ACTION_RIGHT_CLICK)
                setLore(lore)
            }
        }
    }

    private fun getTotalPages(): Int {
        val size = ATTEMPT_SLOTS.size
        return if (attempts.isEmpty()) 1 else (attempts.size + size - 1) / size
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        val slot = event.slot
        when (slot) {
            SLOT_PREV -> {
                if (currentPage > 0) {
                    currentPage--
                    updateInventory()
                }
            }
            SLOT_NEXT -> {
                if (currentPage < getTotalPages() - 1) {
                    currentPage++
                    updateInventory()
                }
            }
            SLOT_REFRESH -> handleRefresh()
            SLOT_BACK -> {
                viewer.closeInventory()
                plugin.server.scheduler.runTask(plugin, Runnable {
                    plugin.getAdminGuiManager().openAdminGui(viewer)
                })
            }
            SLOT_INFO -> Unit
            else -> handleAttemptSlotClick(slot, event.click)
        }
    }

    private fun handleAttemptSlotClick(slot: Int, click: ClickType) {
        val attempt = displayedAttempts[slot] ?: return

        when (click) {
            ClickType.LEFT, ClickType.SHIFT_LEFT -> {
                PendingWhitelistDetailGui(plugin, viewer, attempt.uuid, currentPage).open()
            }
            ClickType.RIGHT, ClickType.SHIFT_RIGHT -> {
                if (!viewer.hasPermission("cloudly.whitelist")) {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                    return
                }
                attemptService.removeAttempt(attempt.uuid)
                viewer.sendMessage(Messages.Gui.PendingWhitelist.ENTRY_DISMISSED)
                reloadOrClose()
            }
            else -> Unit
        }
    }

    private fun handleRefresh() {
        loadAttempts()
        if (attempts.isEmpty()) {
            viewer.closeInventory()
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            return
        }

        val totalPages = getTotalPages()
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }
        updateInventory()
        viewer.sendMessage(Messages.Gui.Admin.REFRESHED)
    }

    private fun reloadOrClose() {
        loadAttempts()
        if (attempts.isEmpty()) {
            viewer.closeInventory()
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
        } else {
            val totalPages = getTotalPages()
            if (currentPage >= totalPages) {
                currentPage = maxOf(0, totalPages - 1)
            }
            updateInventory()
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        plugin.getAdminGuiManager().unregisterPendingGui(viewer.uniqueId)
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        inventory = null
    }
}
