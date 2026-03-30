package net.lazz.modules.money

import net.lazz.core.service.module.AbstractModule
import net.lazz.core.service.module.ModuleContext
import org.bukkit.plugin.java.JavaPlugin

class MoneyModule(
    plugin: JavaPlugin
) : AbstractModule(
    id = "money",
    context = ModuleContext(plugin)
) {

    override fun onEnable() {
        plugin.logger.info("[Money] Módulo ativado")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Money] Módulo desativado")
    }
}