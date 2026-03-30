package net.lazz.core.command

import net.lazz.core.util.reflection.FieldAccessor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.SimpleCommandMap

class CommandMapService private constructor() {

    val commandMap: CommandMap
    val knownCommands: MutableMap<String, Command>

    init {
        val server = Bukkit.getServer()

        val field = server.javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        commandMap = field.get(server) as CommandMap

        knownCommands = FieldAccessor.getValue(
            SimpleCommandMap::class.java,
            "knownCommands",
            commandMap
        ) ?: throw IllegalStateException("knownCommands não encontrado")
    }

    companion object {
        val instance by lazy { CommandMapService() }
    }
}