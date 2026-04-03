package net.lazz.modules.otherexample

import net.lazz.core.services.module.service.listener.ModuleListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@ModuleListener
class OtherexampleListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("§e[Otherexample] Welcome!")
    }
}