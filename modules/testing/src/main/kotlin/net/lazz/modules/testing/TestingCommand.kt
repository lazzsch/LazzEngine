package net.lazz.modules.testing

import net.lazz.core.command.annotation.CommandInfo
import net.lazz.core.service.dependency.annotation.Inject
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandInfo(
    name = "testing",
    aliases = ["testingtest"]
)
class TestingCommand : CommandExecutor {

    @Inject
    lateinit var service: TestingService

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

        sender.sendMessage("§aTesting funcionando!")
        sender.sendMessage("§7Service: " + service.hello())

        return true
    }
}