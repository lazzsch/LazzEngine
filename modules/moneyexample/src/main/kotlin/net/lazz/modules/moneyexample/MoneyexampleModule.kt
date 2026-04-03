package net.lazz.modules.moneyexample

import net.lazz.core.services.module.AbstractModule
import org.bukkit.plugin.java.JavaPlugin

class MoneyexampleModule(
    plugin: JavaPlugin
) : AbstractModule(
    plugin = plugin,
    id = "moneyexample"
) {

    override fun onEnable() {
        plugin.logger.info("[Otherexample] Module activated")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Otherexample] Module deactivated")
    }
}