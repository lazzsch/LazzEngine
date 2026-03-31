package net.lazz.modules.teste

import net.lazz.core.listener.annotation.AutoListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@AutoListener
class TesteListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("§e[Teste] Bem-vindo!")
    }
}