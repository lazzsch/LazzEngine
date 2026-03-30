package net.lazz.modules.testing

import net.lazz.core.service.module.AbstractModule
import net.lazz.core.service.module.ModuleContext
import org.bukkit.plugin.java.JavaPlugin

class TestingModule(
    plugin: JavaPlugin
) : AbstractModule(
    id = "testing",
    context = ModuleContext(plugin)
) {

    override fun onEnable() {
        plugin.logger.info("[Testing] Módulo ativado")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Testing] Módulo desativado")
    }
}