package net.lazz.core.services.module.service.container

import net.lazz.core.injector.IServiceContainer
import net.lazz.core.registry.ServiceRegistry
import net.lazz.core.services.module.ModuleManager
import kotlin.reflect.KClass

class ModuleServiceContainer(
    private val global: ServiceRegistry,
    val manager: ModuleManager,
    val moduleId: String
) : IServiceContainer {

    // ================= LOG =================

    private fun debug(msg: String) {
        if (manager.isDebug()) {
            manager.plugin.logger.info("§8[DEBUG] [Module:$moduleId] $msg")
        }
    }

    private fun error(msg: String): Nothing {
        manager.plugin.logger.severe("[Module:$moduleId] $msg")
        throw IllegalStateException(msg)
    }

    // ================= GET =================

    override fun get(type: KClass<*>): Any {

        debug("Buscando service: ${type.simpleName}")

        // 🔹 1. CORE (ServiceRegistry global do plugin)
        if (global.has(type)) {
            debug("Encontrado CORE: ${type.simpleName}")
            return global.get(type)
        }

        // 🔹 2. BUSCAR EM DEPENDÊNCIAS (via namedServices)
        val desc = manager.getDescription(moduleId)
            ?: error("Module não encontrado: $moduleId")

        desc.depends.forEach { dep ->

            val context = manager.moduleContexts[dep.lowercase()] ?: return@forEach

            val match = context.namedServices.values.firstOrNull {
                type.java.isInstance(it)
            }

            if (match != null) {
                debug("Encontrado DEPENDÊNCIA ($dep): ${type.simpleName}")
                return match
            }
        }

        error("Service ${type.simpleName} não encontrado (core/dependências)")
    }

    // ================= HAS =================

    override fun has(type: KClass<*>): Boolean {

        if (global.has(type)) return true

        val desc = manager.getDescription(moduleId) ?: return false

        return desc.depends.any { dep ->
            val context = manager.moduleContexts[dep.lowercase()]
            context?.namedServices?.values?.any {
                type.java.isInstance(it)
            } == true
        }
    }
}