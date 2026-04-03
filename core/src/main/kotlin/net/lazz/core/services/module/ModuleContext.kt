package net.lazz.core.services.module

import net.lazz.core.services.module.service.ModuleService
import net.lazz.core.services.module.service.command.ModuleCommandRegistry
import net.lazz.core.services.module.service.listener.ModuleListenerRegistry
import org.bukkit.plugin.java.JavaPlugin

class ModuleContext(
    val plugin: JavaPlugin,
    val moduleId: String
) {

    val namedServices = mutableMapOf<String, Any>()

    val commandRegistry = ModuleCommandRegistry(plugin, moduleId)
    val listenerRegistry = ModuleListenerRegistry(plugin, moduleId)

    fun cleanup() {

        commandRegistry.unregisterAll()
        listenerRegistry.unregisterAll()

        // lifecycle dos services
        namedServices.values.forEach { service ->
            if (service is ModuleService) {
                try {
                    service.onDisable()
                } catch (_: Exception) {}
            }
        }

        namedServices.clear()
    }
}