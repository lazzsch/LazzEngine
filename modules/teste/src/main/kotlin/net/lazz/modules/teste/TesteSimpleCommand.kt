package net.lazz.modules.teste

import net.lazz.core.command.annotation.CommandInfo
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandInfo(
    name = "testesimple",
    aliases = ["testes"]
)
class TesteSimpleCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (sender !is Player) {
            sender.sendMessage("Apenas jogadores.")
            return true
        }

        sender.sendMessage("§aComando simples funcionando!")
        return true
    }
}