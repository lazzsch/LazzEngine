package net.lazz.core.command

import net.lazz.core.command.annotation.CommandInfo
import net.lazz.core.service.module.util.CommandCleanupUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class CommandRegistry(private val plugin: Plugin) {

    private val service = CommandMapService.instance

    // 🔥 comandos registrados pelo módulo
    private val registeredCommands = mutableListOf<Command>()

    // ================= REGISTER =================

    fun register(info: CommandInfo, executor: CommandExecutor) {

        val command = object : Command(info.name.lowercase()) {

            init {
                aliases = info.aliases.map { it.lowercase() }

                permission = info.permission.ifEmpty { null }
                permissionMessage = info.permissionMessage.ifEmpty { "§cSem permissão." }
            }

            override fun execute(
                sender: CommandSender,
                label: String,
                args: Array<String>
            ): Boolean {

                permission?.let {
                    if (it.isNotEmpty() && !sender.hasPermission(it)) {
                        sender.sendMessage(permissionMessage ?: "§cSem permissão.")
                        return true
                    }
                }

                return executor.onCommand(sender, this, label, args)
            }
        }

        val fallback = plugin.name.lowercase()

        service.commandMap.register(fallback, command)

        registeredCommands.add(command)
    }

    // ================= UNREGISTER =================

    fun unregisterAll() {

        if (registeredCommands.isEmpty()) return

        try {
            CommandCleanupUtil.unregisterCommands(plugin, registeredCommands)
        } catch (_: Exception) {}

        registeredCommands.clear()
    }
}