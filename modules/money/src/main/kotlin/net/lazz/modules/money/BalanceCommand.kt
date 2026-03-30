package net.lazz.modules.money

import net.lazz.core.command.annotation.CommandInfo
import net.lazz.core.service.dependency.annotation.Inject
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandInfo(
    name = "money",
    aliases = ["balance", "saldo"]
)
class BalanceCommand : CommandExecutor {

    @Inject
    lateinit var moneyService: MoneyService

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

        val saldo = moneyService.get(sender.uniqueId)

        sender.sendMessage("§aSeu saldo: §f$saldo")
        return true
    }
}