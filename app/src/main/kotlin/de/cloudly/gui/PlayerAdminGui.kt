package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.moderation.BanService
import de.cloudly.moderation.model.BanEntry
import de.cloudly.utils.TimeUtils
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
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
 * Detail view for a tracked player with moderation shortcuts.
 */
class PlayerAdminGui(
    private val plugin: CloudlyPaper,
    private val viewer: Player,
    private val targetUuid: UUID,
    private val targetName: String,
    private val parentPage: Int
) : Listener {

    private var inventory: Inventory? = null
    private var cleanedUp = false
    private var reopenDashboard = false
    private var transferring = false

    private val whitelistService = plugin.getWhitelistService()
    private val banService: BanService = plugin.getBanService()
    private val discordService = plugin.getDiscordService()

    private var activeBan: BanEntry? = null

    companion object {
        private const val INVENTORY_SIZE = 27
        private val BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

        private const val SLOT_PLAYER_INFO = 4
        private const val SLOT_DISCORD_INFO = 10
        private const val SLOT_UNLINK = 11
        private const val SLOT_FORCE_RELINK = 12
        private const val SLOT_KICK = 14
        private const val SLOT_TEMP_BAN = 15
        private const val SLOT_PERMA_BAN = 16
        private const val SLOT_BACK = 22
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        val title = Messages.Gui.PlayerAdmin.title(targetName)
        inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
        updateInventory()
        inventory?.let(viewer::openInventory)
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()

        val whitelistPlayer = whitelistService.getPlayer(targetUuid)
        if (whitelistPlayer == null) {
            viewer.sendMessage(Messages.Moderation.playerNotWhitelisted(targetName))
            reopenDashboard = true
            viewer.closeInventory()
            return
        }

        activeBan = banService.getActiveBan(targetUuid)

        fillBorder(inv)
        inv.setItem(SLOT_PLAYER_INFO, createPlayerInfoItem(whitelistPlayer))
        inv.setItem(SLOT_DISCORD_INFO, createDiscordInfoItem(whitelistPlayer))
        inv.setItem(SLOT_UNLINK, createUnlinkItem(whitelistPlayer))
        inv.setItem(SLOT_FORCE_RELINK, createForceRelinkItem())
        inv.setItem(SLOT_KICK, createKickItem())
        inv.setItem(SLOT_TEMP_BAN, createTempBanItem())
        inv.setItem(SLOT_PERMA_BAN, createPermaBanItem())
        inv.setItem(SLOT_BACK, createBackItem())
    }

    private fun fillBorder(inv: Inventory) {
        val borderItem = ItemStack(BORDER_MATERIAL).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§7") }
        }
        for (i in 0 until INVENTORY_SIZE step 9) {
            for (col in 0 until 9) {
                if (i == 0 || i == INVENTORY_SIZE - 9 || col == 0 || col == 8) {
                    inv.setItem(i + col, borderItem)
                }
            }
        }
    }

    private fun createPlayerInfoItem(player: WhitelistPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val offlinePlayer = Bukkit.getOfflinePlayer(player.uuid)
        item.itemMeta = (item.itemMeta as SkullMeta).apply {
            owningPlayer = offlinePlayer
            setDisplayName(player.username)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.PLAYER_INFO_TITLE)
            lore.add("§7")
            lore.add(Messages.Gui.PlayerAdmin.infoUuid(player.uuid.toString()))

            val addedByUuid = player.addedBy
            val addedByName = when {
                addedByUuid == null -> Messages.Gui.PlayerAdmin.UNKNOWN
                addedByUuid == UUID(0, 0) -> Messages.Gui.PlayerAdmin.CONSOLE
                else -> Bukkit.getOfflinePlayer(addedByUuid).name ?: Messages.Gui.PlayerAdmin.UNKNOWN
            }
            lore.add(Messages.Gui.PlayerAdmin.infoAddedBy(addedByName))
            val addedAtFormatted = DATE_FORMAT.format(player.addedAt)
            lore.add(Messages.Gui.PlayerAdmin.infoAddedOn(addedAtFormatted))
            player.reason?.takeIf { it.isNotBlank() }?.let { lore.add(Messages.Gui.PlayerAdmin.infoReason(it)) }

            lore.add("§7")
            val ban = activeBan
            if (ban == null) {
                lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_NONE)
            } else {
                lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_ACTIVE)
                if (ban.isPermanent) {
                    lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_PERMANENT)
                } else {
                    val until = ban.expiresAt?.let { TimeUtils.formatTimestamp(it) } ?: "§cUnbekannt"
                    lore.add(Messages.Gui.PlayerAdmin.banStatusUntil(until))
                }
            }

            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }

        if (activeBan != null) {
            item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
        }
        return item
    }

    private fun createDiscordInfoItem(player: WhitelistPlayer): ItemStack {
        val item = ItemStack(Material.BOOK)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.DISCORD_INFO_TITLE)
            val lore = mutableListOf<String>()
            val connection = player.discordConnection
            if (connection == null) {
                lore.add(Messages.Gui.PlayerAdmin.DISCORD_NOT_LINKED)
            } else {
                lore.add(Messages.Gui.PlayerAdmin.discordLinked(connection.discordUsername, connection.verified))
                val connectedAt = DATE_FORMAT.format(connection.connectedAt)
                lore.add("§7Verbunden seit: §f$connectedAt")
                connection.verifiedAt?.let {
                    val verifiedAt = DATE_FORMAT.format(it)
                    lore.add("§7Verifiziert am: §f$verifiedAt")
                }
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createUnlinkItem(player: WhitelistPlayer): ItemStack {
        val hasConnection = player.discordConnection != null
        val item = ItemStack(if (hasConnection) Material.SHEARS else Material.IRON_NUGGET)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_UNLINK)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.BUTTON_UNLINK_LORE)
            if (!hasConnection) {
                lore.add(Messages.Moderation.discordNotLinked(targetName))
            }
            if (!viewer.hasPermission("cloudly.discord.manage")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createForceRelinkItem(): ItemStack {
        val item = ItemStack(Material.ENDER_EYE)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_FORCE_RELINK)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.BUTTON_FORCE_RELINK_LORE)
            if (!viewer.hasPermission("cloudly.discord.manage")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createKickItem(): ItemStack {
        val item = ItemStack(Material.REDSTONE)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_KICK)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.BUTTON_KICK_LORE)
            if (!viewer.hasPermission("cloudly.moderation.kick")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createTempBanItem(): ItemStack {
        val item = ItemStack(Material.CLOCK)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_TEMP_BAN)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.BUTTON_TEMP_BAN_LORE)
            activeBan?.let { lore.add(Messages.Moderation.banAlreadyActive(targetName)) }
            if (!viewer.hasPermission("cloudly.moderation.tempban")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createPermaBanItem(): ItemStack {
        val item = ItemStack(Material.REDSTONE_BLOCK)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_PERMA_BAN)
            val lore = mutableListOf<String>()
            lore.add(Messages.Gui.PlayerAdmin.BUTTON_PERMA_BAN_LORE)
            activeBan?.let { lore.add(Messages.Moderation.banAlreadyActive(targetName)) }
            if (!viewer.hasPermission("cloudly.moderation.permaban")) {
                lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
            }
            setLore(lore)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun createBackItem(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_BACK)
            setLore(listOf(Messages.Gui.PlayerAdmin.BUTTON_BACK_LORE))
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true
        when (event.slot) {
            SLOT_UNLINK -> handleUnlink()
            SLOT_FORCE_RELINK -> handleForceRelink()
            SLOT_KICK -> handleKick()
            SLOT_TEMP_BAN -> handleTempBan()
            SLOT_PERMA_BAN -> handlePermaBan()
            SLOT_BACK -> handleBack()
        }
    }

    private fun handleUnlink() {
        if (!viewer.hasPermission("cloudly.discord.manage")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val whitelistPlayer = whitelistService.getPlayer(targetUuid)
        if (whitelistPlayer?.discordConnection == null) {
            viewer.sendMessage(Messages.Moderation.discordNotLinked(targetName))
            return
        }

        val success = whitelistService.clearPlayerDiscord(targetUuid, viewer.uniqueId)
        if (!success) {
            viewer.sendMessage(Messages.Moderation.discordUnlinkFailed(targetName))
            return
        }

        discordService.resetVerificationState(targetUuid)
        Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.sendMessage(Messages.Moderation.Target.RELINK_NOTICE)
        viewer.sendMessage(Messages.Moderation.discordUnlinkSuccess(targetName))
        updateInventory()
    }

    private fun handleForceRelink() {
        if (!viewer.hasPermission("cloudly.discord.manage")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val whitelistPlayer = whitelistService.getPlayer(targetUuid)
        if (whitelistPlayer == null) {
            viewer.sendMessage(Messages.Moderation.playerNotWhitelisted(targetName))
            reopenDashboard = true
            viewer.closeInventory()
            return
        }

        whitelistPlayer.discordConnection?.let {
            whitelistService.clearPlayerDiscord(targetUuid, viewer.uniqueId)
        }
        discordService.resetVerificationState(targetUuid)

        Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.let { target ->
            target.sendMessage(Messages.Moderation.Target.RELINK_NOTICE)
            plugin.getDiscordVerificationListener().restartVerification(target, force = true)
        }

        viewer.sendMessage(Messages.Moderation.discordForceRelink(targetName))
        updateInventory()
    }

    private fun handleKick() {
        if (!viewer.hasPermission("cloudly.moderation.kick")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Messages.Moderation.kickOffline(targetName))
            return
        }

        target.kickPlayer(Messages.Moderation.KICK_MESSAGE)
        viewer.sendMessage(Messages.Moderation.kickSuccess(targetName))
    }

    private fun handleTempBan() {
        if (!viewer.hasPermission("cloudly.moderation.tempban")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        if (activeBan != null) {
            viewer.sendMessage(Messages.Moderation.banAlreadyActive(targetName))
            return
        }

        transferring = true
        TempBanGui(
            plugin = plugin,
            viewer = viewer,
            targetUuid = targetUuid,
            targetName = targetName,
            parentPage = parentPage
        ) { page ->
            PlayerAdminGui(plugin, viewer, targetUuid, targetName, page).open()
        }.open()
    }

    private fun handlePermaBan() {
        if (!viewer.hasPermission("cloudly.moderation.permaban")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val existingBan = banService.getActiveBan(targetUuid)
        if (existingBan != null) {
            viewer.sendMessage(Messages.Moderation.banAlreadyActive(targetName))
            return
        }

        val result = banService.banPlayer(
            uuid = targetUuid,
            username = targetName,
            actor = viewer.uniqueId,
            duration = null,
            reason = Messages.Moderation.DEFAULT_BAN_REASON,
            deleteFromWhitelist = true
        )

        when (result) {
            is BanService.BanResult.Success -> {
                val kickMessage = Messages.Moderation.Target.permanentBan(result.entry.reason ?: Messages.Moderation.DEFAULT_BAN_REASON)
                Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.kickPlayer(kickMessage)
                viewer.sendMessage(Messages.Moderation.permaBanSuccess(targetName))
                reopenDashboard = true
                viewer.closeInventory()
            }
            is BanService.BanResult.AlreadyBanned -> viewer.sendMessage(Messages.Moderation.banAlreadyActive(targetName))
            BanService.BanResult.StorageError -> viewer.sendMessage(Messages.Moderation.BAN_STORAGE_ERROR)
        }
    }

    private fun handleBack() {
        reopenDashboard = true
        viewer.closeInventory()
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        cleanup()
        if (reopenDashboard && !transferring) {
            plugin.getAdminGuiManager().openAdminGui(viewer, parentPage)
        }
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
    }
}
