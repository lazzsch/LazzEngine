package net.lazz.modules.moneyexample

import net.lazz.core.services.module.service.listener.ModuleListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@ModuleListener
class MoneyexampleListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("§e[Moneyexample] Welcome!")
    }
}