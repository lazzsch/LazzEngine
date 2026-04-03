package net.lazz.core.services.module.service.command

import net.lazz.core.services.module.service.command.annotation.ModuleCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class ModuleCommandRegistry(
    private val plugin: Plugin,
    private val moduleId: String
) {

    private val service = ModuleCommandMapService.instance

    // comandos registrados pelo módulo
    private val registeredCommands = mutableListOf<Command>()

    // ================= REGISTER =================

    fun register(info: ModuleCommand, executor: CommandExecutor) {

        val command = object : Command(info.name.lowercase()) {

            init {
                aliases = info.aliases.map { it.lowercase() }

                permission = info.permission.ifEmpty { null }
                permissionMessage = info.permissionMessage.ifEmpty { "§cSem permissão." }
            }

            override fun execute(
                sender: CommandSender,
                label: String,
                args: Array<String>
            ): Boolean {

                // valida label
                if (!label.equals(name, ignoreCase = true) &&
                    !aliases.any { it.equals(label, ignoreCase = true) }
                ) {
                    return false
                }

                // valida permissão
                permission?.let {
                    if (it.isNotEmpty() && !sender.hasPermission(it)) {
                        sender.sendMessage(permissionMessage ?: "§cSem permissão.")
                        return true
                    }
                }

                // ================= VALIDAÇÃO DE DEPENDÊNCIA =================
                try {

                    validateExecutor(executor)

                    return executor.onCommand(sender, this, label, args)

                } catch (ex: Exception) {

                    sender.sendMessage("§cErro interno ao executar comando.")

                    plugin.logger.severe(
                        "[Module:$moduleId] Erro no comando ${name}: ${ex.message}"
                    )

                    ex.printStackTrace()

                    return true
                }
            }
        }

        val fallback = "${plugin.name.lowercase()}:$moduleId"

        service.commandMap.register(fallback, command)

        registeredCommands.add(command)
    }

    // ================= VALIDAÇÃO =================

    private fun validateExecutor(executor: CommandExecutor) {

        executor.javaClass.declaredFields.forEach { field ->

            if (field.type.isPrimitive) return@forEach

            field.isAccessible = true

            val value = try {
                field.get(executor)
            } catch (_: Exception) {
                null
            }

            if (value == null) {
                throw IllegalStateException("Dependência não injetada: ${field.name} em ${executor.javaClass.simpleName}")
            }
        }
    }

    // ================= UNREGISTER =================

    fun unregisterAll() {

        if (registeredCommands.isEmpty()) return

        try {
            ModuleCommandCleanupUtil.unregisterCommands(
                plugin,
                moduleId,
                registeredCommands
            )
        } catch (_: Exception) {}

        registeredCommands.clear()
    }
}