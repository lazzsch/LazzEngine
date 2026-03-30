package net.lazz.core.service.module.util

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.PluginCommand
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.Plugin

object CommandCleanupUtil {

    fun unregisterCommands(plugin: Plugin, commands: Collection<Command>) {

        val commandMap = getCommandMap()
        val knownCommands = getKnownCommands(commandMap)

        val toRemove = mutableListOf<String>()

        knownCommands.forEach { (key, cmd) ->

            try {

                // 🔥 remove APENAS comandos do módulo
                if (!commands.contains(cmd)) return@forEach

                cmd.unregister(commandMap)

                toRemove.add(key)

                // 🔥 remove aliases também
                cmd.aliases.forEach { alias ->
                    toRemove.add(alias.lowercase())
                    toRemove.add("${plugin.name.lowercase()}:$alias")
                }

                // 🔥 remove nome principal com namespace
                toRemove.add("${plugin.name.lowercase()}:${cmd.name.lowercase()}")

            } catch (_: Exception) {}
        }

        // 🔥 remove tudo coletado
        toRemove.distinct().forEach { knownCommands.remove(it) }

        syncCommands()
    }

    // ================= INTERNAL =================

    private fun getCommandMap(): SimpleCommandMap {
        val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        return field.get(Bukkit.getServer()) as SimpleCommandMap
    }

    @Suppress("UNCHECKED_CAST")
    private fun getKnownCommands(map: SimpleCommandMap): MutableMap<String, Command> {
        val field = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
        field.isAccessible = true
        return field.get(map) as MutableMap<String, Command>
    }

    private fun syncCommands() {
        try {
            val server = Bukkit.getServer()
            val method = server.javaClass.getMethod("syncCommands")
            method.invoke(server)

            Bukkit.getOnlinePlayers().forEach { it.updateCommands() }
        } catch (_: Exception) {}
    }
}