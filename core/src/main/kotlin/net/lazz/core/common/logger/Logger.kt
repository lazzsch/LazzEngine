package net.lazz.core.common.logger

import org.bukkit.plugin.java.JavaPlugin

class Logger(private val plugin: JavaPlugin) {

    fun info(msg: String) = plugin.logger.info(msg)
    fun warn(msg: String) = plugin.logger.warning(msg)
}