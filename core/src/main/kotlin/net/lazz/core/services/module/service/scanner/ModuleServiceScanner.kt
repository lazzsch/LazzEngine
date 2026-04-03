package net.lazz.core.services.module.service.scanner

import net.lazz.core.services.module.AbstractModule
import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.module.api.annotation.ModuleAPI
import net.lazz.core.services.module.model.ModuleModel
import net.lazz.core.services.module.service.ModuleService
import net.lazz.core.services.module.service.container.ModuleServiceContainer
import net.lazz.core.services.module.service.injector.ModuleDependencyInjector
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.Locale
import java.util.jar.JarFile

object ModuleServiceScanner {

    fun registerServices(
        plugin: JavaPlugin,
        manager: ModuleManager,
        jarFile: File,
        basePackage: String,
        container: ModuleServiceContainer,
        classLoader: ClassLoader,
        module: ModuleModel
    ) {

        val path = basePackage.replace(".", "/")

        val context = (module as? AbstractModule)?.context
            ?: throw IllegalStateException("Module não possui context: ${module.javaClass.simpleName}")

        val debug = manager.isDebug()
        val verbose = manager.isVerbose()

        fun log(msg: String) {
            if (verbose) plugin.logger.info("[Module:${context.moduleId}] $msg")
        }

        fun debugLog(msg: String) {
            if (debug) plugin.logger.info("§8[DEBUG] [Module:${context.moduleId}] $msg")
        }

        fun error(msg: String) {
            plugin.logger.severe("[Module:${context.moduleId}] $msg")
        }

        fun warn(msg: String) {
            plugin.logger.warning("[Module:${context.moduleId}] $msg")
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

                        val apiAnnotation = clazz.getAnnotation(ModuleAPI::class.java)
                            ?: return@forEach

                        val apiName = apiAnnotation.value.lowercase(Locale.ROOT)

                        if (apiName.isBlank()) {
                            error("API inválida (nome vazio): ${clazz.name}")
                            return@forEach
                        }

                        val instance = ModuleDependencyInjector.create(clazz, container, module)

                        // ================= REGISTRO =================

                        if (context.namedServices.containsKey(apiName)) {
                            warn("API duplicada detectada: $apiName (${clazz.simpleName}) — sobrescrevendo")
                        }

                        context.namedServices[apiName] = instance

                        log("API registrada: $apiName (${clazz.simpleName})")
                        debugLog("Instância criada: ${clazz.name}")

                        // ================= LIFECYCLE =================

                        if (instance is ModuleService) {
                            try {
                                instance.onLoad()
                            } catch (ex: Exception) {
                                error("Erro no onLoad de ${clazz.simpleName}: ${ex.message}")
                                ex.printStackTrace()
                            }
                        }

                    } catch (ex: Exception) {
                        warn("Falha ao registrar service: $className (${ex.message})")
                        ex.printStackTrace()
                    }
                }
        }
    }
}