package net.lazz.core.services.module.service.listener

import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.module.service.container.ModuleServiceContainer
import net.lazz.core.services.module.service.injector.ModuleDependencyInjector
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.jar.JarFile

object ModuleListenerScanner {

    fun registerListeners(
        plugin: JavaPlugin,
        manager: ModuleManager,
        jarFile: File,
        basePackage: String,
        registry: ModuleListenerRegistry,
        container: ModuleServiceContainer,
        classLoader: ClassLoader
    ) {

        val path = basePackage.replace(".", "/")

        // ================= LOG CONTROL =================

        val debug = manager.isDebug()
        val verbose = manager.isVerbose()

        val moduleId = container.moduleId

        fun log(msg: String) {
            if (verbose) plugin.logger.info("[Module:$moduleId] $msg")
        }

        fun debugLog(msg: String) {
            if (debug) plugin.logger.info("§8[DEBUG] [Module:$moduleId] $msg")
        }

        fun warn(msg: String) {
            plugin.logger.warning("[Module:$moduleId] $msg")
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

                        if (!clazz.isAnnotationPresent(ModuleListener::class.java)) return@forEach

                        if (!Listener::class.java.isAssignableFrom(clazz)) {
                            debugLog("Ignorado (não é Listener): $className")
                            return@forEach
                        }

                        val instance = ModuleDependencyInjector.create(clazz, container) as Listener

                        registry.register(instance)

                        log("Listener registrado: ${clazz.simpleName}")

                    } catch (ex: Exception) {

                        warn("Falha ao registrar listener: $className")
                        ex.printStackTrace()
                    }
                }
        }
    }
}