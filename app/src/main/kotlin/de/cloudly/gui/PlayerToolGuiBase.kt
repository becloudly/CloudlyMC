package de.cloudly.gui

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import de.cloudly.gui.GuiTheme
import de.cloudly.gui.GuiTheme.applyGlow
import de.cloudly.moderation.BanService
import de.cloudly.moderation.model.BanEntry
import de.cloudly.utils.TimeUtils
import de.cloudly.whitelist.model.WhitelistPlayer
import org.bukkit.Bukkit
import org.bukkit.GameMode
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
 * Shared base for player-focused admin tools with category navigation.
 */
abstract class PlayerToolGuiBase(
    protected val plugin: CloudlyPaper,
    protected val viewer: Player,
    protected val targetUuid: UUID,
    initialTargetName: String,
    private val parentPage: Int,
    private val reopenSelf: (String, Int) -> Unit,
    private val reopenParent: (Int) -> Unit
) : Listener {

    private enum class Menu {
        ROOT,
        PLAYER_ACTIONS,
        PUNISHMENTS,
        DISCORD
    }

    companion object {
        private const val INVENTORY_SIZE = 54
        private const val SLOT_HEAD = 4
        private const val SLOT_BACK = 49

        private val ROOT_BUTTON_SLOTS = linkedMapOf(
            Menu.PLAYER_ACTIONS to 20,
            Menu.PUNISHMENTS to 22,
            Menu.DISCORD to 24
        )

        private val CONTENT_SLOTS = listOf(
            11, 12, 13, 14, 15,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42
        )

        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
    }

    private var inventory: Inventory? = null
    private var cleanedUp = false
    private var shouldReopenParent = false
    private var transferring = false
    private var currentMenu = Menu.ROOT
    private var currentTargetName = initialTargetName
    private var activeBan: BanEntry? = null

    private val whitelistService = plugin.getWhitelistService()
    private val banService: BanService = plugin.getBanService()
    private val discordService = plugin.getDiscordService()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open() {
        openMenu(Menu.ROOT)
    }

    private fun openMenu(menu: Menu) {
        currentMenu = menu
        val title = titleFor(menu, currentTargetName)
        val newInventory = Bukkit.createInventory(null, INVENTORY_SIZE, title)
        inventory = newInventory
        updateInventory()
        viewer.openInventory(newInventory)
    }

    private fun titleFor(menu: Menu, playerName: String): String {
        val base = Messages.Gui.PlayerAdmin.title(playerName)
        return when (menu) {
            Menu.ROOT -> base
            Menu.PLAYER_ACTIONS -> "$base §8• ${Messages.Gui.PlayerAdmin.CATEGORY_PLAYER_ACTIONS_TITLE}"
            Menu.PUNISHMENTS -> "$base §8• ${Messages.Gui.PlayerAdmin.CATEGORY_PUNISHMENTS_TITLE}"
            Menu.DISCORD -> "$base §8• ${Messages.Gui.PlayerAdmin.CATEGORY_DISCORD_TITLE}"
        }
    }

    private fun updateInventory() {
        val inv = inventory ?: return
        inv.clear()
        decorateInventory(inv)

        val whitelistPlayer = whitelistService.getPlayer(targetUuid)
        if (whitelistPlayer == null) {
            viewer.sendMessage(Messages.Moderation.playerNotWhitelisted(currentTargetName))
            shouldReopenParent = true
            viewer.closeInventory()
            return
        }

        currentTargetName = whitelistPlayer.username
        activeBan = banService.getActiveBan(targetUuid)

        when (currentMenu) {
            Menu.ROOT -> renderRoot(inv, whitelistPlayer)
            Menu.PLAYER_ACTIONS -> renderPlayerActions(inv, whitelistPlayer)
            Menu.PUNISHMENTS -> renderPunishments(inv, whitelistPlayer)
            Menu.DISCORD -> renderDiscord(inv, whitelistPlayer)
        }

        renderFooter(inv)
    }

    private fun decorateInventory(inv: Inventory) {
        when (currentMenu) {
            Menu.ROOT -> decorateRoot(inv)
            else -> decorateCategory(inv)
        }
    }

    private fun decorateRoot(inv: Inventory) {
        GuiTheme.applyFrame(inv)
        GuiTheme.applyRow(
            inv,
            rowIndex = 0,
            filler = GuiTheme.pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7"),
            skipColumns = setOf(SLOT_HEAD % 9)
        )
        val skipButtons = ROOT_BUTTON_SLOTS.values.map { it % 9 }.toSet()
        GuiTheme.applyRow(
            inv,
            rowIndex = 2,
            filler = GuiTheme.pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b"),
            skipColumns = skipButtons
        )
        GuiTheme.applyRow(
            inv,
            rowIndex = 5,
            filler = GuiTheme.standardFiller(),
            skipColumns = setOf(0, 8, SLOT_BACK % 9)
        )
    }

    private fun decorateCategory(inv: Inventory) {
        GuiTheme.applyFrame(inv)
        GuiTheme.applyRow(
            inv,
            rowIndex = 0,
            filler = GuiTheme.pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7"),
            skipColumns = setOf(SLOT_HEAD % 9)
        )
        val borderFiller = GuiTheme.pane(Material.BLACK_STAINED_GLASS_PANE, "§0")
        val innerColumns = (2..6).toSet()
        for (row in 1..4) {
            GuiTheme.applyRow(inv, row, borderFiller, innerColumns)
        }
        GuiTheme.applyRow(
            inv,
            rowIndex = 5,
            filler = GuiTheme.standardFiller(),
            skipColumns = setOf(0, 8, SLOT_BACK % 9)
        )
    }

    private fun renderRoot(inv: Inventory, whitelistPlayer: WhitelistPlayer) {
        inv.setItem(SLOT_HEAD, createPlayerInfoItem(whitelistPlayer))
        ROOT_BUTTON_SLOTS.forEach { (menu, slot) ->
            inv.setItem(slot, createRootButton(menu))
        }
    }

    private fun renderPlayerActions(inv: Inventory, whitelistPlayer: WhitelistPlayer) {
        inv.setItem(SLOT_HEAD, createPlayerInfoItem(whitelistPlayer))
        placeContent(
            inv,
            listOf(
                createInventoryViewItem(),
                createTeleportToItem(),
                createTeleportHereItem()
            )
        )
    }

    private fun renderPunishments(inv: Inventory, whitelistPlayer: WhitelistPlayer) {
        inv.setItem(SLOT_HEAD, createPlayerInfoItem(whitelistPlayer))
        placeContent(
            inv,
            listOf(
                createKickItem(),
                createTempBanItem(),
                createPermaBanItem(),
                createBanStatusCard()
            )
        )
    }

    private fun renderDiscord(inv: Inventory, whitelistPlayer: WhitelistPlayer) {
        inv.setItem(SLOT_HEAD, createPlayerInfoItem(whitelistPlayer))
        placeContent(
            inv,
            listOf(
                createDiscordInfoItem(whitelistPlayer),
                createDiscordDisconnectButton(whitelistPlayer)
            )
        )
    }

    private fun renderFooter(inv: Inventory) {
        inv.setItem(SLOT_BACK, createBackItem())
    }

    private fun placeContent(inv: Inventory, items: List<ItemStack?>) {
        CONTENT_SLOTS.forEachIndexed { index, slot ->
            items.getOrNull(index)?.let { inv.setItem(slot, it) }
        }
    }

    private fun createRootButton(menu: Menu): ItemStack {
        val (title, description, material) = when (menu) {
            Menu.PLAYER_ACTIONS -> Triple(
                Messages.Gui.PlayerAdmin.CATEGORY_PLAYER_ACTIONS_TITLE,
                Messages.Gui.PlayerAdmin.CATEGORY_PLAYER_ACTIONS_DESCRIPTION,
                Material.NAUTILUS_SHELL
            )
            Menu.PUNISHMENTS -> Triple(
                Messages.Gui.PlayerAdmin.CATEGORY_PUNISHMENTS_TITLE,
                Messages.Gui.PlayerAdmin.CATEGORY_PUNISHMENTS_DESCRIPTION,
                Material.REDSTONE_BLOCK
            )
            Menu.DISCORD -> Triple(
                Messages.Gui.PlayerAdmin.CATEGORY_DISCORD_TITLE,
                Messages.Gui.PlayerAdmin.CATEGORY_DISCORD_DESCRIPTION,
                Material.LAPIS_LAZULI
            )
            Menu.ROOT -> Triple("", "", Material.AIR)
        }

        return ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(title)
                val lore = mutableListOf<String>()
                lore.add(description)
                lore.add("§7")
                lore.add(Messages.Gui.PlayerAdmin.CATEGORY_OPEN_HINT)
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createBanStatusCard(): ItemStack {
        val ban = activeBan
        return ItemStack(Material.IRON_BARS).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BAN_STATUS_CARD_TITLE)
                val lore = mutableListOf<String>()
                if (ban == null) {
                    lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_NONE)
                } else {
                    lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_ACTIVE)
                    if (ban.isPermanent) {
                        lore.add(Messages.Gui.PlayerAdmin.BAN_STATUS_PERMANENT)
                    } else {
                        val until = ban.expiresAt?.let(TimeUtils::formatTimestamp)
                            ?: Messages.Gui.PlayerAdmin.UNKNOWN
                        lore.add(Messages.Gui.PlayerAdmin.banStatusUntil(until))
                    }
                    ban.reason?.takeIf { it.isNotBlank() }?.let {
                        lore.add(Messages.Gui.PlayerAdmin.infoReason(it))
                    }
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, ban != null)
        }
    }

    private fun createPlayerInfoItem(player: WhitelistPlayer): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        val skullMeta = skull.itemMeta as? SkullMeta ?: return skull

        val onlinePlayer = Bukkit.getPlayer(targetUuid)
        val isOnline = onlinePlayer?.isOnline == true
        val connection = player.discordConnection

        skullMeta.setDisplayName(Messages.Gui.PlayerAdmin.PLAYER_INFO_TITLE)
        skullMeta.owningPlayer = Bukkit.getOfflinePlayer(player.uuid)

        val lore = mutableListOf<String>()
        lore.add(Messages.Gui.PlayerAdmin.infoOnline(isOnline))
        onlinePlayer?.let { current ->
            val location = current.location
            lore.add(Messages.Gui.PlayerAdmin.infoWorld(current.world?.name))
            lore.add(
                Messages.Gui.PlayerAdmin.infoLocation(
                    location.blockX,
                    location.blockY,
                    location.blockZ
                )
            )
            lore.add(Messages.Gui.PlayerAdmin.infoGamemode(formatGamemode(current.gameMode)))
            lore.add(Messages.Gui.PlayerAdmin.infoPing(current.ping))
        }

        lore.add("§7")
        lore.add(Messages.Gui.PlayerAdmin.infoUuid(player.uuid.toString()))
        lore.add(Messages.Gui.PlayerAdmin.infoAddedBy(resolveAddedByName(player)))
        lore.add(Messages.Gui.PlayerAdmin.infoAddedOn(DATE_FORMAT.format(player.addedAt)))
        player.reason?.takeIf { it.isNotBlank() }?.let { lore.add(Messages.Gui.PlayerAdmin.infoReason(it)) }

        lore.add("§7")
        if (connection == null) {
            lore.add(Messages.Gui.PlayerAdmin.DISCORD_NOT_LINKED)
        } else {
            lore.add(Messages.Gui.PlayerAdmin.discordLinked(connection.discordUsername, connection.verified))
            lore.add(Messages.Gui.PlayerAdmin.discordLastSync(DATE_FORMAT.format(connection.connectedAt)))
            connection.verifiedAt?.let {
                lore.add(Messages.Gui.PlayerAdmin.discordVerifiedAt(DATE_FORMAT.format(it)))
            }
        }

        skullMeta.setLore(lore)
        skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        skull.itemMeta = skullMeta
        applyGlow(skull, activeBan != null || isOnline)
        return skull
    }

    private fun createDiscordInfoItem(player: WhitelistPlayer): ItemStack {
        val connection = player.discordConnection
        return ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.DISCORD_INFO_TITLE)
                val lore = mutableListOf<String>()
                if (connection == null) {
                    lore.add(Messages.Gui.PlayerAdmin.DISCORD_NOT_LINKED)
                } else {
                    lore.add(Messages.Gui.PlayerAdmin.discordLinked(connection.discordUsername, connection.verified))
                    lore.add(Messages.Gui.PlayerAdmin.discordLastSync(DATE_FORMAT.format(connection.connectedAt)))
                    connection.verifiedAt?.let {
                        lore.add(Messages.Gui.PlayerAdmin.discordVerifiedAt(DATE_FORMAT.format(it)))
                    }
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, connection?.verified == true)
        }
    }

    private fun createDiscordDisconnectButton(player: WhitelistPlayer): ItemStack {
        val connected = player.discordConnection != null
        val material = if (connected) Material.SHEARS else Material.IRON_NUGGET
        return ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_UNLINK)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_UNLINK_LORE)
                if (!connected) {
                    lore.add(Messages.Moderation.discordNotLinked(currentTargetName))
                }
                if (!viewer.hasPermission("cloudly.discord.manage")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
            applyGlow(this, connected)
        }
    }

    private fun createInventoryViewItem(): ItemStack {
        return ItemStack(Material.CHEST).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_VIEW_INVENTORY)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_VIEW_INVENTORY_LORE)
                if (!viewer.hasPermission("cloudly.admin.viewinventory")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                if (Bukkit.getPlayer(targetUuid)?.isOnline != true) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_TARGET_OFFLINE)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createTeleportToItem(): ItemStack {
        return ItemStack(Material.COMPASS).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_TELEPORT_TO_PLAYER)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_TELEPORT_TO_PLAYER_LORE)
                if (!viewer.hasPermission("cloudly.admin.teleport")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                if (Bukkit.getPlayer(targetUuid)?.isOnline != true) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_TARGET_OFFLINE)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createTeleportHereItem(): ItemStack {
        return ItemStack(Material.ENDER_PEARL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_TELEPORT_PLAYER_HERE)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_TELEPORT_PLAYER_HERE_LORE)
                if (!viewer.hasPermission("cloudly.admin.teleport")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                if (Bukkit.getPlayer(targetUuid)?.isOnline != true) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_TARGET_OFFLINE)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createKickItem(): ItemStack {
        return ItemStack(Material.REDSTONE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_KICK)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_KICK_LORE)
                if (!viewer.hasPermission("cloudly.moderation.kick")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createTempBanItem(): ItemStack {
        return ItemStack(Material.CLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_TEMP_BAN)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_TEMP_BAN_LORE)
                activeBan?.let { lore.add(Messages.Moderation.banAlreadyActive(currentTargetName)) }
                if (!viewer.hasPermission("cloudly.moderation.tempban")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createPermaBanItem(): ItemStack {
        return ItemStack(Material.REDSTONE_BLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(Messages.Gui.PlayerAdmin.BUTTON_PERMA_BAN)
                val lore = mutableListOf<String>()
                lore.add(Messages.Gui.PlayerAdmin.BUTTON_PERMA_BAN_LORE)
                activeBan?.let { lore.add(Messages.Moderation.banAlreadyActive(currentTargetName)) }
                if (!viewer.hasPermission("cloudly.moderation.permaban")) {
                    lore.add(Messages.Gui.PlayerAdmin.ACTION_NO_PERMISSION)
                }
                setLore(lore)
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    private fun createBackItem(): ItemStack {
        val isRoot = currentMenu == Menu.ROOT
        val displayName = if (isRoot) Messages.Gui.PlayerAdmin.BUTTON_BACK else Messages.Gui.PlayerAdmin.BUTTON_BACK_TO_MAIN
        val lore = if (isRoot) Messages.Gui.PlayerAdmin.BUTTON_BACK_LORE else Messages.Gui.PlayerAdmin.BUTTON_BACK_TO_MAIN_LORE
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(displayName)
                setLore(listOf(lore))
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory || event.whoClicked != viewer) return
        event.isCancelled = true

        val clicked = event.slot

        if (clicked == SLOT_BACK) {
            handleBack()
            return
        }

        if (currentMenu == Menu.ROOT) {
            ROOT_BUTTON_SLOTS.entries.firstOrNull { it.value == clicked }?.let { entry ->
                openMenu(entry.key)
            }
            return
        }

        if (clicked in CONTENT_SLOTS) {
            handleContentClick(clicked)
        }
    }

    private fun handleContentClick(slot: Int) {
        val index = CONTENT_SLOTS.indexOf(slot)
        if (index == -1) return
        when (currentMenu) {
            Menu.PLAYER_ACTIONS -> handlePlayerActionsClick(index)
            Menu.PUNISHMENTS -> handlePunishmentsClick(index)
            Menu.DISCORD -> handleDiscordClick(index)
            Menu.ROOT -> Unit
        }
    }

    private fun handlePlayerActionsClick(index: Int) {
        when (index) {
            0 -> handleViewInventory()
            1 -> handleTeleportToPlayer()
            2 -> handleTeleportPlayerHere()
        }
    }

    private fun handlePunishmentsClick(index: Int) {
        when (index) {
            0 -> handleKick()
            1 -> handleTempBan()
            2 -> handlePermaBan()
        }
    }

    private fun handleDiscordClick(index: Int) {
        if (index == 1) {
            handleUnlink()
        }
    }

    private fun handleViewInventory() {
        if (!viewer.hasPermission("cloudly.admin.viewinventory")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Messages.Moderation.teleportTargetOffline(currentTargetName))
            return
        }

        transferring = true
        val resolvedName = target.name ?: currentTargetName
        PlayerInventoryGui(
            plugin = plugin,
            viewer = viewer,
            target = target
        ) {
            reopenSelf(resolvedName, parentPage)
        }.open()
    }

    private fun handleTeleportToPlayer() {
        if (!viewer.hasPermission("cloudly.admin.teleport")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Messages.Moderation.teleportTargetOffline(currentTargetName))
            return
        }

        if (target.uniqueId == viewer.uniqueId) {
            viewer.sendMessage(Messages.Moderation.TELEPORT_SAME_PLAYER)
            return
        }

        viewer.teleport(target.location)
        viewer.sendMessage(Messages.Moderation.teleportToPlayerSuccess(target.name ?: currentTargetName))
    }

    private fun handleTeleportPlayerHere() {
        if (!viewer.hasPermission("cloudly.admin.teleport")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Messages.Moderation.teleportTargetOffline(currentTargetName))
            return
        }

        if (target.uniqueId == viewer.uniqueId) {
            viewer.sendMessage(Messages.Moderation.TELEPORT_SAME_PLAYER)
            return
        }

        target.teleport(viewer.location)
        viewer.sendMessage(Messages.Moderation.teleportPlayerHereSuccess(target.name ?: currentTargetName))
    }

    private fun handleKick() {
        if (!viewer.hasPermission("cloudly.moderation.kick")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Messages.Moderation.kickOffline(currentTargetName))
            return
        }

        target.kickPlayer(Messages.Moderation.KICK_MESSAGE)
        viewer.sendMessage(Messages.Moderation.kickSuccess(currentTargetName))
    }

    private fun handleTempBan() {
        if (!viewer.hasPermission("cloudly.moderation.tempban")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        if (activeBan != null) {
            viewer.sendMessage(Messages.Moderation.banAlreadyActive(currentTargetName))
            return
        }

        transferring = true
        TempBanGui(
            plugin = plugin,
            viewer = viewer,
            targetUuid = targetUuid,
            targetName = currentTargetName,
            parentPage = parentPage
        ) { page ->
            reopenSelf(currentTargetName, page)
        }.open()
    }

    private fun handlePermaBan() {
        if (!viewer.hasPermission("cloudly.moderation.permaban")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val existingBan = banService.getActiveBan(targetUuid)
        if (existingBan != null) {
            viewer.sendMessage(Messages.Moderation.banAlreadyActive(currentTargetName))
            return
        }

        when (val result = banService.banPlayer(
            uuid = targetUuid,
            username = currentTargetName,
            actor = viewer.uniqueId,
            duration = null,
            reason = Messages.Moderation.DEFAULT_BAN_REASON,
            deleteFromWhitelist = true
        )) {
            is BanService.BanResult.Success -> {
                val reason = result.entry.reason ?: Messages.Moderation.DEFAULT_BAN_REASON
                val kickMessage = Messages.Moderation.Target.permanentBan(reason)
                Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.kickPlayer(kickMessage)
                viewer.sendMessage(Messages.Moderation.permaBanSuccess(currentTargetName))
                shouldReopenParent = true
                viewer.closeInventory()
            }
            is BanService.BanResult.AlreadyBanned -> viewer.sendMessage(Messages.Moderation.banAlreadyActive(currentTargetName))
            BanService.BanResult.StorageError -> viewer.sendMessage(Messages.Moderation.BAN_STORAGE_ERROR)
        }
    }

    private fun handleUnlink() {
        if (!viewer.hasPermission("cloudly.discord.manage")) {
            viewer.sendMessage(Messages.Moderation.NO_PERMISSION)
            return
        }

        val whitelistPlayer = whitelistService.getPlayer(targetUuid)
        if (whitelistPlayer?.discordConnection == null) {
            viewer.sendMessage(Messages.Moderation.discordNotLinked(currentTargetName))
            return
        }

        val success = whitelistService.clearPlayerDiscord(targetUuid, viewer.uniqueId)
        if (!success) {
            viewer.sendMessage(Messages.Moderation.discordUnlinkFailed(currentTargetName))
            return
        }

        discordService.resetVerificationState(targetUuid)
        Bukkit.getPlayer(targetUuid)?.takeIf { it.isOnline }?.sendMessage(Messages.Moderation.Target.RELINK_NOTICE)
        viewer.sendMessage(Messages.Moderation.discordUnlinkSuccess(currentTargetName))
        updateInventory()
    }

    private fun handleBack() {
        if (currentMenu != Menu.ROOT) {
            openMenu(Menu.ROOT)
            return
        }
        shouldReopenParent = true
        viewer.closeInventory()
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory || event.player != viewer) return
        val reopen = shouldReopenParent && !transferring
        shouldReopenParent = false
        cleanup()
        if (reopen) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                reopenParent(parentPage)
            })
        }
    }

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        transferring = false
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        inventory = null
    }

    private fun formatGamemode(mode: GameMode?): String {
        val raw = mode?.name ?: return Messages.Gui.PlayerAdmin.UNKNOWN
        return raw.lowercase()
            .split('_')
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun resolveAddedByName(player: WhitelistPlayer): String {
        val addedBy = player.addedBy ?: return Messages.Gui.PlayerAdmin.UNKNOWN
        if (addedBy == UUID(0, 0)) return Messages.Gui.PlayerAdmin.CONSOLE
        return Bukkit.getOfflinePlayer(addedBy).name ?: Messages.Gui.PlayerAdmin.UNKNOWN
    }
}
