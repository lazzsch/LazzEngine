package net.lazz.core.services.smartiventory.holder

import net.lazz.core.services.smartiventory.SmartInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class SmartInventoryHolder(val name: String) : InventoryHolder {

    lateinit var inventoryHandler: SmartInventory

    override fun getInventory(): Inventory {
        return inventoryHandler.getInventory()
    }
}