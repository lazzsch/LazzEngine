package net.lazz.modules.teste

import net.lazz.core.service.module.AbstractModule
import net.lazz.core.service.module.ModuleContext
import org.bukkit.plugin.java.JavaPlugin

class TesteModule(
    plugin: JavaPlugin
) : AbstractModule(
    id = "teste",
    context = ModuleContext(plugin)
) {

    override fun onEnable() {
        plugin.logger.info("[Teste] Módulo ativado")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Teste] Módulo desativado")
    }
}