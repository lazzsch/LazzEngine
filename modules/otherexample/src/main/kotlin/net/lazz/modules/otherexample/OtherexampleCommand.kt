package net.lazz.modules.otherexample

import net.lazz.core.services.module.api.ModuleAPI
import net.lazz.core.services.module.service.command.annotation.ModuleCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@ModuleCommand(name = "otherexample", aliases = ["otherexampletest"])
class OtherexampleCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command.")
            return true
        }

        val player = sender
        val uuid = player.uniqueId

        // Get current balance
        val before = ModuleAPI.callAs<Int>("MoneyexampleService", "get", uuid) ?: 0
        player.sendMessage("§eCurrent balance: $before")

        // Add money
        ModuleAPI.call("MoneyexampleService", "add", uuid, 50)

        // Get updated balance
        val after = ModuleAPI.callAs<Int>("MoneyexampleService", "get", uuid) ?: 0
        player.sendMessage("§aNew balance (+50): $after")

        return true
    }
}