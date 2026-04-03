package net.lazz.core.services.smartiventory

import net.lazz.core.annotation.OnEnable
import net.lazz.core.annotation.Service
import net.lazz.core.services.smartiventory.holder.SmartInventoryHolder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.java.JavaPlugin

@Service
class SmartInventoryManager(private val plugin: JavaPlugin) : Listener {

    @OnEnable
    fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        plugin.logger.info("[SmartInventory] Listener registrado")
    }

    @EventHandler(priority = EventPriority.LOW)
    fun handleClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? SmartInventoryHolder ?: return
        holder.inventoryHandler.handleClick(event)
    }
}