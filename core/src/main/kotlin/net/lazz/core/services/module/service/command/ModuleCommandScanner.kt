package net.lazz.core.services.module.service.command

import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.module.service.command.annotation.ModuleCommand
import net.lazz.core.services.module.service.container.ModuleServiceContainer
import net.lazz.core.services.module.service.injector.ModuleDependencyInjector
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.jar.JarFile

object ModuleCommandScanner {

    fun registerCommands(
        plugin: JavaPlugin,
        manager: ModuleManager,
        jarFile: File,
        basePackage: String,
        registry: ModuleCommandRegistry,
        container: ModuleServiceContainer,
        classLoader: ClassLoader
    ) {

        val path = basePackage.replace(".", "/")

        // ================= LOG CONTROL =================

        val debug = manager.isDebug()
        val verbose = manager.isVerbose()

        fun log(msg: String) {
            if (verbose) plugin.logger.info("[Module] $msg")
        }

        fun debugLog(msg: String) {
            if (debug) plugin.logger.info("§8[DEBUG] [Module] $msg")
        }

        fun warn(msg: String) {
            plugin.logger.warning("[Module] $msg")
        }

        // ================= SCAN =================

        JarFile(jarFile).use { jar ->

            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .filter { it.name.startsWith("$path/") }
                .forEach { entry ->

                    val className = entry.name
                        .replace("/", ".")
                        .removeSuffix(".class")

                    try {

                        val clazz = Class.forName(className, true, classLoader)

                        val annotation = clazz.getAnnotation(ModuleCommand::class.java)
                            ?: return@forEach

                        if (!CommandExecutor::class.java.isAssignableFrom(clazz)) {
                            debugLog("Ignorado (não é CommandExecutor): $className")
                            return@forEach
                        }

                        val instance = ModuleDependencyInjector.create(clazz, container) as CommandExecutor

                        registry.register(annotation, instance)

                        log("Comando registrado: ${annotation.name}")

                    } catch (ex: Exception) {

                        warn("Falha ao registrar comando: $className")
                        ex.printStackTrace()
                    }
                }
        }
    }
}
