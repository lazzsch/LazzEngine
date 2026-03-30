package net.lazz.core.service.module

import net.lazz.core.command.CommandRegistry
import net.lazz.core.service.service.ServiceRegistry
import org.bukkit.plugin.java.JavaPlugin

class ModuleContext(
    val plugin: JavaPlugin
) {

    val serviceRegistry = ServiceRegistry()
    val commandRegistry = CommandRegistry(plugin)

    fun cleanup() {
        commandRegistry.unregisterAll()
        serviceRegistry.clear()
    }
}