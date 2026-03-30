package net.lazz.core.service.module

import org.bukkit.plugin.java.JavaPlugin

abstract class AbstractModule(
    final override val id: String,
    val context: ModuleContext
) : ModuleModel {

    protected val plugin: JavaPlugin
        get() = context.plugin

    override fun onDisable() {
        context.cleanup()
    }
}