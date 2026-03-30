package net.lazz.core.service.module

interface ModuleModel {

    val id: String

    fun onLoad() {}
    fun onEnable() {}
    fun onDisable() {}
}