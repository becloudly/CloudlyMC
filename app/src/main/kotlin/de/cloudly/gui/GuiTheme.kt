package de.cloudly.gui

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/**
 * Shared helpers for building consistent admin GUI visuals.
 */
object GuiTheme {

    fun pane(material: Material, label: String = "§7"): ItemStack = ItemStack(material).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName(label)
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
    }

    fun standardFiller(): ItemStack = pane(Material.GRAY_STAINED_GLASS_PANE)

    fun accentFiller(): ItemStack = pane(Material.BLACK_STAINED_GLASS_PANE, "§0")

    fun applyFrame(inv: Inventory, filler: ItemStack = standardFiller(), includeTop: Boolean = true) {
        val rows = inv.size / 9
        for (slot in 0 until inv.size) {
            val row = slot / 9
            val col = slot % 9
            val shouldFill = when {
                col == 0 || col == 8 -> true
                row == rows - 1 -> true
                includeTop && row == 0 -> true
                else -> false
            }
            if (shouldFill) {
                inv.setItem(slot, filler.clone())
            }
        }
    }

    fun applyRow(inv: Inventory, rowIndex: Int, filler: ItemStack, skipColumns: Set<Int> = emptySet()) {
        val base = rowIndex * 9
        if (base >= inv.size) return
        for (col in 0..8) {
            if (skipColumns.contains(col)) continue
            val slot = base + col
            if (slot >= inv.size) return
            inv.setItem(slot, filler.clone())
        }
    }

    fun applyGlow(item: ItemStack, glow: Boolean) {
        if (!glow) return
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
        item.itemMeta = item.itemMeta?.apply { addItemFlags(ItemFlag.HIDE_ENCHANTS) }
    }
}
