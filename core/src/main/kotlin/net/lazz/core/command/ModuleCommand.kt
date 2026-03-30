package net.lazz.core.command

import net.lazz.core.service.module.manager.ModuleManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ModuleCommand(
    private val moduleManager: ModuleManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (args.isEmpty()) {
            sender.sendMessage("§cUse: /wm <enable|disable|reload|load|list> [module]")
            return true
        }

        when (args[0].lowercase()) {

            "enable" -> {
                val module = args.getOrNull(1) ?: return sender.sendMessage("§cUse: /wm enable <module>").let { true }
                moduleManager.enable(module)
            }

            "disable" -> {
                val module = args.getOrNull(1) ?: return sender.sendMessage("§cUse: /wm disable <module>").let { true }
                moduleManager.disable(module)
            }

            "reload" -> {
                val module = args.getOrNull(1) ?: return sender.sendMessage("§cUse: /wm reload <module>").let { true }
                moduleManager.reload(module)
            }

            "load" -> {
                val module = args.getOrNull(1) ?: return sender.sendMessage("§cUse: /wm load <module>").let { true }
                moduleManager.loadModule(module)
            }

            "list" -> {
                sender.sendMessage("§eMódulos:")
                moduleManager.getFormattedList().forEach {
                    sender.sendMessage(" $it")
                }
            }

            else -> {
                sender.sendMessage("§cSubcomando inválido.")
            }
        }

        return true
    }
}