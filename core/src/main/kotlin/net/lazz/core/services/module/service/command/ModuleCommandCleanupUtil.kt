package net.lazz.core.services.module.service.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.Plugin

object ModuleCommandCleanupUtil {

    fun unregisterCommands(
        plugin: Plugin,
        moduleId: String,
        commands: Collection<Command>
    ) {

        val commandMap = getCommandMap()
        val knownCommands = getKnownCommands(commandMap)

        val toRemove = mutableListOf<String>()

        knownCommands.forEach { (key, cmd) ->

            try {

                val isModuleCommand =
                    commands.contains(cmd) ||
                            key.startsWith("${plugin.name.lowercase()}:$moduleId")

                if (!isModuleCommand) return@forEach

                cmd.unregister(commandMap)

                toRemove.add(key)

                cmd.aliases.forEach { alias ->
                    toRemove.add(alias.lowercase())
                    toRemove.add("${plugin.name.lowercase()}:$alias")
                }

                toRemove.add("${plugin.name.lowercase()}:${cmd.name.lowercase()}")

            } catch (_: Exception) {}
        }

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