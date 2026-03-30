package net.lazz.modules.money

import org.bukkit.entity.Player

interface Economy {

    fun getBalance(player: Player): Double

    fun add(player: Player, amount: Double)

    fun remove(player: Player, amount: Double)

    fun set(player: Player, amount: Double)
}