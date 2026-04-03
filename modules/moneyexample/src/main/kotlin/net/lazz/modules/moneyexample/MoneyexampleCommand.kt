package net.lazz.modules.moneyexample

import net.lazz.core.services.module.service.command.annotation.ModuleCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@ModuleCommand(name = "moneyexample", aliases = ["moneyexampletest"])
class MoneyexampleCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (sender !is Player) {
            sender.sendMessage("only players can use this command.")
            return true
        }

        sender.sendMessage("§etest")

        return true
    }
}