package net.lazz.core.service.module.model

data class ModuleDescription(
    val id: String,
    val name: String,
    val main: String,
    val packageName: String,
    val version: String,
    val depends: List<String>
)