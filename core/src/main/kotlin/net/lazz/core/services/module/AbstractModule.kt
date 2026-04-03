package net.lazz.core.services.module

import net.lazz.core.services.module.model.ModuleModel
import org.bukkit.plugin.java.JavaPlugin

abstract class AbstractModule(
    final override val id: String,
    plugin: JavaPlugin
) : ModuleModel {

    val context = ModuleContext(plugin, id)

    protected val plugin: JavaPlugin
        get() = context.plugin

    override fun onDisable() {}
}