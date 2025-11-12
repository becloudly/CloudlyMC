package de.cloudly.listeners

import de.cloudly.CloudlyPaper
import de.cloudly.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Prevents non-operator players from using or holding command blocks when protection is enabled.
 */
class AntiCommandBlockListener(private val plugin: CloudlyPaper) : Listener {

    private val blockedMaterials = EnumSet.of(
        Material.COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.COMMAND_BLOCK_MINECART
    )

    private val lastNotifications = ConcurrentHashMap<UUID, Long>()
    private val notificationCooldownMillis = 3000L

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!shouldHandle(player)) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        if (blockedMaterials.contains(clickedBlock.type)) {
            event.isCancelled = true
            player.sendMessage(Messages.Protections.CommandBlock.INTERACT_BLOCKED)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (!shouldHandle(player)) {
            return
        }

        if (blockedMaterials.contains(event.blockPlaced.type)) {
            event.isCancelled = true
            player.sendMessage(Messages.Protections.CommandBlock.INTERACT_BLOCKED)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        scheduleInventorySanitization(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        lastNotifications.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        scheduleInventorySanitization(player)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        scheduleInventorySanitization(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (!shouldHandle(player)) {
            return
        }

        if (blockedMaterials.contains(event.item.itemStack.type)) {
            scheduleInventorySanitization(player)
        }
    }

    fun shutdown() {
        lastNotifications.clear()
    }

    fun runInitialScan() {
        plugin.server.onlinePlayers.forEach { scheduleInventorySanitization(it) }
    }

    private fun scheduleInventorySanitization(player: Player) {
        if (!shouldHandle(player)) {
            return
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            sanitizeInventory(player)
        })
    }

    private fun sanitizeInventory(player: Player) {
        if (!player.isOnline) {
            return
        }

        if (!shouldHandle(player)) {
            return
        }

        val inventory = player.inventory
        var replaced = 0

        inventory.contents?.forEachIndexed { index, stack ->
            if (stack != null && blockedMaterials.contains(stack.type)) {
                replaced += stack.amount
                inventory.setItem(index, ItemStack(Material.STONE, stack.amount))
            }
        }

        val armor = inventory.armorContents
        var armorChanged = false
        armor.forEachIndexed { index, stack ->
            if (stack != null && blockedMaterials.contains(stack.type)) {
                replaced += stack.amount
                armor[index] = ItemStack(Material.STONE, stack.amount)
                armorChanged = true
            }
        }
        if (armorChanged) {
            inventory.armorContents = armor
        }

        val offHand = inventory.itemInOffHand
        if (blockedMaterials.contains(offHand.type)) {
            replaced += offHand.amount
            inventory.setItemInOffHand(ItemStack(Material.STONE, offHand.amount))
        }

        if (replaced > 0) {
            handleReplacement(player, replaced)
        }
    }

    private fun handleReplacement(player: Player, amount: Int) {
        val now = System.currentTimeMillis()
        val last = lastNotifications[player.uniqueId]
        if (last == null || now - last >= notificationCooldownMillis) {
            player.sendMessage(Messages.Protections.CommandBlock.itemsReplaced(amount))
            notifyAdmins(player, amount)
            lastNotifications[player.uniqueId] = now
        }
    }

    private fun notifyAdmins(player: Player, amount: Int) {
        if (!plugin.getConfigManager().getBoolean("protections.anti_command_block.notify_admins", true)) {
            return
        }

        val message = Messages.Protections.CommandBlock.notifyAdmins(player.name, amount)
        plugin.server.onlinePlayers
            .filter { it.isOp || it.hasPermission("cloudly.admin") }
            .forEach { it.sendMessage(message) }
        val plural = if (amount == 1) "command block" else "command blocks"
        plugin.logger.warning("[AntiCommandBlock] ${player.name} attempted to keep $amount $plural; replaced with stone")
    }

    private fun shouldHandle(player: Player): Boolean {
        if (player.isOp) {
            return false
        }
        return plugin.getConfigManager().getBoolean("protections.anti_command_block.enabled", true)
    }
}
