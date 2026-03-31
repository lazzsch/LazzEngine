package net.lazz.modules.teste

import net.lazz.core.command.annotation.CommandInfo
import net.lazz.core.service.dependency.annotation.Inject
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandInfo(
    name = "teste",
    aliases = ["testetest"]
)
class TesteCommand : CommandExecutor {

    @Inject
    lateinit var service: TesteService

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

        sender.sendMessage("§aTeste funcionando!")
        sender.sendMessage("§7Service: " + service.hello())

        return true
    }
}