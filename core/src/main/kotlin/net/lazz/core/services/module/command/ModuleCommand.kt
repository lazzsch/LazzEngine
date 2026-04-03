package net.lazz.core.services.module.command

import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.module.menu.ModuleMenu
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ModuleCommand(
    private val moduleManager: ModuleManager
) : CommandExecutor {

    private val menu = ModuleMenu(moduleManager)

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (args.isEmpty()) {

            if (sender !is Player) {
                sender.sendMessage("§cApenas jogadores podem abrir o menu.")
                return true
            }

            menu.open(sender)
            return true
        }

        when (args[0].lowercase()) {

            "enable" -> {
                val module = args.getOrNull(1)
                    ?: return sender.sendMessage("§cUse: /lm enable <module>").let { true }

                moduleManager.enable(module)
            }

            "disable" -> {
                val module = args.getOrNull(1)
                    ?: return sender.sendMessage("§cUse: /lm disable <module>").let { true }

                moduleManager.disable(module)
            }

            "reload" -> {
                val module = args.getOrNull(1)
                    ?: return sender.sendMessage("§cUse: /lm reload <module>").let { true }

                moduleManager.reload(module)
            }

            "load" -> {
                val module = args.getOrNull(1)
                    ?: return sender.sendMessage("§cUse: /lm load <module>").let { true }

                moduleManager.loadModule(module)
            }

            "list" -> {
                sender.sendMessage("§eMódulos:")
                moduleManager.getFormattedList().forEach {
                    sender.sendMessage(" $it")
                }
            }

            "info" -> {
                val module = args.getOrNull(1)
                    ?: return sender.sendMessage("§cUse: /lm info <module>").let { true }

                val desc = moduleManager.getDescription(module)

                if (desc == null) {
                    sender.sendMessage("§cModulo nao encontrado.")
                    return true
                }

                val status = if (moduleManager.isEnabled(module)) "§aATIVO" else "§cDESATIVADO"

                sender.sendMessage("§eInformações do módulo:")
                sender.sendMessage(" §7Nome: §f${desc.name}")
                sender.sendMessage(" §7ID: §f${desc.id}")
                sender.sendMessage(" §7Versão: §b${desc.version}")
                sender.sendMessage(" §7Status: $status")
            }

            else -> {
                sender.sendMessage("§cSubcomando inválido.")
                sender.sendMessage("§7Use: /lm <enable|disable|reload|load|info|list>")
            }
        }

        return true
    }
}