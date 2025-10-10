package de.cloudly.gui

/**
 * Flexible layout system for GUI slot positioning.
 * Allows defining slot positions using row and column coordinates instead of hard-coded slot numbers.
 * 
 * @param rows The number of rows in the inventory (typically 1-6 for Minecraft inventories)
 */
class GuiLayout(val rows: Int) {
    private val slots = mutableMapOf<String, Int>()
    
    init {
        require(rows in 1..6) { "Inventory rows must be between 1 and 6, got: $rows" }
    }
    
    /**
     * Sets a named slot position using row and column coordinates.
     * Slot numbers are calculated as: row * 9 + col
     * 
     * @param name The name identifier for this slot
     * @param row The row number (0-indexed, 0 = top row)
     * @param col The column number (0-indexed, 0 = leftmost column)
     * @throws IllegalArgumentException if row or col are out of valid range
     */
    fun setSlot(name: String, row: Int, col: Int) {
        require(row in 0 until rows) { "Row must be between 0 and ${rows - 1}, got: $row" }
        require(col in 0..8) { "Column must be between 0 and 8, got: $col" }
        slots[name] = row * 9 + col
    }
    
    /**
     * Gets the slot number for a named slot.
     * 
     * @param name The name identifier for the slot
     * @return The calculated slot number
     * @throws IllegalArgumentException if the slot name is not found
     */
    fun getSlot(name: String): Int {
        return slots[name] ?: throw IllegalArgumentException("Unknown slot name: $name")
    }
    
    /**
     * Checks if a slot name has been defined.
     * 
     * @param name The name identifier to check
     * @return true if the slot exists, false otherwise
     */
    fun hasSlot(name: String): Boolean = slots.containsKey(name)
    
    /**
     * Gets the total number of slots in the inventory.
     * 
     * @return The total slot count (rows * 9)
     */
    fun getTotalSlots(): Int = rows * 9
}
