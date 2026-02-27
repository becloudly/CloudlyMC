package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.GuiTheme.applyFrame
import de.cloudly.gui.GuiTheme.applyGlow
import de.cloudly.gui.GuiTheme.applyRow
import de.cloudly.gui.GuiTheme.pane
import de.cloudly.moderation.BanService
import de.cloudly.utils.TimeUtils
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
import java.time.Duration
import java.util.UUID

/**
 * Secondary GUI that allows staff to pick a temporary ban duration.
 */
class TempBanGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val targetUuid: UUID,
    private val targetName: String,
    private val parentPage: Int,
    private val onReturn: (Int) -> Unit
) : Listener {

    private var inventory: Inventory? = null
    private var cleanedUp = false

    private val optionSlots = listOf(10, 12, 14, 16)

    private val options = listOf(
        BanOption("30 Minuten", Duration.ofMinutes(30), Material.SUGAR),
        BanOption("6 Stunden", Duration.ofHours(6), Material.BLAZE_POWDER),
        BanOption("1 Tag", Duration.ofDays(1), Material.CLOCK),
        BanOption("7 Tage", Duration.ofDays(7), Material.OBSIDIAN)
    )

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        val title = Messages.Gui.PlayerAdmin.tempBanTitle(targetName)
        inventory = Bukkit.createInventory(null, 27, title)
        updateInventory()
        viewer.openInventory(inventory!!)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()

        decorate(inv)

        options.forEachIndexed { index, option ->
            optionSlots.getOrNull(index)?.let { slot ->
                inv.setItem(slot, createOptionItem(option))
            }
        }

        inv.setItem(22, createBackItem())
    }

    private fun createOptionItem(option: BanOption): ItemStack {
        return ItemStack(option.material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.tempBanOptionLabel(option.label))
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.TEMP_BAN_OPTION_LORE)
                lore.add(Messages.Gui.PlayerAdmin.tempBanDuration(option.label))
                if (!viewer.hasPermission("cloudly.moderation.tempban")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, viewer.hasPermission("cloudly.moderation.tempban"))
        }
    }

    private fun createBackItem(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.TEMP_BAN_BACK)
            setLore(listOf(Messages.Gui.PlayerAdmin.TEMP_BAN_BACK_LORE))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        val clicked = event.slot

        if (clicked == 22) {
            viewer.closeInventory()
            return
        }

        optionSlots.indexOf(clicked).takeIf { it >= 0 }?.let { index ->
            if (index < options.size) {
                handleOption(options[index])
            }
        }
    }

    private fun decorate(inv: Inventory) {
        applyFrame(inv)

        applyRow(inv, 0, pane(Material.BLACK_STAINED_GLASS_PANE, "§0"), setOf(0, 8, 4))
        val optionColumns = buildSet {
            add(0)
            add(8)
            optionSlots.forEach { add(it % 9) }
        }
        applyRow(inv, 1, pane(Material.GRAY_STAINED_GLASS_PANE), optionColumns)
        applyRow(inv, 2, pane(Material.GRAY_STAINED_GLASS_PANE), setOf(0, 8, 4))

        inv.setItem(4, ItemStack(Material.CLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.tempBanHeader(targetName))
                setLore(listOf(Messages.Gui.PlayerAdmin.TEMP_BAN_HEADER_HINT))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        })

    }

    private fun handleOption(option: BanOption) {
        if (!viewer.hasPermission("cloudly.moderation.tempban")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val banService = plugin.getBanService()
        val result = banService.banPlayer(
            uuid = targetUuid,
            username = targetName,
            actor = viewer.uniqueId,
            duration = option.duration,
            reason = Messages.Moderation.DEFAULT_BAN_REASON,
            deleteFromWhitelist = false
        )

        when (result) {
            is BanService.BanResult.Success -> {
                val durationText = banService.describeDuration(option.duration)
                viewer.sendMessage(Messages.Moderation.tempBanSuccess(targetName, durationText))
                val expiresAt = result.entry.expiresAt ?: return
                val kickMessage = Messages.Moderation.Target.tempBan(
                    TimeUtils.formatTimestamp(expiresAt),
                    result.entry.reason ?: Messages.Moderation.DEFAULT_BAN_REASON
                )
                Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.kickPlayer(kickMessage)
                viewer.closeInventory()
            }
            is BanService.BanResult.AlreadyBanned -> {
                viewer.sendMessage(Messages.Moderation.banAlreadyActive(targetName))
                viewer.closeInventory()
            }
            BanService.BanResult.StorageError -> viewer.sendMessage(Messages.Moderation.BAN_STORAGE_ERROR)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        plugin.server.scheduler.runTask(plugin, Runnable {
            onReturn(parentPage)
        })
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        inventory = null
    }

    private data class BanOption(
        val label: String,
        val duration: Duration,
        val material: Material
    )
}
