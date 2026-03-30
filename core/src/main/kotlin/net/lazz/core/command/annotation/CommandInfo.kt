package net.lazz.core.command.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandInfo(
    val name: String,
    val aliases: Array<String> = [],
    val permission: String = "",
    val permissionMessage: String = "§cSem permissão.",
    val cooldown: Long = 0
)