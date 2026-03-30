package net.lazz.core.service.module.util

import net.lazz.core.command.CommandRegistry
import net.lazz.core.command.annotation.CommandInfo
import net.lazz.core.service.dependency.injector.DependencyInjector
import net.lazz.core.service.dependency.container.ServiceContainer
import net.lazz.core.service.service.ServiceRegistry
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

object CommandScanner {

    fun registerCommands(
        plugin: JavaPlugin,
        basePackage: String,
        commandRegistry: CommandRegistry,
        serviceRegistry: ServiceRegistry
    ) {

        val modulesFolder = File(plugin.dataFolder, "modules")

        if (!modulesFolder.exists()) {
            plugin.logger.warning("[Module] Pasta modules não encontrada")
            return
        }

        val jars = modulesFolder.listFiles { f -> f.name.endsWith(".jar") } ?: return

        val path = basePackage.replace(".", "/")

        jars.forEach { jarFile ->

            try {

                val classLoader = URLClassLoader(
                    arrayOf(jarFile.toURI().toURL()),
                    plugin.javaClass.classLoader
                )

                JarFile(jarFile).use { jar ->

                    val entries = jar.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()

                        if (!entry.name.endsWith(".class")) continue
                        if (!entry.name.startsWith(path)) continue

                        val className = entry.name
                            .replace("/", ".")
                            .removeSuffix(".class")

                        try {
                            val clazz = Class.forName(className, true, classLoader)

                            val annotation = clazz.getAnnotation(CommandInfo::class.java)
                                ?: continue

                            // 🔥 GARANTE QUE É COMMAND EXECUTOR
                            if (!CommandExecutor::class.java.isAssignableFrom(clazz)) {
                                plugin.logger.warning("[Module] Ignorado (não é CommandExecutor): $className")
                                continue
                            }

                            val instance = createInstance(clazz) as CommandExecutor

                            // 🔥 INJEÇÃO DE DEPENDÊNCIA
                            val container = ServiceContainer(serviceRegistry)
                            DependencyInjector.inject(instance, container)

                            commandRegistry.register(annotation, instance)

                            plugin.logger.info("[Module] Comando registrado: ${annotation.name}")

                        } catch (ex: Exception) {
                            plugin.logger.warning("[Module] Falha ao registrar comando: $className")
                            ex.printStackTrace()
                        }
                    }
                }

            } catch (ex: Exception) {
                plugin.logger.severe("[Module] Erro ao ler ${jarFile.name}: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }

    private fun createInstance(clazz: Class<*>): Any {
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }
}