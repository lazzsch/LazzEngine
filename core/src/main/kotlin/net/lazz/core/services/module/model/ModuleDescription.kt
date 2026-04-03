package net.lazz.core.services.module.model

data class ModuleDescription(
    val id: String,
    val name: String,
    val main: String,
    val packageName: String,
    val version: String,
    val depends: List<String>,
    val softDepends: List<String> = emptyList()
)