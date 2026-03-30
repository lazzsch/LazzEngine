package net.lazz.core

import net.lazz.core.bootstrap.Bootstrap
import org.bukkit.plugin.java.JavaPlugin

class Core : JavaPlugin() {

    private lateinit var bootstrap: Bootstrap

    override fun onLoad() {
        bootstrap = Bootstrap(this)
        bootstrap.init()
        bootstrap.loadModules()
    }

    override fun onEnable() {
        bootstrap.enableModules()
    }

    override fun onDisable() {
        bootstrap.shutdown()
    }
}