package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.PendingWhitelistDetailGui.Companion.DATE_FORMAT
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
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

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
    private val attemptsPerPage = 28
    private var attempts: List<WhitelistAttemptService.AttemptSnapshot> = emptyList()
    private var cleanedUp = false

    companion object {
        private const val INVENTORY_SIZE = 54
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val NAVIGATION_MATERIAL = Material.ARROW
        private val INFO_MATERIAL = Material.BOOK
        private val REFRESH_MATERIAL = Material.EMERALD
        private val BACK_MATERIAL = Material.BARRIER

        private val layout = GuiLayout(6).apply {
            setSlot("prev_page", 5, 0)
            setSlot("back", 5, 2)
            setSlot("info", 5, 4)
            setSlot("refresh", 5, 6)
            setSlot("next_page", 5, 8)
        }
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

        addBorderDecoration(inv)
        addAttemptItems(inv)
        addNavigationItems(inv)
    }

    private fun addBorderDecoration(inv: Inventory) {
        val borderItem = ItemStack(BORDER_MATERIAL).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§7") }
        }

        for (i in 0..8) {
            inv.setItem(i, borderItem)
        }
        for (i in 45..53) {
            inv.setItem(i, borderItem)
        }
        for (row in 1..4) {
            inv.setItem(row * 9, borderItem)
            inv.setItem(row * 9 + 8, borderItem)
        }
    }

    private fun addAttemptItems(inv: Inventory) {
        val startIndex = currentPage * attemptsPerPage
        val endIndex = minOf(startIndex + attemptsPerPage, attempts.size)

        for (i in startIndex until endIndex) {
            val attempt = attempts[i]
            val slotIndex = getAttemptSlotIndex(i - startIndex)
            inv.setItem(slotIndex, createAttemptItem(attempt))
        }
    }

    private fun getAttemptSlotIndex(relativeIndex: Int): Int {
        val row = relativeIndex / 7
        val col = relativeIndex % 7
        return (row + 1) * 9 + col + 1
    }

    private fun createAttemptItem(attempt: WhitelistAttemptService.AttemptSnapshot): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val offlinePlayer = Bukkit.getOfflinePlayer(attempt.uuid)

        item.itemMeta = (item.itemMeta as SkullMeta).apply {
            owningPlayer = offlinePlayer
            setDisplayName(Messages.Gui.PendingWhitelist.playerLabel(attempt.username))

            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PendingWhitelist.infoUuid(attempt.uuid.toString()))
            lore.add(Messages.Gui.PendingWhitelist.infoAttempts(attempt.attemptCount))
            val lastAttempt = DATE_FORMAT.format(attempt.lastAttempt)
            lore.add(Messages.Gui.PendingWhitelist.infoLastAttempt(lastAttempt))
            val firstAttempt = DATE_FORMAT.format(attempt.firstAttempt)
            lore.add(Messages.Gui.PendingWhitelist.infoFirstAttempt(firstAttempt))
            attempt.lastAddress?.let {
                lore.add(Messages.Gui.PendingWhitelist.infoAddress(it))
            } ?: lore.add(Messages.Gui.PendingWhitelist.DETAIL_NO_ADDRESS)
            lore.add("§7")
            lore.add(Messages.Gui.PendingWhitelist.ACTIONS_TITLE)
            lore.add(Messages.Gui.PendingWhitelist.ACTION_LEFT_CLICK)
            lore.add(Messages.Gui.PendingWhitelist.ACTION_RIGHT_CLICK)
            setLore(lore)
        }

        return item
    }

    private fun addNavigationItems(inv: Inventory) {
        val totalPages = getTotalPages()

        if (currentPage > 0) {
            val prevItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Admin.PREVIOUS_PAGE)
                    setLore(listOf(Messages.Gui.Admin.previousPageLore(currentPage)))
                }
            }
            inv.setItem(layout.getSlot("prev_page"), prevItem)
        }

        val backItem = ItemStack(BACK_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.BUTTON_BACK)
                setLore(listOf(Messages.Gui.PendingWhitelist.BUTTON_BACK_LORE))
            }
        }
        inv.setItem(layout.getSlot("back"), backItem)

        val infoItem = ItemStack(INFO_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PendingWhitelist.title(attempts.size))
                setLore(listOf(Messages.Gui.PendingWhitelist.infoAttempts(attempts.size)))
            }
        }
        inv.setItem(layout.getSlot("info"), infoItem)

        val refreshItem = ItemStack(REFRESH_MATERIAL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.Admin.REFRESH_BUTTON)
                setLore(listOf(Messages.Gui.Admin.REFRESH_LORE))
            }
        }
        inv.setItem(layout.getSlot("refresh"), refreshItem)

        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(NAVIGATION_MATERIAL).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(Messages.Gui.Admin.NEXT_PAGE)
                    setLore(listOf(Messages.Gui.Admin.nextPageLore(currentPage + 2)))
                }
            }
            inv.setItem(layout.getSlot("next_page"), nextItem)
        }
    }

    private fun getTotalPages(): Int {
        return if (attempts.isEmpty()) 1 else (attempts.size + attemptsPerPage - 1) / attemptsPerPage
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        val slot = event.slot
        val clickedItem = event.currentItem ?: return

        when (slot) {
            layout.getSlot("prev_page") -> if (currentPage > 0) {
                currentPage--
                updateInventory()
            }
            layout.getSlot("next_page") -> if (currentPage < getTotalPages() - 1) {
                currentPage++
                updateInventory()
            }
            layout.getSlot("refresh") -> {
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
                    viewer.sendMessage(Messages.Gui.Admin.REFRESHED)
                }
            }
            layout.getSlot("info") -> Unit
            layout.getSlot("back") -> {
                viewer.closeInventory()
                plugin.server.scheduler.runTask(plugin, Runnable {
                    plugin.getAdminGuiManager().openAdminGui(viewer)
                })
            }
            else -> {
                if (clickedItem.type == Material.PLAYER_HEAD) {
                    handleAttemptItemClick(event.click, clickedItem)
                }
            }
        }
    }

    private fun handleAttemptItemClick(click: ClickType, item: ItemStack) {
        val meta = item.itemMeta as? SkullMeta ?: return
        val owningPlayer = meta.owningPlayer ?: return
        val attemptUuid = owningPlayer.uniqueId
        val attempt = attempts.firstOrNull { it.uuid == attemptUuid }

        if (attempt == null) {
            viewer.sendMessage(Messages.Gui.PendingWhitelist.NO_ATTEMPTS)
            reloadOrClose()
            return
        }

        when (click) {
            ClickType.LEFT, ClickType.SHIFT_LEFT -> {
                PendingWhitelistDetailGui(plugin, viewer, attemptUuid, currentPage).open()
            }
            ClickType.RIGHT, ClickType.SHIFT_RIGHT -> {
                if (!viewer.hasPermission("cloudly.whitelist")) {
                    viewer.sendMessage(Messages.Commands.NO_PERMISSION)
                    return
                }
                attemptService.removeAttempt(attemptUuid)
                viewer.sendMessage(Messages.Gui.PendingWhitelist.ENTRY_DISMISSED)
                reloadOrClose()
            }
            else -> Unit
        }
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
