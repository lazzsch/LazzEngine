package net.lazz.modules.testing

import net.lazz.core.listener.annotation.AutoListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@AutoListener
class TestingListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("§e[Testing] Bem-vindo!")
    }
}