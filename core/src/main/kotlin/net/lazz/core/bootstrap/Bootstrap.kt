package net.lazz.core.bootstrap

import net.lazz.core.command.ModuleCommand
import net.lazz.core.service.module.manager.ModuleManager
import org.bukkit.plugin.java.JavaPlugin

class Bootstrap(
    private val plugin: JavaPlugin
) {

    private lateinit var moduleManager: ModuleManager

    fun init() {
        moduleManager = ModuleManager(plugin)
    }

    fun loadModules() {
        moduleManager.init()
        moduleManager.loadModules()
    }

    fun enableModules() {

        plugin.getCommand("wm")?.setExecutor(ModuleCommand(moduleManager))

        moduleManager.enableAll()

        plugin.logger.info("[Module] Sistema de módulos iniciado com sucesso")
    }

    fun shutdown() {
        moduleManager.disableAll()
    }
}