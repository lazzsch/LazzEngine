package net.lazz.core.services.module.service.listener

import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class ModuleListenerRegistry(
    private val plugin: Plugin,
    private val moduleId: String
) {

    private val listeners = mutableSetOf<Listener>()

    fun register(listener: Listener) {

        if (listeners.contains(listener)) return

        Bukkit.getPluginManager().registerEvents(listener, plugin)

        listeners.add(listener)
    }

    fun unregisterAll() {

        if (listeners.isEmpty()) return

        listeners.forEach { listener ->
            try {
                HandlerList.unregisterAll(listener)
            } catch (ex: Exception) {
                plugin.logger.warning("[Module:$moduleId] Erro ao remover listener ${listener.javaClass.simpleName}")
                ex.printStackTrace()
            }
        }

        listeners.clear()
    }

    fun getAll(): Set<Listener> = listeners
}