package net.lazz.modules.otherexample

import net.lazz.core.services.module.AbstractModule
import net.lazz.core.services.module.service.annotation.Depend
import org.bukkit.plugin.java.JavaPlugin

@Depend("MoneyexampleService")
class OtherexampleModule(
    plugin: JavaPlugin
) : AbstractModule(
    plugin = plugin,
    id = "otherexample"
) {

    override fun onEnable() {
        plugin.logger.info("[Otherexample] Module activated")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Otherexample] Module deactivated")
    }
}