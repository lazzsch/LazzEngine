package net.lazz.core.services.module.model

interface ModuleModel {

    val id: String

    fun onLoad() {}
    fun onEnable() {}
    fun onDisable() {}
}